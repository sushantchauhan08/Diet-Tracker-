package com.example.data.api

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

// --- Gemini REST API Request & Response Schema ---

@JsonClass(generateAdapter = true)
data class GeminiRequest(
    val contents: List<Content>,
    val generationConfig: GenerationConfig? = null,
    val systemInstruction: Content? = null
)

@JsonClass(generateAdapter = true)
data class Content(
    val parts: List<Part>
)

@JsonClass(generateAdapter = true)
data class InlineData(
    val mimeType: String,
    val data: String
)

@JsonClass(generateAdapter = true)
data class Part(
    val text: String? = null,
    val inlineData: InlineData? = null
)

@JsonClass(generateAdapter = true)
data class GenerationConfig(
    val responseMimeType: String? = null,
    val temperature: Double? = null
)

@JsonClass(generateAdapter = true)
data class GeminiResponse(
    val candidates: List<Candidate>? = null
)

@JsonClass(generateAdapter = true)
data class Candidate(
    val content: Content? = null
)

// --- Domain Models for AI Parsed Outputs ---

@JsonClass(generateAdapter = true)
data class AnalyzedFoodItem(
    val name: String,
    val calories: Int,
    val protein: Float,
    val carbs: Float,
    val fat: Float
)

@JsonClass(generateAdapter = true)
data class AnalyzedFoodResponse(
    val foodItems: List<AnalyzedFoodItem>,
    val totalCalories: Int,
    val totalProtein: Float,
    val totalCarbs: Float,
    val totalFat: Float
)

@JsonClass(generateAdapter = true)
data class SuggestedMeal(
    val type: String,
    val suggestion: String,
    val approxMacros: String
)

@JsonClass(generateAdapter = true)
data class MealSuggestionsResponse(
    val title: String,
    val description: String,
    val meals: List<SuggestedMeal>
)
