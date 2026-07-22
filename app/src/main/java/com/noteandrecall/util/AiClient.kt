package com.noteandrecall.util

import com.google.gson.Gson
import com.google.gson.JsonParser
import java.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

object AiClient {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()
    private val gson = Gson()

    suspend fun chat(endpoint: String, apiKey: String, model: String, prompt: String): String =
        withContext(Dispatchers.IO) {
            LogManager.i("AiClient", "chat() model=$model endpoint=$endpoint prompt.length=${prompt.length}")
            val body = mapOf(
                "model" to model,
                "messages" to listOf(mapOf("role" to "user", "content" to prompt)),
                "temperature" to 0.3,
                "max_tokens" to 1024
            )
            val reqBody = gson.toJson(body).toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url("$endpoint/chat/completions")
                .addHeader("api-key", apiKey)
                .addHeader("Content-Type", "application/json")
                .post(reqBody)
                .build()
            val response = client.newCall(request).execute()
            // Read body ONCE (OkHttp body is single-use)
            val rawBody = response.body?.string()
            if (!response.isSuccessful) {
                LogManager.e("AiClient", "chat() HTTP ${response.code}: ${rawBody ?: "(no body)"}")
                throw Exception("API error ${response.code}: ${rawBody?.take(200) ?: "(no body)"}")
            }
            if (rawBody == null) {
                LogManager.e("AiClient", "chat() response body is null")
                throw Exception("API returned empty body")
            }
            val json = try {
                JsonParser.parseString(rawBody).asJsonObject
            } catch (e: Exception) {
                LogManager.e("AiClient", "chat() JSON parse error: ${e.message}. Body: ${rawBody.take(300)}")
                throw Exception("Failed to parse API response: ${e.message}")
            }
            val choices = json["choices"]?.asJsonArray
            if (choices == null || choices.size() == 0) {
                LogManager.e("AiClient", "chat() no choices: ${rawBody.take(300)}")
                throw Exception("API returned no results: ${rawBody.take(200)}")
            }
            val content = choices[0].asJsonObject["message"]?.asJsonObject?.get("content")?.asString
                ?: throw Exception("API response missing content field")
            LogManager.i("AiClient", "chat() OK response.length=${content.length}")
            content
        }

    suspend fun polishNote(endpoint: String, apiKey: String, model: String, raw: String, existingTags: List<String> = emptyList()): String {
        val tagHint = if (existingTags.isNotEmpty()) {
            "Existing tags you CAN reuse: ${existingTags.take(20).joinToString(", ")}\n- Prefer reusing existing tags when relevant; only create NEW tags if nothing matches.\n- Maximum 3 tags total."
        } else {
            "- Maximum 3 tags."
        }
        val prompt = """You are a knowledge organizer. Polish this raw note into a well-structured knowledge item.

Rules:
- Generate a concise title (max 8 words)
- Keep the core content accurate
- $tagHint
- Write CONTENT in Markdown format. Use headings (##), tables, lists, or bold only when the content exceeds 500 words — keep short notes as plain text or minimal formatting.
- Match the language of the raw note below (if input is Chinese, output Chinese; if English, output English, etc.)
- Return EXACTLY this format:
TITLE: [your title]
TAGS: [#tag1 #tag2 #tag3]
CONTENT: [polished content]

Raw note:
$raw"""
        return chat(endpoint, apiKey, model, prompt)
    }

    suspend fun extractFromImage(endpoint: String, apiKey: String, model: String, base64Image: String, mimeType: String, existingTags: List<String> = emptyList()): String =
        withContext(Dispatchers.IO) {
        LogManager.i("AiClient", "extractFromImage() image_base64.length=${base64Image.length} mime=$mimeType")
        val body = mapOf(
            "model" to model,
            "messages" to listOf(
                // System prompt matching official MiMo example
                mapOf(
                    "role" to "system",
                    "content" to "You are MiMo, an AI assistant developed by Xiaomi."
                ),
                mapOf(
                    "role" to "user",
                    "content" to listOf(
                        // Image FIRST, then text (matching official MiMo order)
                        mapOf("type" to "image_url", "image_url" to mapOf("url" to "data:${mimeType};base64,$base64Image")),
                        mapOf("type" to "text", "text" to """Extract all text and knowledge from this image.\nWrite CONTENT in Markdown format. Only use headings (##), tables, lists, or bold if the content exceeds 500 words.\nMatch the language of the text visible in the image.\nMaximum 3 tags. Prefer reusing existing tags when relevant.\nReturn EXACTLY this format:\nTITLE: [concise title]\nTAGS: [#tag1 #tag2]\nCONTENT: [extracted knowledge content]""")
                    )
                )
            ),
            "max_tokens" to 1024
        )
        val reqBody = gson.toJson(body).toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url("$endpoint/chat/completions")
            .addHeader("api-key", apiKey)
            .addHeader("Content-Type", "application/json")
            .post(reqBody)
            .build()
        val response = client.newCall(request).execute()
        // Read body ONCE (OkHttp body is single-use)
        val rawBody = response.body?.string()
        if (!response.isSuccessful) {
            LogManager.e("AiClient", "extractFromImage() HTTP ${response.code}: ${rawBody ?: "(no body)"}")
            throw Exception("API error ${response.code}: ${rawBody?.take(200) ?: "(no body)"}")
        }
        if (rawBody == null) {
            LogManager.e("AiClient", "extractFromImage() response body is null")
            throw Exception("API returned empty body")
        }
        val json = try {
            JsonParser.parseString(rawBody).asJsonObject
        } catch (e: Exception) {
            LogManager.e("AiClient", "extractFromImage() JSON parse error: ${e.message}. Body: ${rawBody.take(300)}")
            throw Exception("Failed to parse API response: ${e.message}")
        }
        val choices = json["choices"]?.asJsonArray
        if (choices == null || choices.size() == 0) {
            LogManager.e("AiClient", "extractFromImage() no choices: ${rawBody.take(300)}")
            throw Exception("API returned no results: ${rawBody.take(200)}")
        }
        val choice = choices[0].asJsonObject
        val finishReason = choice["finish_reason"]?.asString ?: "unknown"
        val content = choice["message"]?.asJsonObject?.get("content")?.asString
            ?: throw Exception("API response missing content field (finish_reason=$finishReason)")
        LogManager.i("AiClient", "extractFromImage() OK finish_reason=$finishReason content.length=${content.length}")
        content
    }

    /**
     * Transcribe audio using MiMo v2.5-ASR.
     * MiMo does NOT use the Whisper /audio/transcriptions endpoint — instead it uses
     * /chat/completions with a special model (mimo-v2.5-asr) and input_audio content type.
     */
    suspend fun transcribeAudio(endpoint: String, apiKey: String, model: String, audioBytes: ByteArray, fileName: String): String =
        withContext(Dispatchers.IO) {
            LogManager.i("AiClient", "transcribeAudio() file=$fileName size=${audioBytes.size}")
            // MiMo ASR uses model mimo-v2.5-asr regardless of the configured chat model
            val asrModel = "mimo-v2.5-asr"
            val base64Audio = Base64.getEncoder().encodeToString(audioBytes)
            val body = mapOf(
                "model" to asrModel,
                "messages" to listOf(
                    mapOf(
                        "role" to "user",
                        "content" to listOf(
                            mapOf(
                                "type" to "input_audio",
                                "input_audio" to mapOf(
                                    "data" to "data:audio/wav;base64,$base64Audio"
                                )
                            )
                        )
                    )
                ),
                // extra_body for asr_options
                "asr_options" to mapOf(
                    "language" to "auto"
                )
            )
            val reqBody = gson.toJson(body).toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url("$endpoint/chat/completions")
                .addHeader("api-key", apiKey)
                .addHeader("Content-Type", "application/json")
                .post(reqBody)
                .build()
            val response = client.newCall(request).execute()
            val rawBody = response.body?.string()
            if (!response.isSuccessful) {
                LogManager.e("AiClient", "transcribeAudio() HTTP ${response.code}: ${rawBody ?: "(no body)"}")
                throw Exception("Transcription API error ${response.code}: ${rawBody?.take(200) ?: "(no body)"}")
            }
            if (rawBody == null) {
                LogManager.e("AiClient", "transcribeAudio() response body is null")
                throw Exception("Transcription API returned empty body")
            }
            val json = try {
                JsonParser.parseString(rawBody).asJsonObject
            } catch (e: Exception) {
                LogManager.e("AiClient", "transcribeAudio() JSON parse error: ${e.message}. Body: ${rawBody.take(300)}")
                throw Exception("Failed to parse transcription response: ${e.message}")
            }
            val choices = json["choices"]?.asJsonArray
            if (choices == null || choices.size() == 0) {
                LogManager.e("AiClient", "transcribeAudio() no choices: ${rawBody.take(300)}")
                throw Exception("Transcription returned no results: ${rawBody.take(200)}")
            }
            val text = choices[0].asJsonObject["message"]?.asJsonObject?.get("content")?.asString
                ?: throw Exception("Transcription response missing content: ${rawBody.take(200)}")
            LogManager.i("AiClient", "transcribeAudio() OK text.length=${text.length}")
            text
        }

    suspend fun extendContent(endpoint: String, apiKey: String, model: String, content: String): String {
        val prompt = """Expand the following content to at least 500 words. Keep the original meaning, add relevant details, examples, and explanations. Return ONLY the expanded content, no extra formatting.

$content"""
        return chat(endpoint, apiKey, model, prompt)
    }
}
