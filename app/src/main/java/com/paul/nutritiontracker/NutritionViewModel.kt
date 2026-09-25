package com.paul.nutritiontracker

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.paul.nutritiontracker.data.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.LocalDate

class NutritionViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = NutritionRepository(app)
    private val _state = MutableStateFlow(repo.load())
    val state: StateFlow<AppData> = _state.asStateFlow()

    private fun update(block: (AppData) -> AppData) {
        val next = block(_state.value)
        _state.value = next
        repo.save(next)
    }

    // ---- Ingredients ----
    fun addIngredient(ing: Ingredient) = update { it.copy(ingredients = it.ingredients + ing) }

    fun updateIngredient(id: String, patch: Ingredient) = update { data ->
        data.copy(ingredients = data.ingredients.map { if (it.id == id) patch.copy(id = id) else it })
    }

    fun deleteIngredient(id: String) = update { data ->
        data.copy(ingredients = data.ingredients.filterNot { it.id == id })
    }

    // ---- Recipes ----
    fun addRecipe(r: Recipe) = update { it.copy(recipes = it.recipes + r) }

    fun updateRecipe(id: String, patch: Recipe) = update { data ->
        data.copy(recipes = data.recipes.map { if (it.id == id) patch.copy(id = id) else it })
    }

    fun deleteRecipe(id: String) = update { data ->
        data.copy(recipes = data.recipes.filterNot { it.id == id })
    }

    // ---- Meal log (date-based, with optional weekly repeat) ----
    fun setMealEntry(date: LocalDate, mealType: String, entry: MealEntry?, repeatWeekly: Boolean) =
        update { data -> setMealEntryPure(data, date, mealType, entry, repeatWeekly) }

    // ---- Preferences ----
    fun setPreferences(p: Preferences) = update { it.copy(preferences = p) }

    // ---- Body metrics ----
    fun setBodyMetric(date: LocalDate, entry: BodyMetricEntry) = update { data ->
        val newMap = data.bodyMetrics.toMutableMap()
        newMap[date.toString()] = entry
        data.copy(bodyMetrics = newMap)
    }

    fun deleteBodyMetric(date: LocalDate) = update { data ->
        val newMap = data.bodyMetrics.toMutableMap()
        newMap.remove(date.toString())
        data.copy(bodyMetrics = newMap)
    }

    // ---- Full reset ----
    fun resetAll() = update { AppData() }

    // ---- Bulk import (Excel), tracked as a removable/replaceable batch ----
    fun importData(fileName: String, newIngredients: List<Ingredient>, newRecipes: List<Recipe>) = update { data ->
        val batch = ImportBatch(
            fileName = fileName,
            importedAt = System.currentTimeMillis(),
            ingredientIds = newIngredients.map { it.id },
            recipeIds = newRecipes.map { it.id }
        )
        data.copy(
            ingredients = data.ingredients + newIngredients,
            recipes = data.recipes + newRecipes,
            importBatches = data.importBatches + batch
        )
    }

    fun removeImportBatch(batchId: String) = update { data ->
        val batch = data.importBatches.find { it.id == batchId } ?: return@update data
        data.copy(
            ingredients = data.ingredients.filterNot { it.id in batch.ingredientIds },
            recipes = data.recipes.filterNot { it.id in batch.recipeIds },
            importBatches = data.importBatches.filterNot { it.id == batchId }
        )
    }
}
