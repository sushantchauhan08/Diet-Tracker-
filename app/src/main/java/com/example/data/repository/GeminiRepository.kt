package com.example.data.repository

import android.graphics.Bitmap
import android.util.Base64
import android.util.Log
import com.example.BuildConfig
import com.example.data.api.*
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.io.ByteArrayOutputStream

class GeminiRepository(private val apiService: GeminiApiService) {

    private val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
    private val foodAdapter = moshi.adapter(AnalyzedFoodResponse::class.java)
    private val mealAdapter = moshi.adapter(MealSuggestionsResponse::class.java)

    private val apiKey: String = BuildConfig.GEMINI_API_KEY

    private fun Bitmap.toBase64(): String {
        val outputStream = ByteArrayOutputStream()
        compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
        return Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
    }

    suspend fun analyzeFoodImage(bitmap: Bitmap, textPrompt: String? = null): AnalyzedFoodResponse? {
        val systemPrompt = "You are an expert nutritionist AI. The user will upload a photo of a meal or plate of food. " +
                "You must analyze the image input (and any accompanying text context description: ${textPrompt ?: "None"}) and estimate the net Calories, Protein (g), Carbs (g), and Fat (g). " +
                "You MUST return a JSON object containing the parsed food items and totals. " +
                "Do NOT return any backticks, markdown markers, or other explanation outside of the valid JSON object itself. " +
                "Format of the JSON response is exactly: \n" +
                "{\n" +
                "  \"foodItems\": [\n" +
                "     { \"name\": \"item name\", \"calories\": 120, \"protein\": 10.5, \"carbs\": 15.0, \"fat\": 2.0 }\n" +
                "  ],\n" +
                "  \"totalCalories\": 120,\n" +
                "  \"totalProtein\": 10.5,\n" +
                "  \"totalCarbs\": 15.0,\n" +
                "  \"totalFat\": 2.0\n" +
                "}"

        val userPromptText = "Identify and estimate the food items and their calories/macros in this photo. " +
                if (!textPrompt.isNullOrBlank()) "Additional description context: $textPrompt" else ""

        return try {
            val base64Data = bitmap.toBase64()
            val requestBody = GeminiRequest(
                contents = listOf(
                    Content(
                        parts = listOf(
                            Part(text = userPromptText),
                            Part(inlineData = InlineData(mimeType = "image/jpeg", data = base64Data))
                        )
                    )
                ),
                systemInstruction = Content(parts = listOf(Part(text = systemPrompt))),
                generationConfig = GenerationConfig(
                    responseMimeType = "application/json",
                    temperature = 0.2
                )
            )

            val apiResponse = apiService.generateContent(apiKey, requestBody)
            val jsonText = apiResponse.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
            Log.d("GeminiRepository", "Raw image food parse output: $jsonText")

            if (jsonText != null) {
                val cleanedJson = cleanMarkdownJson(jsonText)
                foodAdapter.fromJson(cleanedJson) ?: throw Exception("JSON conversion returned null")
            } else {
                throw Exception("Received empty response candidate list from Gemini API")
            }
        } catch (e: Exception) {
            handleException(e, "analyzing food image")
        }
    }

    suspend fun analyzeFood(foodText: String): AnalyzedFoodResponse? {
        val systemPrompt = "You are an expert nutritionist AI. The user will type a list of food items. " +
                "You must analyze the text input and estimate the net Calories, Protein (g), Carbs (g), and Fat (g). " +
                "You MUST return a JSON object containing the parsed food items and totals. " +
                "Do NOT return any backticks, markdown markers, or other explanation outside of the valid JSON object itself. " +
                "Format of the JSON response is exactly: \n" +
                "{\n" +
                "  \"foodItems\": [\n" +
                "     { \"name\": \"item name\", \"calories\": 120, \"protein\": 10.5, \"carbs\": 15.0, \"fat\": 2.0 }\n" +
                "  ],\n" +
                "  \"totalCalories\": 120,\n" +
                "  \"totalProtein\": 10.5,\n" +
                "  \"totalCarbs\": 15.0,\n" +
                "  \"totalFat\": 2.0\n" +
                "}"

        val userPrompt = "Analyze the following items: $foodText"

        return try {
            val requestBody = GeminiRequest(
                contents = listOf(Content(parts = listOf(Part(text = userPrompt)))),
                systemInstruction = Content(parts = listOf(Part(text = systemPrompt))),
                generationConfig = GenerationConfig(
                    responseMimeType = "application/json",
                    temperature = 0.2
                )
            )

            val apiResponse = apiService.generateContent(apiKey, requestBody)
            val jsonText = apiResponse.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
            Log.d("GeminiRepository", "Raw food parse output: $jsonText")

            if (jsonText != null) {
                // Pre-process response in case Gemini accidentally includes markdown wrap
                val cleanedJson = cleanMarkdownJson(jsonText)
                foodAdapter.fromJson(cleanedJson) ?: throw Exception("JSON conversion returned null")
            } else {
                throw Exception("Received empty response candidate list from Gemini API")
            }
        } catch (e: Exception) {
            handleException(e, "analyzing food items")
        }
    }

    suspend fun suggestMeals(goal: String, dietType: String, notes: String): MealSuggestionsResponse? {
        val systemPrompt = "You are a personal fitness diet coach AI. Help the user plan meals. " +
                "The user will describe their target and dietary requirements. " +
                "You MUST return a clean JSON object outlining the meals and approximate macros. " +
                "Do NOT return any backticks, markdown markers, or other explanation outside of the valid JSON object itself. " +
                "Format of the JSON response is exactly: \n" +
                "{\n" +
                "  \"title\": \"Plan Title\",\n" +
                "  \"description\": \"Plan general description\",\n" +
                "  \"meals\": [\n" +
                "     { \"type\": \"Breakfast\", \"suggestion\": \"Meal recommendation details\", \"approxMacros\": \"Calories: 500, Protein: 25g\" }\n" +
                "  ]\n" +
                "}"

        val userPrompt = "Goal: $goal, Dietary Type: $dietType, Additional Info/Restrictions: $notes"

        return try {
            val requestBody = GeminiRequest(
                contents = listOf(Content(parts = listOf(Part(text = userPrompt)))),
                systemInstruction = Content(parts = listOf(Part(text = systemPrompt))),
                generationConfig = GenerationConfig(
                    responseMimeType = "application/json",
                    temperature = 0.7
                )
            )

            val apiResponse = apiService.generateContent(apiKey, requestBody)
            val jsonText = apiResponse.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
            Log.d("GeminiRepository", "Raw suggester output: $jsonText")

            if (jsonText != null) {
                val cleanedJson = cleanMarkdownJson(jsonText)
                mealAdapter.fromJson(cleanedJson) ?: throw Exception("JSON conversion returned null")
            } else {
                throw Exception("Received empty response candidate list from Gemini API")
            }
        } catch (e: Exception) {
            handleException(e, "suggesting meals")
        }
    }

    private fun handleException(e: Exception, actionName: String): Nothing {
        Log.e("GeminiRepository", "Error during $actionName", e)
        if (e is retrofit2.HttpException) {
            val code = e.code()
            val errorBody = try {
                e.response()?.errorBody()?.string()
            } catch (ex: Exception) {
                null
            }
            throw Exception("API Error ($code): ${errorBody ?: e.message() ?: "Unknown API response"}")
        }
        throw e
    }

    private fun cleanMarkdownJson(raw: String): String {
        var clean = raw.trim()
        if (clean.startsWith("```json")) {
            clean = clean.removePrefix("```json")
        } else if (clean.startsWith("```")) {
            clean = clean.removePrefix("```")
        }
        if (clean.endsWith("```")) {
            clean = clean.removeSuffix("```")
        }
        return clean.trim()
    }
}
