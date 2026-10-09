package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.database.AppDatabase
import com.example.data.model.MealLog
import com.example.data.repository.GeminiRepository
import com.example.data.repository.MealRepository
import com.example.data.api.AnalyzedFoodResponse
import com.example.data.api.MealSuggestionsResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class DietViewModel(
    application: Application,
    private val mealRepository: MealRepository,
    private val geminiRepository: GeminiRepository
) : AndroidViewModel(application) {

    private val sharedPrefs = application.getSharedPreferences("diet_tracker_prefs", Context.MODE_PRIVATE)

    // Daily Goals
    private val _calorieGoal = MutableStateFlow(sharedPrefs.getInt("calorie_goal", 2500))
    val calorieGoal: StateFlow<Int> = _calorieGoal.asStateFlow()

    private val _proteinGoal = MutableStateFlow(sharedPrefs.getFloat("protein_goal", 100f))
    val proteinGoal: StateFlow<Float> = _proteinGoal.asStateFlow()

    // App Theme Setting: "system", "light", "dark"
    private val _themeSetting = MutableStateFlow(sharedPrefs.getString("theme_setting", "system") ?: "system")
    val themeSetting: StateFlow<String> = _themeSetting.asStateFlow()

    // Database Logs
    val loggedMeals: StateFlow<List<MealLog>> = mealRepository.allMeals
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Food Analyzer Input/Output States
    private val _analyzerInput = MutableStateFlow("")
    val analyzerInput: StateFlow<String> = _analyzerInput.asStateFlow()

    private val _analyzerImage = MutableStateFlow<android.graphics.Bitmap?>(null)
    val analyzerImage: StateFlow<android.graphics.Bitmap?> = _analyzerImage.asStateFlow()

    private val _analyzerLoading = MutableStateFlow(false)
    val analyzerLoading: StateFlow<Boolean> = _analyzerLoading.asStateFlow()

    private val _analyzerResult = MutableStateFlow<AnalyzedFoodResponse?>(null)
    val analyzerResult: StateFlow<AnalyzedFoodResponse?> = _analyzerResult.asStateFlow()

    private val _analyzerError = MutableStateFlow<String?>(null)
    val analyzerError: StateFlow<String?> = _analyzerError.asStateFlow()

    // Meal Suggestions Input/Output States
    private val _suggestionGoal = MutableStateFlow("Gain muscle")
    val suggestionGoal: StateFlow<String> = _suggestionGoal.asStateFlow()

    private val _suggestionDiet = MutableStateFlow("Vegetarian")
    val suggestionDiet: StateFlow<String> = _suggestionDiet.asStateFlow()

    private val _suggestionNotes = MutableStateFlow("")
    val suggestionNotes: StateFlow<String> = _suggestionNotes.asStateFlow()

    private val _suggestionLoading = MutableStateFlow(false)
    val suggestionLoading: StateFlow<Boolean> = _suggestionLoading.asStateFlow()

    private val _suggestionResult = MutableStateFlow<MealSuggestionsResponse?>(null)
    val suggestionResult: StateFlow<MealSuggestionsResponse?> = _suggestionResult.asStateFlow()

    private val _suggestionError = MutableStateFlow<String?>(null)
    val suggestionError: StateFlow<String?> = _suggestionError.asStateFlow()

    // Setters for Goals
    fun updateCalorieGoal(goal: Int) {
        _calorieGoal.value = goal
        sharedPrefs.edit().putInt("calorie_goal", goal).apply()
    }

    fun updateProteinGoal(goal: Float) {
        _proteinGoal.value = goal
        sharedPrefs.edit().putFloat("protein_goal", goal).apply()
    }

    fun updateThemeSetting(theme: String) {
        _themeSetting.value = theme
        sharedPrefs.edit().putString("theme_setting", theme).apply()
    }

    // Setters for input bindings
    fun updateAnalyzerInput(text: String) {
        _analyzerInput.value = text
    }

    fun updateAnalyzerImage(bitmap: android.graphics.Bitmap?) {
        _analyzerImage.value = bitmap
    }

    fun updateSuggestionGoal(goal: String) {
        _suggestionGoal.value = goal
    }

    fun updateSuggestionDiet(diet: String) {
        _suggestionDiet.value = diet
    }

    fun updateSuggestionNotes(notes: String) {
        _suggestionNotes.value = notes
    }

    // Database Actions
    fun logMeal(name: String, foodItems: String, calories: Int, protein: Float, carbs: Float, fat: Float) {
        viewModelScope.launch {
            val meal = MealLog(
                name = name,
                foodItems = foodItems,
                calories = calories,
                protein = protein,
                carbs = carbs,
                fat = fat
            )
            mealRepository.insertMeal(meal)
        }
    }

    fun deleteMeal(id: Int) {
        viewModelScope.launch {
            mealRepository.deleteMealById(id)
        }
    }

    fun clearAllLogs() {
        viewModelScope.launch {
            mealRepository.deleteAllMeals()
        }
    }

    // AI Actions
    fun analyzeFood() {
        val input = _analyzerInput.value.trim()
        val bitmap = _analyzerImage.value

        if (input.isEmpty() && bitmap == null) {
            _analyzerError.value = "Please enter some food text or upload a food photo first."
            return
        }

        viewModelScope.launch {
            _analyzerLoading.value = true
            _analyzerError.value = null
            _analyzerResult.value = null

            try {
                val result = if (bitmap != null) {
                    geminiRepository.analyzeFoodImage(bitmap, input.ifEmpty { null })
                } else {
                    geminiRepository.analyzeFood(input)
                }

                if (result != null) {
                    _analyzerResult.value = result
                } else {
                    _analyzerError.value = "Failed to analyze food items: Received null response content."
                }
            } catch (e: Exception) {
                _analyzerError.value = e.message ?: e.toString()
            } finally {
                _analyzerLoading.value = false
            }
        }
    }

    fun addAnalyzedResultToLog(mealName: String) {
        val result = _analyzerResult.value ?: return
        viewModelScope.launch {
            // Join parsed item names for the log entry description
            val joinedDescription = result.foodItems.joinToString(", ") { "${it.name} (${it.calories} kcal)" }
            val meal = MealLog(
                name = mealName,
                foodItems = joinedDescription,
                calories = result.totalCalories,
                protein = result.totalProtein,
                carbs = result.totalCarbs,
                fat = result.totalFat
            )
            mealRepository.insertMeal(meal)
            // Reset input, image, and analyzer states after adding to list
            _analyzerInput.value = ""
            _analyzerImage.value = null
            _analyzerResult.value = null
        }
    }

    fun fetchMealSuggestions() {
        viewModelScope.launch {
            _suggestionLoading.value = true
            _suggestionError.value = null
            _suggestionResult.value = null

            try {
                val result = geminiRepository.suggestMeals(
                    goal = _suggestionGoal.value,
                    dietType = _suggestionDiet.value,
                    notes = _suggestionNotes.value
                )

                if (result != null) {
                    _suggestionResult.value = result
                } else {
                    _suggestionError.value = "Failed to generate plan: Received null response content."
                }
            } catch (e: Exception) {
                _suggestionError.value = e.message ?: e.toString()
            } finally {
                _suggestionLoading.value = false
            }
        }
    }

    // Factory Class
    companion object {
        fun provideFactory(
            application: Application,
            mealRepository: MealRepository,
            geminiRepository: GeminiRepository
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                if (modelClass.isAssignableFrom(DietViewModel::class.java)) {
                    return DietViewModel(application, mealRepository, geminiRepository) as T
                }
                throw IllegalArgumentException("Unknown ViewModel class")
            }
        }
    }
}
