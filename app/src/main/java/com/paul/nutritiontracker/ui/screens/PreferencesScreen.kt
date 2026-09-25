package com.paul.nutritiontracker.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
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
fun PreferencesScreen(modifier: Modifier, data: AppData, vm: NutritionViewModel) {
    var showTargets by remember { mutableStateOf(false) }
    var showMealTypes by remember { mutableStateOf(false) }
    var showQuickLog by remember { mutableStateOf(false) }
    var showNotes by remember { mutableStateOf(false) }
    var showUploads by remember { mutableStateOf(false) }
    var showImport by remember { mutableStateOf(false) }
    var showResetConfirm by remember { mutableStateOf(false) }
    var showWeightGoal by remember { mutableStateOf(false) }
    val prefs = data.preferences

    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
    ) {
        Text("Preferences", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(vertical = 12.dp))
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SettingsRow(
                "Daily Targets",
                "kcal ${prefs.dailyTarget.kcal.toInt()} · P ${prefs.dailyTarget.protein.toInt()}g · C ${prefs.dailyTarget.carbs.toInt()}g · F ${prefs.dailyTarget.fat.toInt()}g — same every day"
            ) { showTargets = true }
            SettingsRow("Meal Types", prefs.mealTypes.joinToString(", ")) { showMealTypes = true }
            SettingsRow(
                "Quick-Log Macro Split",
                "Used for calorie-only logging: ${prefs.quickLogSplit.proteinPct.toInt()}% P / ${prefs.quickLogSplit.fatPct.toInt()}% F / ${prefs.quickLogSplit.carbPct.toInt()}% C"
            ) { showQuickLog = true }
            SettingsRow(
                "Weight Goal",
                buildString {
                    append(when (prefs.weightGoal) { "lose" -> "Lose weight"; "gain" -> "Gain muscle"; else -> "Maintain weight" })
                    prefs.weightGoalKg?.let { append(" · target ${it}kg") }
                }
            ) { showWeightGoal = true }
            SettingsRow(
                "Uploaded Excels",
                if (data.importBatches.isEmpty()) "No files imported yet" else "${data.importBatches.size} file(s) imported"
            ) { showUploads = true }
            SettingsRow(
                "Notes",
                if (prefs.notes.isBlank()) "Assumptions, substitutions, rules..." else prefs.notes.take(70) + if (prefs.notes.length > 70) "…" else ""
            ) { showNotes = true }
            Spacer(Modifier.height(8.dp))
            SettingsRow(
                "Reset All Data",
                "Permanently delete every ingredient, recipe, logged meal, and rotation — starts the app completely blank",
                danger = true
            ) { showResetConfirm = true }
        }
        Spacer(Modifier.height(24.dp))
    }

    if (showTargets) {
        DailyTargetDialog(prefs, onDismiss = { showTargets = false }) { updated -> vm.setPreferences(updated); showTargets = false }
    }
    if (showMealTypes) {
        MealTypesDialog(prefs, onDismiss = { showMealTypes = false }) { updated -> vm.setPreferences(updated); showMealTypes = false }
    }
    if (showQuickLog) {
        QuickLogSplitDialog(prefs, onDismiss = { showQuickLog = false }) { updated -> vm.setPreferences(updated); showQuickLog = false }
    }
    if (showNotes) {
        NotesDialog(prefs, onDismiss = { showNotes = false }) { updated -> vm.setPreferences(updated); showNotes = false }
    }
    if (showUploads) {
        UploadedExcelsDialog(
            batches = data.importBatches,
            onDismiss = { showUploads = false },
            onRemove = { batchId -> vm.removeImportBatch(batchId) },
            onReplace = { batchId -> vm.removeImportBatch(batchId); showUploads = false; showImport = true }
        )
    }
    if (showImport) {
        ImportExcelDialog(
            onDismiss = { showImport = false },
            existingIngredients = data.ingredients,
            mealTypes = data.preferences.mealTypes,
            onImport = { fileName, newIngredients, newRecipes -> vm.importData(fileName, newIngredients, newRecipes); showImport = false }
        )
    }
    if (showResetConfirm) {
        ResetConfirmDialog(onDismiss = { showResetConfirm = false }, onConfirm = { vm.resetAll(); showResetConfirm = false })
    }
    if (showWeightGoal) {
        WeightGoalDialog(prefs, onDismiss = { showWeightGoal = false }) { updated -> vm.setPreferences(updated); showWeightGoal = false }
    }
}

@Composable
private fun WeightGoalDialog(prefs: Preferences, onDismiss: () -> Unit, onSave: (Preferences) -> Unit) {
    var goal by remember { mutableStateOf(prefs.weightGoal) }
    var targetKg by remember { mutableStateOf(prefs.weightGoalKg?.toString() ?: "") }

    Dialog(onDismiss) {
        Column(
            Modifier
                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(20.dp))
                .padding(20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text("Weight Goal", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Text("Used by the Calculator.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("lose" to "Lose", "maintain" to "Maintain", "gain" to "Gain").forEach { (g, label) ->
                    FilterChip(selected = goal == g, onClick = { goal = g }, label = { Text(label) })
                }
            }
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = targetKg,
                onValueChange = { targetKg = it },
                label = { Text("Target body weight (kg) — optional") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onDismiss, Modifier.weight(1f)) { Text("Cancel") }
                Button(onClick = { onSave(prefs.copy(weightGoal = goal, weightGoalKg = targetKg.toDoubleOrNull())) }, Modifier.weight(1f)) { Text("Save") }
            }
        }
    }
}

@Composable
private fun ResetConfirmDialog(onDismiss: () -> Unit, onConfirm: () -> Unit) {
    Dialog(onDismiss) {
        Column(
            Modifier
                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(20.dp))
                .padding(20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text("Reset All Data?", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.error)
            Spacer(Modifier.height(10.dp))
            Text(
                "This permanently deletes every ingredient, recipe, logged meal, weekly rotation, and uploaded-file record, and resets your Preferences to their defaults. There's no undo.",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.height(20.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text("Cancel") }
                Button(
                    onClick = onConfirm,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Delete Everything") }
            }
        }
    }
}

@Composable
private fun UploadedExcelsDialog(
    batches: List<ImportBatch>,
    onDismiss: () -> Unit,
    onRemove: (String) -> Unit,
    onReplace: (String) -> Unit
) {
    val dateFormat = remember { java.text.SimpleDateFormat("d MMM yyyy", java.util.Locale.getDefault()) }
    Dialog(onDismiss) {
        Column(
            Modifier
                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(20.dp))
                .padding(20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text("Uploaded Excels", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Text(
                "Removing a file also removes the ingredients and recipes it added.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
            Spacer(Modifier.height(12.dp))
            if (batches.isEmpty()) {
                EmptyState("No Excel files imported yet.")
            }
            batches.forEach { batch ->
                Column(
                    Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    Text(batch.fileName, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(2.dp))
                    Text(
                        "${dateFormat.format(java.util.Date(batch.importedAt))} · ${batch.ingredientIds.size} ingredients, ${batch.recipeIds.size} recipes",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { onReplace(batch.id) }, modifier = Modifier.weight(1f)) { Text("Replace") }
                        ConfirmDeleteButton(onConfirm = { onRemove(batch.id) })
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text("Close") }
        }
    }
}

/** One target, applied to every day — replaces the old per-weekday rotation editor. */
@Composable
private fun DailyTargetDialog(prefs: Preferences, onDismiss: () -> Unit, onSave: (Preferences) -> Unit) {
    val t = prefs.dailyTarget
    var kcal by remember { mutableStateOf(t.kcal.toInt().toString()) }
    var protein by remember { mutableStateOf(t.protein.toInt().toString()) }
    var carbs by remember { mutableStateOf(t.carbs.toInt().toString()) }
    var fat by remember { mutableStateOf(t.fat.toInt().toString()) }

    Dialog(onDismiss) {
        Column(
            Modifier
                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(20.dp))
                .padding(20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text("Daily Targets", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Text(
                "Applies to every day. Use the Calculator on Home for a suggested starting point.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
            Spacer(Modifier.height(14.dp))
            OutlinedTextField(value = kcal, onValueChange = { kcal = it }, label = { Text("kcal") }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = protein, onValueChange = { protein = it }, label = { Text("Protein (g)") }, modifier = Modifier.weight(1f))
                OutlinedTextField(value = carbs, onValueChange = { carbs = it }, label = { Text("Carbs (g)") }, modifier = Modifier.weight(1f))
                OutlinedTextField(value = fat, onValueChange = { fat = it }, label = { Text("Fat (g)") }, modifier = Modifier.weight(1f))
            }
            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text("Cancel") }
                Button(
                    onClick = {
                        onSave(prefs.copy(dailyTarget = DayTargets(
                            kcal = kcal.toDoubleOrNull() ?: 0.0,
                            protein = protein.toDoubleOrNull() ?: 0.0,
                            carbs = carbs.toDoubleOrNull() ?: 0.0,
                            fat = fat.toDoubleOrNull() ?: 0.0
                        )))
                    },
                    modifier = Modifier.weight(1f)
                ) { Text("Save") }
            }
        }
    }
}

@Composable
private fun MealTypesDialog(prefs: Preferences, onDismiss: () -> Unit, onSave: (Preferences) -> Unit) {
    var types by remember { mutableStateOf(prefs.mealTypes) }
    var newType by remember { mutableStateOf("") }

    Dialog(onDismiss) {
        Column(
            Modifier
                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(20.dp))
                .padding(20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text("Meal Types", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Text(
                "These are the rows you log meals into on the Plan tab, and the tags recipes can be filtered by.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
            Spacer(Modifier.height(12.dp))
            types.forEachIndexed { idx, t ->
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    OutlinedTextField(
                        value = t,
                        onValueChange = { new -> types = types.toMutableList().also { it[idx] = new } },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    IconButton(onClick = { types = types.toMutableList().also { it.removeAt(idx) } }) {
                        Icon(Icons.Filled.Delete, contentDescription = "Remove")
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedTextField(value = newType, onValueChange = { newType = it }, label = { Text("New meal type") }, modifier = Modifier.weight(1f), singleLine = true)
                IconButton(onClick = {
                    if (newType.isNotBlank()) { types = types + newType.trim(); newType = "" }
                }) { Icon(Icons.Filled.Add, contentDescription = "Add") }
            }
            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text("Cancel") }
                Button(
                    onClick = { onSave(prefs.copy(mealTypes = types.filter { it.isNotBlank() })) },
                    modifier = Modifier.weight(1f),
                    enabled = types.any { it.isNotBlank() }
                ) { Text("Save") }
            }
        }
    }
}

@Composable
private fun QuickLogSplitDialog(prefs: Preferences, onDismiss: () -> Unit, onSave: (Preferences) -> Unit) {
    var proteinPct by remember { mutableStateOf(prefs.quickLogSplit.proteinPct.toInt().toString()) }
    var fatPct by remember { mutableStateOf(prefs.quickLogSplit.fatPct.toInt().toString()) }
    var carbPct by remember { mutableStateOf(prefs.quickLogSplit.carbPct.toInt().toString()) }
    val sum = (proteinPct.toIntOrNull() ?: 0) + (fatPct.toIntOrNull() ?: 0) + (carbPct.toIntOrNull() ?: 0)

    Dialog(onDismiss) {
        Column(
            Modifier
                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(20.dp))
                .padding(20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text("Quick-Log Macro Split", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Text(
                "When you log a meal by calories only, this split decides how those calories count toward protein/fat/carbs.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = proteinPct, onValueChange = { proteinPct = it }, label = { Text("Protein %") }, modifier = Modifier.weight(1f))
                OutlinedTextField(value = fatPct, onValueChange = { fatPct = it }, label = { Text("Fat %") }, modifier = Modifier.weight(1f))
                OutlinedTextField(value = carbPct, onValueChange = { carbPct = it }, label = { Text("Carb %") }, modifier = Modifier.weight(1f))
            }
            if (sum != 100) {
                Spacer(Modifier.height(4.dp))
                Text("Adds up to $sum% (aim for 100%)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
            }
            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text("Cancel") }
                Button(
                    onClick = {
                        onSave(prefs.copy(quickLogSplit = MacroSplit(
                            proteinPct = proteinPct.toDoubleOrNull() ?: 0.0,
                            fatPct = fatPct.toDoubleOrNull() ?: 0.0,
                            carbPct = carbPct.toDoubleOrNull() ?: 0.0
                        )))
                    },
                    modifier = Modifier.weight(1f)
                ) { Text("Save") }
            }
        }
    }
}

@Composable
private fun NotesDialog(prefs: Preferences, onDismiss: () -> Unit, onSave: (Preferences) -> Unit) {
    var notes by remember { mutableStateOf(prefs.notes) }
    Dialog(onDismiss) {
        Column(
            Modifier
                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(20.dp))
                .padding(20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text("Notes", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                placeholder = { Text("e.g. no tomato/ketchup, only Total 0%/2% Greek yogurt (no skyr), minimize protein powder scoops for cost...") },
                modifier = Modifier.fillMaxWidth().height(280.dp)
            )
            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text("Cancel") }
                Button(onClick = { onSave(prefs.copy(notes = notes)) }, modifier = Modifier.weight(1f)) { Text("Save") }
            }
        }
    }
}
