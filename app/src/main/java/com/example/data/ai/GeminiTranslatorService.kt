package com.example.data.ai

import com.example.BuildConfig
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class GeminiRequest(
    val contents: List<Content>,
    val systemInstruction: Content? = null
)

@JsonClass(generateAdapter = true)
data class Content(
    val parts: List<Part>
)

@JsonClass(generateAdapter = true)
data class Part(
    val text: String? = null,
    val inlineData: InlineData? = null
)

@JsonClass(generateAdapter = true)
data class InlineData(
    val mimeType: String,
    val data: String
)

@JsonClass(generateAdapter = true)
data class GeminiResponse(
    val candidates: List<Candidate>?
)

@JsonClass(generateAdapter = true)
data class Candidate(
    val content: Content?
)

interface GeminiApi {
    @POST("v1beta/models/gemini-3.5-flash:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @Body request: GeminiRequest
    ): GeminiResponse
}

object GeminiClient {
    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        })
        .build()

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    val api: GeminiApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://generativelanguage.googleapis.com/")
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(GeminiApi::class.java)
    }
}

class GeminiTranslatorService {
    suspend fun translateText(
        sourceText: String,
        sourceLang: String,
        targetLang: String,
        style: String
    ): String {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isBlank()) {
            throw IllegalAccessException("Gemini API Key is missing. Please configure GEMINI_API_KEY in the AI Studio Secrets panel.")
        }

        val prompt = buildString {
            append("You are an expert professional translator and linguist.\n")
            append("Translate the following text accurately from $sourceLang to $targetLang.\n")
            append("Translation Style: $style.\n")
            if (style == "Formal") append("Maintain a formal, polite, and respectful tone.\n")
            if (style == "Informal") append("Maintain a casual, friendly, and conversational tone.\n")
            if (style == "Professional") append("Maintain corporate, business-appropriate terminology.\n")
            if (style == "Casual") append("Maintain relaxed everyday colloquial phrasing.\n")
            append("Provide ONLY the final translated text without extra conversational filler, markdown formatting blocks (unless necessary), or notes, unless requested.\n\n")
            append("Text to translate:\n$sourceText")
        }

        val request = GeminiRequest(
            contents = listOf(
                Content(parts = listOf(Part(text = prompt)))
            )
        )

        val response = GeminiClient.api.generateContent(apiKey, request)
        val resultText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
        return resultText?.trim() ?: throw Exception("Failed to get translation from Gemini API.")
    }

    suspend fun translateImage(
        base64Image: String,
        mimeType: String,
        targetLang: String
    ): String {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isBlank()) {
            throw IllegalAccessException("Gemini API Key is missing. Please configure GEMINI_API_KEY in the AI Studio Secrets panel.")
        }

        val prompt = "Extract all readable text from this image (OCR) and translate it accurately into $targetLang. Provide the extracted text and the clean translation clearly."

        val request = GeminiRequest(
            contents = listOf(
                Content(
                    parts = listOf(
                        Part(text = prompt),
                        Part(inlineData = InlineData(mimeType = mimeType, data = base64Image))
                    )
                )
            )
        )

        val response = GeminiClient.api.generateContent(apiKey, request)
        val resultText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
        return resultText?.trim() ?: throw Exception("Failed to process image translation.")
    }
}
