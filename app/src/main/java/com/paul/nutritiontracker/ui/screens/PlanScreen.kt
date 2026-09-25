@file:OptIn(ExperimentalMaterial3Api::class)

package com.paul.nutritiontracker.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.paul.nutritiontracker.NutritionViewModel
import com.paul.nutritiontracker.data.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private fun startOfWeek(date: LocalDate): LocalDate = date.minusDays((date.dayOfWeek.value - 1).toLong())

@Composable
fun PlanScreen(modifier: Modifier, data: AppData, vm: NutritionViewModel) {
    val today = remember { LocalDate.now() }
    var selectedDate by remember { mutableStateOf(today) }
    var weekStart by remember { mutableStateOf(startOfWeek(today)) }
    var editingMealType by remember { mutableStateOf<String?>(null) }

    val ingById = remember(data.ingredients) { data.ingredients.associateBy { it.id } }
    val recById = remember(data.recipes) { data.recipes.associateBy { it.id } }
    val isFuture = selectedDate.isAfter(today)
    val totals = remember(selectedDate, data.rotation, data.overrides, data.recipes, data.ingredients) {
        dateTotals(selectedDate, data, recById, ingById)
    }
    val targets = data.preferences.dailyTarget

    Column(modifier.fillMaxSize()) {
        // Week navigation
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { weekStart = weekStart.minusWeeks(1) }) { Icon(Icons.Filled.ChevronLeft, contentDescription = "Previous week") }
            Text(
                "${weekStart.format(DateTimeFormatter.ofPattern("d MMM"))} – ${weekStart.plusDays(6).format(DateTimeFormatter.ofPattern("d MMM"))}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            IconButton(onClick = { weekStart = weekStart.plusWeeks(1) }) { Icon(Icons.Filled.ChevronRight, contentDescription = "Next week") }
        }

        // Date strip
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            for (i in 0..6) {
                val d = weekStart.plusDays(i.toLong())
                val selected = d == selectedDate
                val future = d.isAfter(today)
                Column(
                    Modifier
                        .weight(1f)
                        .background(
                            if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                            RoundedCornerShape(12.dp)
                        )
                        .clickableSimple { selectedDate = d }
                        .padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val contentColor = when {
                        selected -> MaterialTheme.colorScheme.onPrimary
                        future -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)
                        else -> MaterialTheme.colorScheme.onSurface
                    }
                    Text(d.dayOfWeek.name.take(3), style = MaterialTheme.typography.labelSmall, color = contentColor)
                    Text("${d.dayOfMonth}", fontWeight = FontWeight.SemiBold, color = contentColor)
                    if (d == today) {
                        Box(Modifier.size(4.dp).background(if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary, RoundedCornerShape(2.dp)))
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StatCard("kcal", "${totals.kcal.toInt()}", Modifier.weight(1f), KcalColor)
            StatCard("Protein", "${totals.protein.toInt()}g", Modifier.weight(1f), ProteinColor)
            StatCard("Carbs", "${totals.carbs.toInt()}g", Modifier.weight(1f), CarbsColor)
            StatCard("Fat", "${totals.fat.toInt()}g", Modifier.weight(1f), FatColor)
        }

        if (isFuture) {
            Text(
                "This day hasn't happened yet — showing your weekly rotation.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
            )
        }

        Spacer(Modifier.height(4.dp))
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            data.preferences.mealTypes.forEach { mealType ->
                val entry = effectiveEntry(selectedDate, mealType, data)
                MealRow(
                    mealType = mealType,
                    entry = entry,
                    recById = recById,
                    ingById = ingById,
                    split = data.preferences.quickLogSplit,
                    locked = isFuture,
                    onClick = { if (!isFuture) editingMealType = mealType }
                )
                Spacer(Modifier.height(10.dp))
            }
            Spacer(Modifier.height(16.dp))
        }
    }

    editingMealType?.let { mealType ->
        val currentEntry = effectiveEntry(selectedDate, mealType, data)
        EditMealDialog(
            date = selectedDate,
            mealType = mealType,
            currentEntry = currentEntry,
            allRecipes = data.recipes,
            ingById = ingById,
            onDismiss = { editingMealType = null },
            onClear = { vm.setMealEntry(selectedDate, mealType, null, false); editingMealType = null },
            onSave = { entry, repeat -> vm.setMealEntry(selectedDate, mealType, entry, repeat); editingMealType = null }
        )
    }
}

@Composable
private fun MealRow(
    mealType: String,
    entry: MealEntry?,
    recById: Map<String, Recipe>,
    ingById: Map<String, Ingredient>,
    split: MacroSplit,
    locked: Boolean,
    onClick: () -> Unit
) {
    val macros = entryMacros(entry, recById, ingById, split)
    val summary = when {
        entry?.recipeId != null -> recById[entry.recipeId]?.name ?: "Unknown recipe"
        entry?.quickKcal != null -> "${entry.quickKcal.toInt()} kcal (quick log)"
        else -> "— empty —"
    }
    Row(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp))
            .let { if (!locked) it.clickableSimple(onClick) else it }
            .padding(14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(mealType, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(2.dp))
            Text(
                summary,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (locked) 0.45f else 0.75f)
            )
            if (entry != null) {
                Spacer(Modifier.height(4.dp))
                MacroChipRow(macros.kcal, macros.protein, macros.carbs, macros.fat)
            }
        }
        if (locked) {
            Icon(Icons.Filled.Lock, contentDescription = "Locked — future day", tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f), modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun EditMealDialog(
    date: LocalDate,
    mealType: String,
    currentEntry: MealEntry?,
    allRecipes: List<Recipe>,
    ingById: Map<String, Ingredient>,
    onDismiss: () -> Unit,
    onClear: () -> Unit,
    onSave: (MealEntry, Boolean) -> Unit
) {
    var mode by remember { mutableStateOf(if (currentEntry?.quickKcal != null) "quick" else "recipe") }
    var selectedRecipeId by remember { mutableStateOf(currentEntry?.recipeId) }
    var kcalText by remember { mutableStateOf(currentEntry?.quickKcal?.toInt()?.toString() ?: "") }
    var repeatWeekly by remember { mutableStateOf(false) }

    val filteredRecipes = remember(allRecipes, mealType) {
        allRecipes.filter { it.mealTypes.isEmpty() || it.mealTypes.contains(mealType) }
    }
    val weekdayName = weekdayOf(date)

    Dialog(onDismiss) {
        Column(
            Modifier
                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(20.dp))
                .padding(20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text("$mealType · ${date.format(DateTimeFormatter.ofPattern("EEE d MMM"))}", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                FilterChip(selected = mode == "recipe", onClick = { mode = "recipe" }, label = { Text("Recipe") })
                FilterChip(selected = mode == "quick", onClick = { mode = "quick" }, label = { Text("Quick calories") })
            }
            Spacer(Modifier.height(12.dp))

            if (mode == "recipe") {
                if (filteredRecipes.isEmpty()) {
                    Text(
                        "No recipes tagged for $mealType yet. Add one in the Recipes tab.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                } else {
                    val selectedRecipe = filteredRecipes.find { it.id == selectedRecipeId }
                    SearchableDropdownField(
                        label = "Search recipes for $mealType",
                        options = filteredRecipes,
                        selectedName = selectedRecipe?.name ?: "",
                        displayName = { it.name },
                        onSelect = { r -> selectedRecipeId = r.id },
                        modifier = Modifier.fillMaxWidth()
                    )
                    selectedRecipe?.let {
                        Spacer(Modifier.height(8.dp))
                        val m = recipeMacros(it, ingById)
                        MacroChipRow(m.kcal, m.protein, m.carbs, m.fat)
                    }
                }
            } else {
                Text(
                    "Macros are split using your Preferences quick-log setting.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = kcalText, onValueChange = { kcalText = it }, label = { Text("Calories") }, modifier = Modifier.fillMaxWidth())
            }

            Spacer(Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(checked = repeatWeekly, onCheckedChange = { repeatWeekly = it })
                Spacer(Modifier.width(8.dp))
                Text("Repeat every $weekdayName", style = MaterialTheme.typography.bodyMedium)
            }

            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (currentEntry != null) {
                    OutlinedButton(onClick = onClear, modifier = Modifier.weight(1f)) { Text("Clear") }
                }
                OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text("Cancel") }
                Button(
                    onClick = {
                        val entry = if (mode == "recipe") {
                            selectedRecipeId?.let { MealEntry(recipeId = it) }
                        } else {
                            kcalText.toDoubleOrNull()?.let { MealEntry(quickKcal = it) }
                        }
                        entry?.let { onSave(it, repeatWeekly) }
                    },
                    modifier = Modifier.weight(1f),
                    enabled = (mode == "recipe" && selectedRecipeId != null) || (mode == "quick" && kcalText.toDoubleOrNull() != null)
                ) { Text("Save") }
            }
        }
    }
}

/** Plain click without a ripple indication. */
fun Modifier.clickableSimple(onClick: () -> Unit): Modifier = composed {
    clickable(
        interactionSource = remember { MutableInteractionSource() },
        indication = null,
        onClick = onClick
    )
}
