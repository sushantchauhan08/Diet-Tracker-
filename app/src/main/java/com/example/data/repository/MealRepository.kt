package com.example.data.repository

import com.example.data.database.MealLogDao
import com.example.data.model.MealLog
import kotlinx.coroutines.flow.Flow

class MealRepository(private val mealLogDao: MealLogDao) {
    val allMeals: Flow<List<MealLog>> = mealLogDao.getAllMeals()

    suspend fun insertMeal(meal: MealLog) {
        mealLogDao.insertMeal(meal)
    }

    suspend fun deleteMealById(id: Int) {
        mealLogDao.deleteMealById(id)
    }

    suspend fun deleteAllMeals() {
        mealLogDao.deleteAllMeals()
    }
}
