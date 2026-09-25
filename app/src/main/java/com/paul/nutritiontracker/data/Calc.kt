package com.paul.nutritiontracker.data

import java.time.LocalDate

data class Macros(
    val kcal: Double = 0.0,
    val protein: Double = 0.0,
    val carbs: Double = 0.0,
    val fat: Double = 0.0,
    val sugar: Double = 0.0
) {
    operator fun plus(other: Macros) = Macros(
        kcal + other.kcal, protein + other.protein, carbs + other.carbs, fat + other.fat, sugar + other.sugar
    )
}

fun ingredientPerServing(ing: Ingredient): Macros {
    val f = ing.serving / 100.0
    return Macros(ing.kcal100 * f, ing.protein100 * f, ing.carbs100 * f, ing.fat100 * f, ing.sugar100 * f)
}

fun recipeMacros(recipe: Recipe, ingredientsById: Map<String, Ingredient>): Macros {
    return when (recipe.mode) {
        "direct" -> Macros(recipe.kcal, recipe.protein, recipe.carbs, recipe.fat, recipe.sugar)
        "percent" -> {
            val kcal = recipe.kcal
            Macros(
                kcal = kcal,
                protein = kcal * recipe.proteinPct / 100.0 / 4.0,
                fat = kcal * recipe.fatPct / 100.0 / 9.0,
                carbs = kcal * recipe.carbPct / 100.0 / 4.0,
                sugar = recipe.sugar
            )
        }
        "ingredient" -> {
            val ing = ingredientsById[recipe.sourceIngredientId] ?: return Macros()
            val f = (ing.serving.takeIf { it > 0 } ?: 100.0) * recipe.servingMultiplier / 100.0
            Macros(ing.kcal100 * f, ing.protein100 * f, ing.carbs100 * f, ing.fat100 * f, ing.sugar100 * f)
        }
        else -> {
            var kcal = 0.0; var protein = 0.0; var carbs = 0.0; var fat = 0.0; var sugar = 0.0
            recipe.items.forEach { item ->
                val ing = ingredientsById[item.ingredientId] ?: return@forEach
                val f = item.qty / 100.0
                kcal += ing.kcal100 * f
                protein += ing.protein100 * f
                carbs += ing.carbs100 * f
                fat += ing.fat100 * f
                sugar += ing.sugar100 * f
            }
            Macros(kcal, protein, carbs, fat, sugar)
        }
    }
}

fun entryMacros(entry: MealEntry?, recipesById: Map<String, Recipe>, ingredientsById: Map<String, Ingredient>, split: MacroSplit): Macros {
    if (entry == null) return Macros()
    entry.recipeId?.let { rid -> return recipesById[rid]?.let { recipeMacros(it, ingredientsById) } ?: Macros() }
    entry.quickKcal?.let { k ->
        return Macros(
            kcal = k,
            protein = k * split.proteinPct / 100.0 / 4.0,
            fat = k * split.fatPct / 100.0 / 9.0,
            carbs = k * split.carbPct / 100.0 / 4.0,
            sugar = 0.0
        )
    }
    return Macros()
}

fun weekdayOf(date: LocalDate): String = WEEKDAYS[date.dayOfWeek.value - 1]

fun effectiveEntry(date: LocalDate, mealType: String, data: AppData): MealEntry? {
    val iso = date.toString()
    data.overrides[iso]?.get(mealType)?.let { return it }
    return data.rotation[weekdayOf(date)]?.get(mealType)
}

fun dateTotals(date: LocalDate, data: AppData, recipesById: Map<String, Recipe>, ingredientsById: Map<String, Ingredient>): Macros {
    var total = Macros()
    data.preferences.mealTypes.forEach { mt ->
        val entry = effectiveEntry(date, mt, data)
        total += entryMacros(entry, recipesById, ingredientsById, data.preferences.quickLogSplit)
    }
    return total
}

/** One logged food for a date, with the macros it contributed — used by the nutrition breakdown screen. */
data class FoodContribution(val mealType: String, val name: String, val macros: Macros)

fun dateContributions(date: LocalDate, data: AppData, recipesById: Map<String, Recipe>, ingredientsById: Map<String, Ingredient>): List<FoodContribution> {
    return data.preferences.mealTypes.mapNotNull { mt ->
        val entry = effectiveEntry(date, mt, data) ?: return@mapNotNull null
        val macros = entryMacros(entry, recipesById, ingredientsById, data.preferences.quickLogSplit)
        val name = entry.recipeId?.let { recipesById[it]?.name }
            ?: entry.quickKcal?.let { "${it.toInt()} kcal (quick log)" }
            ?: "Unknown"
        FoodContribution(mt, name, macros)
    }
}

fun setMealEntryPure(data: AppData, date: LocalDate, mealType: String, entry: MealEntry?, repeatWeekly: Boolean): AppData {
    val iso = date.toString()
    return if (repeatWeekly) {
        val weekday = weekdayOf(date)
        val dayMap = (data.rotation[weekday] ?: emptyMap()).toMutableMap()
        if (entry == null) dayMap.remove(mealType) else dayMap[mealType] = entry
        val newRotation = data.rotation.toMutableMap()
        if (dayMap.isEmpty()) newRotation.remove(weekday) else newRotation[weekday] = dayMap
        val overrideDayMap = (data.overrides[iso] ?: emptyMap()).toMutableMap()
        overrideDayMap.remove(mealType)
        val newOverrides = data.overrides.toMutableMap()
        if (overrideDayMap.isEmpty()) newOverrides.remove(iso) else newOverrides[iso] = overrideDayMap
        data.copy(rotation = newRotation, overrides = newOverrides)
    } else {
        val dayMap = (data.overrides[iso] ?: emptyMap()).toMutableMap()
        if (entry == null) dayMap.remove(mealType) else dayMap[mealType] = entry
        val newOverrides = data.overrides.toMutableMap()
        if (dayMap.isEmpty()) newOverrides.remove(iso) else newOverrides[iso] = dayMap
        data.copy(overrides = newOverrides)
    }
}

fun Double.round(decimals: Int = 0): Double {
    val f = Math.pow(10.0, decimals.toDouble())
    return Math.round(this * f) / f
}
