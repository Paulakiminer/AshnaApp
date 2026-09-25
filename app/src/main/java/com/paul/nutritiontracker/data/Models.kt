package com.paul.nutritiontracker.data

import kotlinx.serialization.Serializable
import java.util.UUID

fun newId(): String = UUID.randomUUID().toString().take(8)

val WEEKDAYS = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")
val DEFAULT_MEAL_TYPES = listOf("Breakfast", "Snack", "Lunch", "Pre-Workout", "Post-Workout", "Dinner")

@Serializable
data class Ingredient(
    val id: String = newId(),
    val name: String = "",
    val kcal100: Double = 0.0,
    val protein100: Double = 0.0,
    val carbs100: Double = 0.0,
    val fat100: Double = 0.0,
    val sugar100: Double = 0.0,
    val serving: Double = 100.0,
    val source: String = ""
)

@Serializable
data class RecipeItem(
    val ingredientId: String = "",
    val qty: Double = 0.0
)

@Serializable
data class Recipe(
    val id: String = newId(),
    val name: String = "",
    // "compose" | "direct" | "percent" | "ingredient"
    val mode: String = "compose",
    val items: List<RecipeItem> = emptyList(),
    val kcal: Double = 0.0,
    val protein: Double = 0.0,
    val carbs: Double = 0.0,
    val fat: Double = 0.0,
    val sugar: Double = 0.0, // manual entry, used by "direct" and "percent" modes
    val proteinPct: Double = 0.0,
    val fatPct: Double = 0.0,
    val carbPct: Double = 0.0,
    val mealTypes: List<String> = emptyList(),
    val sourceIngredientId: String = "",
    val servingMultiplier: Double = 1.0
)

@Serializable
data class MealEntry(
    val recipeId: String? = null,
    val quickKcal: Double? = null
)

@Serializable
data class DayTargets(
    val kcal: Double = 2400.0,
    val protein: Double = 180.0,
    val carbs: Double = 240.0,
    val fat: Double = 67.0
)

@Serializable
data class MacroSplit(
    val proteinPct: Double = 30.0,
    val fatPct: Double = 30.0,
    val carbPct: Double = 40.0
)

@Serializable
data class Preferences(
    val mealTypes: List<String> = DEFAULT_MEAL_TYPES,
    // one target, applies to every day
    val dailyTarget: DayTargets = DayTargets(),
    val quickLogSplit: MacroSplit = MacroSplit(),
    val notes: String = "",
    val weightGoalKg: Double? = null,
    val weightGoal: String = "maintain" // "lose" | "maintain" | "gain"
)

@Serializable
data class ImportBatch(
    val id: String = newId(),
    val fileName: String = "Imported file",
    val importedAt: Long = 0L,
    val ingredientIds: List<String> = emptyList(),
    val recipeIds: List<String> = emptyList()
)

@Serializable
data class BodyMetricEntry(
    val weightKg: Double? = null,
    val bodyFatPct: Double? = null,
    val muscleMassKg: Double? = null,
    val waterPct: Double? = null,
    val notes: String = ""
)

@Serializable
data class AppData(
    val ingredients: List<Ingredient> = emptyList(),
    val recipes: List<Recipe> = emptyList(),
    val rotation: Map<String, Map<String, MealEntry>> = emptyMap(),
    val overrides: Map<String, Map<String, MealEntry>> = emptyMap(),
    val preferences: Preferences = Preferences(),
    val importBatches: List<ImportBatch> = emptyList(),
    val bodyMetrics: Map<String, BodyMetricEntry> = emptyMap()
)
