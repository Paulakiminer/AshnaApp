package com.paul.nutritiontracker.data

import android.content.Context
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class NutritionRepository(context: Context) {
    private val prefs = context.getSharedPreferences("nutrition_prefs", Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    fun load(): AppData {
        val raw = prefs.getString(KEY, null) ?: return AppData()
        return try {
            json.decodeFromString(AppData.serializer(), raw)
        } catch (e: Exception) {
            AppData()
        }
    }

    fun save(data: AppData) {
        prefs.edit().putString(KEY, json.encodeToString(data)).apply()
    }

    companion object {
        private const val KEY = "app_data_json"
    }
}
