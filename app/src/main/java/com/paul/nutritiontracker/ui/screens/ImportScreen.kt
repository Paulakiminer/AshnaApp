@file:OptIn(ExperimentalMaterial3Api::class)

package com.paul.nutritiontracker.ui.screens

import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.paul.nutritiontracker.data.Ingredient
import com.paul.nutritiontracker.data.ImportedSheet
import com.paul.nutritiontracker.data.Recipe
import com.paul.nutritiontracker.data.RecipeItem
import com.paul.nutritiontracker.data.XlsxReader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val INGREDIENT_FIELDS = listOf(
    "name" to "Name *", "kcal100" to "kcal/100g *", "protein100" to "Protein/100g *",
    "carbs100" to "Carbs/100g *", "fat100" to "Fat/100g *", "serving" to "Serving size (g)", "source" to "Source note"
)
private val RECIPE_DIRECT_FIELDS = listOf(
    "name" to "Name *", "kcal" to "kcal *", "protein" to "Protein (g) *", "carbs" to "Carbs (g) *", "fat" to "Fat (g) *", "mealTypes" to "Meal type(s)"
)
private val RECIPE_FROM_ING_FIELDS = listOf(
    "name" to "Recipe Name *", "ingredient" to "Ingredient Name *", "qty" to "Quantity (g) *", "mealTypes" to "Meal type(s)"
)

private fun colLabels(sheet: ImportedSheet?, useHeader: Boolean): List<String> {
    if (sheet == null) return emptyList()
    val maxCols = sheet.rows.maxOfOrNull { it.size } ?: 0
    val header = if (useHeader) sheet.rows.firstOrNull() else null
    return (0 until maxCols).map { i ->
        val h = header?.getOrNull(i)
        if (!h.isNullOrBlank()) h else "Column ${('A' + i)}"
    }
}

private fun colDouble(row: List<String>, colIdx: Int?): Double {
    if (colIdx == null || colIdx < 0) return 0.0
    return row.getOrNull(colIdx)?.trim()?.replace(",", ".")?.toDoubleOrNull() ?: 0.0
}

private fun colString(row: List<String>, colIdx: Int?): String {
    if (colIdx == null || colIdx < 0) return ""
    return row.getOrNull(colIdx)?.trim() ?: ""
}

private fun parseMealTypes(raw: String, options: List<String>): List<String> {
    if (raw.isBlank()) return emptyList()
    val tokens = raw.split(",", "/", ";").map { it.trim() }.filter { it.isNotBlank() }
    return tokens.mapNotNull { tok -> options.find { it.equals(tok, ignoreCase = true) } }.distinct()
}

private fun queryFileName(context: android.content.Context, uri: Uri): String {
    return try {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (idx >= 0 && cursor.moveToFirst()) cursor.getString(idx) else null
        } ?: uri.lastPathSegment ?: "Imported file"
    } catch (e: Exception) {
        uri.lastPathSegment ?: "Imported file"
    }
}

@Composable
fun ImportExcelDialog(
    onDismiss: () -> Unit,
    existingIngredients: List<Ingredient>,
    mealTypes: List<String>,
    onImport: (String, List<Ingredient>, List<Recipe>) -> Unit
) {
    val context = LocalContext.current

    var uri by remember { mutableStateOf<Uri?>(null) }
    var fileName by remember { mutableStateOf("") }
    var sheets by remember { mutableStateOf<List<ImportedSheet>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var skipHeader by remember { mutableStateOf(true) }
    var ingredientSheetIdx by remember { mutableIntStateOf(-1) }
    var recipeSheetIdx by remember { mutableIntStateOf(-1) }
    var recipeMode by remember { mutableStateOf("direct") } // "direct" | "fromIngredients"
    var ingCols by remember { mutableStateOf(mapOf<String, Int>()) }
    var recCols by remember { mutableStateOf(mapOf<String, Int>()) }
    var resultMessage by remember { mutableStateOf<String?>(null) }
    var resultIsWarning by remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { picked ->
        if (picked != null) { uri = picked; error = null; resultMessage = null; resultIsWarning = false }
    }

    LaunchedEffect(uri) {
        val u = uri ?: return@LaunchedEffect
        loading = true
        fileName = queryFileName(context, u)
        try {
            val result = withContext(Dispatchers.IO) { XlsxReader.read(context, u) }
            sheets = result
            val ingKeywords = listOf("ingredient", "υλικ")
            val recKeywords = listOf("recipe", "meal", "food", "dish", "συνταγ", "φαγητ", "γευμα")
            ingredientSheetIdx = result.indexOfFirst { s -> ingKeywords.any { s.name.contains(it, ignoreCase = true) } }
            recipeSheetIdx = result.indexOfFirst { s -> recKeywords.any { s.name.contains(it, ignoreCase = true) } }
            // If neither sheet name gave it away and there are exactly two sheets, assume the
            // conventional order: ingredients first, recipes second.
            if (ingredientSheetIdx == -1 && recipeSheetIdx == -1 && result.size == 2) {
                ingredientSheetIdx = 0
                recipeSheetIdx = 1
            }
        } catch (e: Exception) {
            error = "Couldn't read that file — make sure it's a valid .xlsx export (not .xls or a locked/protected sheet)."
            sheets = emptyList()
        }
        loading = false
    }

    val ingSheet = sheets.getOrNull(ingredientSheetIdx)
    val recSheet = sheets.getOrNull(recipeSheetIdx)
    val recipeFields = if (recipeMode == "direct") RECIPE_DIRECT_FIELDS else RECIPE_FROM_ING_FIELDS

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(
            Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.9f)
                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(20.dp))
                .padding(20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text("Import from Excel", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Text(
                "Pick a .xlsx file, then map its columns below.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
            )
            Spacer(Modifier.height(16.dp))

            Button(onClick = { launcher.launch(arrayOf("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "*/*")) }) {
                Icon(Icons.Filled.UploadFile, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(if (uri == null) "Choose .xlsx file" else "Choose a different file")
            }
            if (fileName.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                Text(fileName, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
            }

            if (loading) {
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) { CircularProgressIndicator(modifier = Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text("Reading file...") }
            }
            error?.let {
                Spacer(Modifier.height(12.dp))
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            }
            resultMessage?.let {
                Spacer(Modifier.height(12.dp))
                Text(it, color = if (resultIsWarning) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            }

            if (sheets.isNotEmpty()) {
                Spacer(Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = skipHeader, onCheckedChange = { skipHeader = it })
                    Text("First row is a header (not data)")
                }
                Spacer(Modifier.height(12.dp))

                SheetPicker("Ingredients sheet", sheets, ingredientSheetIdx) { ingredientSheetIdx = it }
                if (ingSheet == null) {
                    Spacer(Modifier.height(4.dp))
                    Text("No ingredients will be imported — pick a sheet above if you have one.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                }
                Spacer(Modifier.height(8.dp))
                SheetPicker("Recipes sheet", sheets, recipeSheetIdx) { recipeSheetIdx = it }
                if (recSheet == null) {
                    Spacer(Modifier.height(4.dp))
                    Text("No recipes will be imported — pick a sheet above if you have one.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                }

                if (ingSheet != null) {
                    Spacer(Modifier.height(16.dp))
                    Text("Map ingredient columns", fontWeight = FontWeight.SemiBold)
                    val labels = remember(ingSheet, skipHeader) { colLabels(ingSheet, skipHeader) }
                    INGREDIENT_FIELDS.forEach { (key, label) ->
                        Spacer(Modifier.height(6.dp))
                        ColumnPicker(label, labels, ingCols[key] ?: -1) { idx -> ingCols = ingCols + (key to idx) }
                    }
                }

                if (recSheet != null) {
                    Spacer(Modifier.height(16.dp))
                    Text("Recipes are...", fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FilterChip(selected = recipeMode == "direct", onClick = { recipeMode = "direct" }, label = { Text("Total macros per row") })
                        FilterChip(selected = recipeMode == "fromIngredients", onClick = { recipeMode = "fromIngredients" }, label = { Text("Built from ingredients") })
                    }
                    if (recipeMode == "fromIngredients") {
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "One row per ingredient, grouped by matching Recipe Name (e.g. several \"Chicken Bowl\" rows, one per ingredient).",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    Text("Map recipe columns", fontWeight = FontWeight.SemiBold)
                    val labels = remember(recSheet, skipHeader) { colLabels(recSheet, skipHeader) }
                    recipeFields.forEach { (key, label) ->
                        Spacer(Modifier.height(6.dp))
                        ColumnPicker(label, labels, recCols[key] ?: -1) { idx -> recCols = recCols + (key to idx) }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text("Cancel") }
                val canImport = (ingSheet != null && (ingCols["name"] ?: -1) >= 0) ||
                    (recSheet != null && recipeMode == "direct" && (recCols["name"] ?: -1) >= 0) ||
                    (recSheet != null && recipeMode == "fromIngredients" && (recCols["name"] ?: -1) >= 0 && (recCols["ingredient"] ?: -1) >= 0)
                Button(
                    onClick = {
                        val ingRows = if (ingSheet != null) ingSheet.rows.drop(if (skipHeader) 1 else 0) else emptyList()
                        val newIngredients = ingRows.mapNotNull { row ->
                            val name = colString(row, ingCols["name"])
                            if (name.isBlank()) return@mapNotNull null
                            Ingredient(
                                name = name,
                                kcal100 = colDouble(row, ingCols["kcal100"]),
                                protein100 = colDouble(row, ingCols["protein100"]),
                                carbs100 = colDouble(row, ingCols["carbs100"]),
                                fat100 = colDouble(row, ingCols["fat100"]),
                                serving = colDouble(row, ingCols["serving"]).let { if (it <= 0.0) 100.0 else it },
                                source = colString(row, ingCols["source"])
                            )
                        }

                        val recRows = if (recSheet != null) recSheet.rows.drop(if (skipHeader) 1 else 0) else emptyList()
                        var skippedIngredientMatches = 0

                        val newRecipes: List<Recipe> = if (recipeMode == "direct") {
                            recRows.mapNotNull { row ->
                                val name = colString(row, recCols["name"])
                                if (name.isBlank()) return@mapNotNull null
                                Recipe(
                                    name = name,
                                    mode = "direct",
                                    kcal = colDouble(row, recCols["kcal"]),
                                    protein = colDouble(row, recCols["protein"]),
                                    carbs = colDouble(row, recCols["carbs"]),
                                    fat = colDouble(row, recCols["fat"]),
                                    mealTypes = parseMealTypes(colString(row, recCols["mealTypes"]), mealTypes)
                                )
                            }
                        } else {
                            val combined = existingIngredients + newIngredients
                            val groups = linkedMapOf<String, MutableList<Pair<String, Double>>>()
                            val mealTypesByRecipe = mutableMapOf<String, String>()
                            recRows.forEach { row ->
                                val rName = colString(row, recCols["name"])
                                val ingName = colString(row, recCols["ingredient"])
                                if (rName.isBlank() || ingName.isBlank()) return@forEach
                                groups.getOrPut(rName) { mutableListOf() }.add(ingName to colDouble(row, recCols["qty"]))
                                val mt = colString(row, recCols["mealTypes"])
                                if (mt.isNotBlank()) mealTypesByRecipe.putIfAbsent(rName, mt)
                            }
                            groups.map { (rName, ingList) ->
                                val items = ingList.mapNotNull { (ingName, qty) ->
                                    val match = combined.find { it.name.equals(ingName, ignoreCase = true) }
                                    if (match == null) { skippedIngredientMatches++; null } else RecipeItem(ingredientId = match.id, qty = qty)
                                }
                                Recipe(
                                    name = rName,
                                    mode = "compose",
                                    items = items,
                                    mealTypes = parseMealTypes(mealTypesByRecipe[rName] ?: "", mealTypes)
                                )
                            }
                        }

                        val warningNote = when {
                            recSheet != null && newRecipes.isEmpty() -> " ⚠ 0 recipes came from the selected sheet — double-check the header checkbox and column mapping."
                            ingSheet != null && newIngredients.isEmpty() -> " ⚠ 0 ingredients came from the selected sheet — double-check the header checkbox and column mapping."
                            else -> ""
                        }
                        resultIsWarning = warningNote.isNotEmpty()
                        resultMessage = "Imported ${newIngredients.size} ingredient(s) and ${newRecipes.size} recipe(s)." +
                            (if (skippedIngredientMatches > 0) " $skippedIngredientMatches ingredient reference(s) couldn't be matched by name and were skipped." else "") +
                            warningNote
                        onImport(fileName.ifBlank { "Imported file" }, newIngredients, newRecipes)
                    },
                    modifier = Modifier.weight(1f),
                    enabled = canImport
                ) { Text("Import") }
            }
        }
    }
}

@Composable
private fun SheetPicker(label: String, sheets: List<ImportedSheet>, selected: Int, onSelect: (Int) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = sheets.getOrNull(selected)?.name ?: "— None —",
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            modifier = Modifier.menuAnchor().fillMaxWidth(),
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) }
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(text = { Text("— None —") }, onClick = { onSelect(-1); expanded = false })
            sheets.forEachIndexed { idx, s ->
                DropdownMenuItem(text = { Text(s.name) }, onClick = { onSelect(idx); expanded = false })
            }
        }
    }
}

@Composable
private fun ColumnPicker(label: String, options: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = options.getOrNull(selected) ?: "— None —",
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            modifier = Modifier.menuAnchor().fillMaxWidth(),
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) }
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(text = { Text("— None —") }, onClick = { onSelect(-1); expanded = false })
            options.forEachIndexed { idx, o ->
                DropdownMenuItem(text = { Text(o) }, onClick = { onSelect(idx); expanded = false })
            }
        }
    }
}
