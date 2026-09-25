package com.paul.nutritiontracker.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.paul.nutritiontracker.NutritionViewModel
import com.paul.nutritiontracker.data.*

@Composable
fun IngredientsScreen(modifier: Modifier, data: AppData, vm: NutritionViewModel) {
    var editing by remember { mutableStateOf<Ingredient?>(null) }
    var showNew by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    val filteredIngredients = remember(data.ingredients, query) {
        if (query.isBlank()) data.ingredients else data.ingredients.filter { it.name.contains(query, ignoreCase = true) }
    }

    Box(modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
            Text("Ingredients", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(vertical = 12.dp))
            SearchField(query, { query = it }, "Search ingredients", Modifier.padding(bottom = 10.dp))
            if (data.ingredients.isEmpty()) {
                EmptyState("No ingredients yet. Add your first one, or import from Excel on the Home tab.")
            } else if (filteredIngredients.isEmpty()) {
                EmptyState("No ingredients match \"$query\".")
            }
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(filteredIngredients, key = { it.id }) { ing ->
                    val per = ingredientPerServing(ing)
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(14.dp))
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(ing.name, fontWeight = FontWeight.SemiBold)
                            Text(
                                "/100g: ${ing.kcal100.toInt()}kcal ${"%.1f".format(ing.protein100)}P ${"%.1f".format(ing.carbs100)}C ${"%.1f".format(ing.fat100)}F ${"%.1f".format(ing.sugar100)}sugar  ·  serving ${ing.serving.toInt()}g → ${per.kcal.toInt()}kcal",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                            )
                        }
                        IconButton(onClick = { editing = ing }) { Icon(Icons.Filled.Edit, contentDescription = "Edit") }
                        ConfirmDeleteButton(onConfirm = { vm.deleteIngredient(ing.id) })
                    }
                }
                item { Spacer(Modifier.height(80.dp)) }
            }
        }

        FloatingActionButton(
            onClick = { showNew = true },
            modifier = Modifier.align(Alignment.BottomEnd).padding(20.dp)
        ) { Icon(Icons.Filled.Add, contentDescription = "New ingredient") }
    }

    if (showNew) {
        IngredientDialog(initial = null, onDismiss = { showNew = false }) { ing -> vm.addIngredient(ing); showNew = false }
    }
    editing?.let { ing ->
        IngredientDialog(initial = ing, onDismiss = { editing = null }) { updated -> vm.updateIngredient(ing.id, updated); editing = null }
    }
}

@Composable
private fun IngredientDialog(initial: Ingredient?, onDismiss: () -> Unit, onSave: (Ingredient) -> Unit) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var kcal by remember { mutableStateOf(initial?.kcal100?.let { if (it == 0.0) "" else it.toString() } ?: "") }
    var protein by remember { mutableStateOf(initial?.protein100?.let { if (it == 0.0) "" else it.toString() } ?: "") }
    var carbs by remember { mutableStateOf(initial?.carbs100?.let { if (it == 0.0) "" else it.toString() } ?: "") }
    var fat by remember { mutableStateOf(initial?.fat100?.let { if (it == 0.0) "" else it.toString() } ?: "") }
    var sugar by remember { mutableStateOf(initial?.sugar100?.let { if (it == 0.0) "" else it.toString() } ?: "") }
    var serving by remember { mutableStateOf((initial?.serving ?: 100.0).toInt().toString()) }
    var source by remember { mutableStateOf(initial?.source ?: "") }

    val per = remember(kcal, protein, carbs, fat, sugar, serving) {
        ingredientPerServing(
            Ingredient(
                kcal100 = kcal.toDoubleOrNull() ?: 0.0,
                protein100 = protein.toDoubleOrNull() ?: 0.0,
                carbs100 = carbs.toDoubleOrNull() ?: 0.0,
                fat100 = fat.toDoubleOrNull() ?: 0.0,
                sugar100 = sugar.toDoubleOrNull() ?: 0.0,
                serving = serving.toDoubleOrNull() ?: 0.0
            )
        )
    }

    Dialog(onDismiss) {
        Column(
            Modifier
                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(20.dp))
                .padding(20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(if (initial == null) "New ingredient" else "Edit ingredient", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = kcal, onValueChange = { kcal = it }, label = { Text("kcal/100g") }, modifier = Modifier.weight(1f))
                OutlinedTextField(value = protein, onValueChange = { protein = it }, label = { Text("Protein/100g") }, modifier = Modifier.weight(1f))
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = carbs, onValueChange = { carbs = it }, label = { Text("Carbs/100g") }, modifier = Modifier.weight(1f))
                OutlinedTextField(value = fat, onValueChange = { fat = it }, label = { Text("Fat/100g") }, modifier = Modifier.weight(1f))
            }
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(value = sugar, onValueChange = { sugar = it }, label = { Text("Sugar/100g (optional)") }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(value = serving, onValueChange = { serving = it }, label = { Text("Default serving size (g/ml)") }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(value = source, onValueChange = { source = it }, label = { Text("Source note") }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(10.dp))
            MacroChipRow(per.kcal, per.protein, per.carbs, per.fat)
            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text("Cancel") }
                Button(
                    onClick = {
                        onSave(
                            Ingredient(
                                id = initial?.id ?: newId(),
                                name = name,
                                kcal100 = kcal.toDoubleOrNull() ?: 0.0,
                                protein100 = protein.toDoubleOrNull() ?: 0.0,
                                carbs100 = carbs.toDoubleOrNull() ?: 0.0,
                                fat100 = fat.toDoubleOrNull() ?: 0.0,
                                sugar100 = sugar.toDoubleOrNull() ?: 0.0,
                                serving = serving.toDoubleOrNull() ?: 100.0,
                                source = source
                            )
                        )
                    },
                    modifier = Modifier.weight(1f),
                    enabled = name.isNotBlank()
                ) { Text("Save") }
            }
        }
    }
}
