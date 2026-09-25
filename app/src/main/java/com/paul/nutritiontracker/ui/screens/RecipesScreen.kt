@file:OptIn(ExperimentalMaterial3Api::class)

package com.paul.nutritiontracker.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.paul.nutritiontracker.NutritionViewModel
import com.paul.nutritiontracker.data.*
import com.paul.nutritiontracker.ui.theme.Basil
import com.paul.nutritiontracker.ui.theme.Blueberry
import com.paul.nutritiontracker.ui.theme.Tangerine
import com.paul.nutritiontracker.ui.theme.Butter

private enum class SortMode { NAME, MEAL_TYPE }

@Composable
fun RecipesScreen(modifier: Modifier, data: AppData, vm: NutritionViewModel) {
    var editing by remember { mutableStateOf<Recipe?>(null) }
    var showNew by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    var filterType by remember { mutableStateOf<String?>(null) }
    var sortMode by remember { mutableStateOf(SortMode.NAME) }
    val ingById = remember(data.ingredients) { data.ingredients.associateBy { it.id } }

    val displayed = remember(data.recipes, query, filterType, sortMode) {
        data.recipes
            .filter { r -> query.isBlank() || r.name.contains(query, ignoreCase = true) }
            .filter { r -> filterType == null || r.mealTypes.contains(filterType) }
            .let { list ->
                when (sortMode) {
                    SortMode.NAME -> list.sortedBy { it.name }
                    SortMode.MEAL_TYPE -> list.sortedBy { it.mealTypes.firstOrNull() ?: "zzz" }
                }
            }
    }

    Box(modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
            Text("Recipes", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(vertical = 12.dp))
            SearchField(query, { query = it }, "Search recipes", Modifier.padding(bottom = 8.dp))

            // Filter chips by meal type
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(bottom = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                FilterChip(selected = filterType == null, onClick = { filterType = null }, label = { Text("All") })
                data.preferences.mealTypes.forEach { mt ->
                    FilterChip(selected = filterType == mt, onClick = { filterType = if (filterType == mt) null else mt }, label = { Text(mt) })
                }
            }
            // Sort row
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Sort:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                FilterChip(selected = sortMode == SortMode.NAME, onClick = { sortMode = SortMode.NAME }, label = { Text("Name") })
                FilterChip(selected = sortMode == SortMode.MEAL_TYPE, onClick = { sortMode = SortMode.MEAL_TYPE }, label = { Text("Meal type") })
            }
            Spacer(Modifier.height(4.dp))

            if (data.recipes.isEmpty()) EmptyState("No recipes yet. Tap + to add your first one.")
            else if (displayed.isEmpty()) EmptyState("No recipes match your filter.")

            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(displayed, key = { it.id }) { r ->
                    val m = recipeMacros(r, ingById)
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(14.dp))
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(r.name, fontWeight = FontWeight.SemiBold)
                            MacroChipRow(m.kcal, m.protein, m.carbs, m.fat)
                            if (r.mealTypes.isNotEmpty()) {
                                Spacer(Modifier.height(4.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    r.mealTypes.forEach { mt ->
                                        Surface(shape = RoundedCornerShape(6.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                                            Text(mt, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                        }
                                    }
                                }
                            }
                        }
                        IconButton(onClick = { editing = r }) { Icon(Icons.Filled.Edit, contentDescription = "Edit") }
                        ConfirmDeleteButton(onConfirm = { vm.deleteRecipe(r.id) })
                    }
                }
                item { Spacer(Modifier.height(80.dp)) }
            }
        }

        FloatingActionButton(onClick = { showNew = true }, modifier = Modifier.align(Alignment.BottomEnd).padding(20.dp)) {
            Icon(Icons.Filled.Add, contentDescription = "New recipe")
        }
    }

    if (showNew) {
        RecipeDialog(null, data.ingredients, data.preferences.mealTypes, onDismiss = { showNew = false }) { vm.addRecipe(it); showNew = false }
    }
    editing?.let { r ->
        RecipeDialog(r, data.ingredients, data.preferences.mealTypes, onDismiss = { editing = null }) { vm.updateRecipe(r.id, it); editing = null }
    }
}

@Composable
private fun RecipeDialog(
    initial: Recipe?,
    ingredients: List<Ingredient>,
    mealTypeOptions: List<String>,
    onDismiss: () -> Unit,
    onSave: (Recipe) -> Unit
) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var mode by remember { mutableStateOf(initial?.mode ?: "compose") }
    var items by remember { mutableStateOf(initial?.items ?: emptyList()) }
    var mealTypes by remember { mutableStateOf(initial?.mealTypes?.toSet() ?: emptySet()) }
    var kcal by remember { mutableStateOf((initial?.kcal ?: 0.0).let { if (it == 0.0) "" else it.toInt().toString() }) }
    var protein by remember { mutableStateOf((initial?.protein ?: 0.0).let { if (it == 0.0) "" else it.toInt().toString() }) }
    var carbs by remember { mutableStateOf((initial?.carbs ?: 0.0).let { if (it == 0.0) "" else it.toInt().toString() }) }
    var fat by remember { mutableStateOf((initial?.fat ?: 0.0).let { if (it == 0.0) "" else it.toInt().toString() }) }
    var sugar by remember { mutableStateOf((initial?.sugar ?: 0.0).let { if (it == 0.0) "" else it.toInt().toString() }) }
    var pctKcal by remember { mutableStateOf((initial?.kcal ?: 0.0).let { if (it == 0.0) "" else it.toInt().toString() }) }
    var proteinPct by remember { mutableStateOf((initial?.proteinPct ?: 30.0).toInt().toString()) }
    var fatPct by remember { mutableStateOf((initial?.fatPct ?: 30.0).toInt().toString()) }
    var carbPct by remember { mutableStateOf((initial?.carbPct ?: 40.0).toInt().toString()) }
    var sourceIngId by remember { mutableStateOf(initial?.sourceIngredientId ?: "") }
    var ingMultiplier by remember { mutableStateOf(initial?.servingMultiplier ?: 1.0) }

    val ingById = remember(ingredients) { ingredients.associateBy { it.id } }

    val computed = remember(mode, items, kcal, protein, carbs, fat, sugar, pctKcal, proteinPct, fatPct, carbPct, sourceIngId, ingMultiplier) {
        when (mode) {
            "compose" -> recipeMacros(Recipe(mode = "compose", items = items), ingById)
            "direct" -> Macros(kcal.toDoubleOrNull() ?: 0.0, protein.toDoubleOrNull() ?: 0.0, carbs.toDoubleOrNull() ?: 0.0, fat.toDoubleOrNull() ?: 0.0, sugar.toDoubleOrNull() ?: 0.0)
            "ingredient" -> {
                val ing = ingById[sourceIngId]
                if (ing == null) Macros()
                else {
                    val f = (ing.serving.takeIf { it > 0 } ?: 100.0) * ingMultiplier / 100.0
                    Macros(ing.kcal100 * f, ing.protein100 * f, ing.carbs100 * f, ing.fat100 * f, ing.sugar100 * f)
                }
            }
            else -> {
                val k = pctKcal.toDoubleOrNull() ?: 0.0
                Macros(k, k * (proteinPct.toDoubleOrNull() ?: 0.0) / 100.0 / 4.0, k * (carbPct.toDoubleOrNull() ?: 0.0) / 100.0 / 4.0, k * (fatPct.toDoubleOrNull() ?: 0.0) / 100.0 / 9.0, sugar.toDoubleOrNull() ?: 0.0)
            }
        }
    }
    val pctSum = (proteinPct.toIntOrNull() ?: 0) + (fatPct.toIntOrNull() ?: 0) + (carbPct.toIntOrNull() ?: 0)

    Dialog(onDismiss) {
        Column(
            Modifier
                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(20.dp))
                .padding(20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(if (initial == null) "New recipe" else "Edit recipe", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Recipe name") }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(12.dp))

            Text("Meal types", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
            Spacer(Modifier.height(4.dp))
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                mealTypeOptions.forEach { mt ->
                    FilterChip(selected = mealTypes.contains(mt), onClick = { mealTypes = if (mealTypes.contains(mt)) mealTypes - mt else mealTypes + mt }, label = { Text(mt) })
                }
            }
            Spacer(Modifier.height(2.dp))
            Text("(none = any meal)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f))
            Spacer(Modifier.height(12.dp))

            Text("Recipe type", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
            Spacer(Modifier.height(6.dp))
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                ModeButton("From ingredients", "Build from your ingredient list", mode == "compose", Basil) { mode = "compose" }
                ModeButton("Single ingredient", "Log one ingredient as a meal (banana, egg...)", mode == "ingredient", Butter) { mode = "ingredient" }
                ModeButton("By grams", "Enter macros directly in grams", mode == "direct", Blueberry) { mode = "direct" }
                ModeButton("By calories + %", "Total kcal plus macro percentages", mode == "percent", Tangerine) { mode = "percent" }
            }
            Spacer(Modifier.height(12.dp))

            when (mode) {
                "compose" -> {
                    if (ingredients.isEmpty()) { Text("Add ingredients first in the Ingredients tab."); }
                    items.forEachIndexed { idx, item ->
                        RecipeItemRow(
                            item = item, ingredients = ingredients,
                            onChange = { newItem -> items = items.toMutableList().also { it[idx] = newItem } },
                            onRemove = { items = items.toMutableList().also { it.removeAt(idx) } }
                        )
                        Spacer(Modifier.height(6.dp))
                    }
                    if (ingredients.isNotEmpty()) {
                        TextButton(onClick = { items = items + RecipeItem(ingredientId = "", qty = 100.0) }) {
                            Icon(Icons.Filled.Add, null, Modifier.size(16.dp)); Spacer(Modifier.width(4.dp)); Text("Add ingredient")
                        }
                    }
                }
                "ingredient" -> {
                    val selIng = ingById[sourceIngId]
                    SearchableDropdownField(
                        label = "— Choose ingredient —",
                        options = ingredients,
                        selectedName = selIng?.name ?: "",
                        displayName = { it.name },
                        onSelect = { sourceIngId = it.id; ingMultiplier = 1.0; if (name.isBlank()) name = it.name },
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (selIng != null) {
                        Spacer(Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Servings", Modifier.weight(1f))
                            IconButton(onClick = { ingMultiplier = (ingMultiplier - 0.5).coerceAtLeast(0.5) }, Modifier.size(30.dp)) {
                                Icon(Icons.Filled.Remove, null, Modifier.size(14.dp))
                            }
                            Text("×${"%.1f".format(ingMultiplier)}", fontWeight = FontWeight.SemiBold, modifier = Modifier.width(40.dp), textAlign = TextAlign.Center)
                            IconButton(onClick = { ingMultiplier += 0.5 }, Modifier.size(30.dp)) {
                                Icon(Icons.Filled.Add, null, Modifier.size(14.dp))
                            }
                            Text("(${((selIng.serving.takeIf { it > 0 } ?: 100.0) * ingMultiplier).toInt()}g)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f))
                        }
                    }
                }
                "direct" -> {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = kcal, onValueChange = { kcal = it }, label = { Text("kcal") }, modifier = Modifier.weight(1f))
                        OutlinedTextField(value = protein, onValueChange = { protein = it }, label = { Text("Protein (g)") }, modifier = Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = carbs, onValueChange = { carbs = it }, label = { Text("Carbs (g)") }, modifier = Modifier.weight(1f))
                        OutlinedTextField(value = fat, onValueChange = { fat = it }, label = { Text("Fat (g)") }, modifier = Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(value = sugar, onValueChange = { sugar = it }, label = { Text("Sugar (g) — optional") }, modifier = Modifier.fillMaxWidth())
                }
                else -> {
                    OutlinedTextField(value = pctKcal, onValueChange = { pctKcal = it }, label = { Text("Total kcal") }, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = proteinPct, onValueChange = { proteinPct = it }, label = { Text("Protein %") }, modifier = Modifier.weight(1f))
                        OutlinedTextField(value = fatPct, onValueChange = { fatPct = it }, label = { Text("Fat %") }, modifier = Modifier.weight(1f))
                        OutlinedTextField(value = carbPct, onValueChange = { carbPct = it }, label = { Text("Carb %") }, modifier = Modifier.weight(1f))
                    }
                    if (pctSum != 100) {
                        Spacer(Modifier.height(4.dp))
                        Text("Adds up to $pctSum% (aim for 100%)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                    }
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(value = sugar, onValueChange = { sugar = it }, label = { Text("Sugar (g) — optional") }, modifier = Modifier.fillMaxWidth())
                }
            }

            Spacer(Modifier.height(12.dp))
            MacroChipRow(computed.kcal, computed.protein, computed.carbs, computed.fat)
            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onDismiss, Modifier.weight(1f)) { Text("Cancel") }
                Button(
                    onClick = {
                        onSave(Recipe(
                            id = initial?.id ?: newId(), name = name, mode = mode,
                            items = items.filter { it.ingredientId.isNotBlank() },
                            kcal = if (mode == "percent") pctKcal.toDoubleOrNull() ?: 0.0 else kcal.toDoubleOrNull() ?: 0.0,
                            protein = protein.toDoubleOrNull() ?: 0.0, carbs = carbs.toDoubleOrNull() ?: 0.0, fat = fat.toDoubleOrNull() ?: 0.0,
                            sugar = sugar.toDoubleOrNull() ?: 0.0,
                            proteinPct = proteinPct.toDoubleOrNull() ?: 0.0, fatPct = fatPct.toDoubleOrNull() ?: 0.0, carbPct = carbPct.toDoubleOrNull() ?: 0.0,
                            mealTypes = mealTypes.toList(),
                            sourceIngredientId = sourceIngId, servingMultiplier = ingMultiplier
                        ))
                    },
                    modifier = Modifier.weight(1f), enabled = name.isNotBlank()
                ) { Text("Save") }
            }
        }
    }
}

@Composable
private fun ModeButton(label: String, description: String, selected: Boolean, color: Color, onClick: () -> Unit) {
    Surface(onClick = onClick, shape = RoundedCornerShape(12.dp), color = if (selected) color.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(12.dp).background(if (selected) color else color.copy(alpha = 0.3f), RoundedCornerShape(6.dp)))
            Spacer(Modifier.width(10.dp))
            Column {
                Text(label, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal, color = if (selected) color else MaterialTheme.colorScheme.onSurface)
                Text(description, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f))
            }
        }
    }
}

@Composable
private fun RecipeItemRow(item: RecipeItem, ingredients: List<Ingredient>, onChange: (RecipeItem) -> Unit, onRemove: () -> Unit) {
    val selected = if (item.ingredientId.isBlank()) null else ingredients.find { it.id == item.ingredientId }
    val servingSize = selected?.serving?.takeIf { it > 0 } ?: 100.0
    var multiplier by remember(item.ingredientId) { mutableStateOf(if (item.qty > 0 && selected != null) item.qty / servingSize else 1.0) }

    Column(Modifier.fillMaxWidth().padding(vertical = 4.dp).background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp)).padding(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            SearchableDropdownField(
                label = "— Choose ingredient —", options = ingredients, selectedName = selected?.name ?: "", displayName = { it.name },
                onSelect = { ing -> multiplier = 1.0; onChange(item.copy(ingredientId = ing.id, qty = ing.serving.takeIf { it > 0 } ?: 100.0)) },
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onRemove) { Icon(Icons.Filled.Delete, null) }
        }
        if (selected != null) {
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Servings", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                IconButton(onClick = { multiplier = (multiplier - 0.5).coerceAtLeast(0.5); onChange(item.copy(qty = servingSize * multiplier)) }, Modifier.size(30.dp)) {
                    Icon(Icons.Filled.Remove, null, Modifier.size(14.dp))
                }
                Text("×${"%.1f".format(multiplier)}", fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center, modifier = Modifier.width(40.dp))
                IconButton(onClick = { multiplier += 0.5; onChange(item.copy(qty = servingSize * multiplier)) }, Modifier.size(30.dp)) {
                    Icon(Icons.Filled.Add, null, Modifier.size(14.dp))
                }
                Text("(${(servingSize * multiplier).toInt()}g)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f))
            }
        }
    }
}
