package com.paul.nutritiontracker.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.paul.nutritiontracker.NutritionViewModel
import com.paul.nutritiontracker.data.*
import com.paul.nutritiontracker.ui.theme.*
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import kotlin.math.absoluteValue

@Composable
fun HomeScreen(
    modifier: Modifier,
    data: AppData,
    vm: NutritionViewModel,
    onOpenMetrics: () -> Unit,
    onOpenBreakdown: () -> Unit
) {
    val ingById = remember(data.ingredients) { data.ingredients.associateBy { it.id } }
    val recById = remember(data.recipes) { data.recipes.associateBy { it.id } }
    val today = remember { LocalDate.now() }
    val todayTotals = remember(data.rotation, data.overrides, data.recipes, data.ingredients) {
        dateTotals(today, data, recById, ingById)
    }
    val targets = data.preferences.dailyTarget

    val hasBatches = data.importBatches.isNotEmpty()
    // Import card is only shown if there are no batches
    // Never show the "Import complete" animation when returning to home
    var showImportCard by remember { mutableStateOf(!hasBatches) }
    var importJustDone by remember { mutableStateOf(false) }

    var showImport by remember { mutableStateOf(false) }
    var showQuickLog by remember { mutableStateOf(false) }
    var showCalculator by remember { mutableStateOf(false) }

    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
    ) {
        Spacer(Modifier.height(4.dp))
        Text("Today · ${today.format(DateTimeFormatter.ofPattern("EEE, d MMM"))}", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(12.dp))

        // Macro stat cards — tap any to open the nutrition breakdown
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ClickableStatCard("kcal", "${todayTotals.kcal.toInt()}", Modifier.weight(1f), KcalColor, onOpenBreakdown)
            ClickableStatCard("Protein", "${todayTotals.protein.toInt()}g", Modifier.weight(1f), ProteinColor, onOpenBreakdown)
            ClickableStatCard("Carbs", "${todayTotals.carbs.toInt()}g", Modifier.weight(1f), CarbsColor, onOpenBreakdown)
            ClickableStatCard("Fat", "${todayTotals.fat.toInt()}g", Modifier.weight(1f), FatColor, onOpenBreakdown)
        }

        Spacer(Modifier.height(20.dp))

        Text("Today's Goal Progress", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(10.dp))
        Row(
            Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp))
                .padding(vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            MacroHemisphere("kcal", todayTotals.kcal, targets.kcal, KcalColor)
            MacroHemisphere("Protein", todayTotals.protein, targets.protein, ProteinColor)
            MacroHemisphere("Carbs", todayTotals.carbs, targets.carbs, CarbsColor)
            MacroHemisphere("Fat", todayTotals.fat, targets.fat, FatColor)
        }

        Spacer(Modifier.height(20.dp))

        Text("Monthly Overview", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(10.dp))
        MonthlyCalendar(today = today, data = data, recById = recById, ingById = ingById)

        Spacer(Modifier.height(20.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Surface(onClick = { showQuickLog = true }, shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.weight(1f)) {
                Column(Modifier.padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Filled.Add, null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                    Spacer(Modifier.height(4.dp))
                    Text("Quick Log", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                }
            }
            Surface(onClick = { showCalculator = true }, shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.secondaryContainer, modifier = Modifier.weight(1f)) {
                Column(Modifier.padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Filled.Calculate, null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
                    Spacer(Modifier.height(4.dp))
                    Text("Calculator", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSecondaryContainer)
                }
            }
            Surface(onClick = onOpenMetrics, shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.tertiaryContainer, modifier = Modifier.weight(1f)) {
                Column(Modifier.padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Filled.FitnessCenter, null, tint = MaterialTheme.colorScheme.onTertiaryContainer)
                    Spacer(Modifier.height(4.dp))
                    Text("Metrics", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onTertiaryContainer)
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        AnimatedVisibility(
            visible = showImportCard,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut(tween(600)) + shrinkVertically(tween(600))
        ) {
            if (importJustDone) {
                Row(
                    Modifier.fillMaxWidth().background(Basil.copy(alpha = 0.15f), RoundedCornerShape(16.dp)).padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Filled.Check, null, tint = Basil, modifier = Modifier.size(28.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Import complete!", fontWeight = FontWeight.SemiBold, color = Basil)
                }
            } else {
                Row(
                    Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), RoundedCornerShape(16.dp)).padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Import from Excel", fontWeight = FontWeight.SemiBold)
                        Text("Bring in ingredients and recipes from a spreadsheet.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f))
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = { showImport = true }) {
                        Icon(Icons.Filled.UploadFile, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Import")
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))
    }

    if (showImport) {
        ImportExcelDialog(
            onDismiss = { showImport = false },
            existingIngredients = data.ingredients,
            mealTypes = data.preferences.mealTypes,
            onImport = { fileName, newIng, newRec ->
                vm.importData(fileName, newIng, newRec)
                importJustDone = true
                showImport = false
            }
        )
    }

    if (showQuickLog) {
        QuickLogDialog(
            mealTypes = data.preferences.mealTypes,
            recipes = data.recipes,
            ingById = ingById,
            defaultMealType = data.preferences.mealTypes.firstOrNull() ?: "Lunch",
            onDismiss = { showQuickLog = false },
            onSave = { mealType, entry -> vm.setMealEntry(today, mealType, entry, false); showQuickLog = false }
        )
    }

    if (showCalculator) {
        DietGoalsCalculatorDialog(
            weightGoal = data.preferences.weightGoal,
            onDismiss = { showCalculator = false },
            onApply = { newTargets, newGoal ->
                vm.setPreferences(data.preferences.copy(dailyTarget = newTargets, weightGoal = newGoal))
                showCalculator = false
            }
        )
    }
}

@Composable
private fun ClickableStatCard(label: String, value: String, modifier: Modifier, accent: Color, onClick: () -> Unit) {
    Surface(onClick = onClick, color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(14.dp), modifier = modifier) {
        Column(Modifier.padding(vertical = 10.dp, horizontal = 6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = accent)
            Spacer(Modifier.height(2.dp))
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun MacroHemisphere(label: String, value: Double, target: Double, color: Color) {
    val fraction = if (target > 0) (value / target).toFloat().coerceIn(0f, 1f) else 0f
    val trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Canvas(modifier = Modifier.size(width = 72.dp, height = 40.dp)) {
            val sw = 9.dp.toPx()
            val diam = size.width - sw
            val tl = Offset(sw / 2f, sw / 2f)
            val sz = Size(diam, diam)
            drawArc(trackColor, 180f, 180f, false, tl, sz, style = Stroke(sw, cap = StrokeCap.Round))
            drawArc(color, 180f, 180f * fraction, false, tl, sz, style = Stroke(sw, cap = StrokeCap.Round))
        }
        Spacer(Modifier.height(4.dp))
        Text("${value.toInt()}/${target.toInt()}", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = color)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
    }
}

@Composable
private fun MonthlyCalendar(today: LocalDate, data: AppData, recById: Map<String, Recipe>, ingById: Map<String, Ingredient>) {
    val month = YearMonth.from(today)
    val firstDayOffset = month.atDay(1).dayOfWeek.value - 1
    val daysInMonth = month.lengthOfMonth()
    val targets = data.preferences.dailyTarget

    val hitDays = remember(data, today) {
        (1..daysInMonth).mapNotNull { day ->
            val date = month.atDay(day)
            if (date.isAfter(today)) return@mapNotNull null
            val t = dateTotals(date, data, recById, ingById)
            val hit = targets.kcal > 0 && t.kcal >= targets.kcal * 0.9 &&
                      t.protein >= targets.protein * 0.9 &&
                      t.carbs >= targets.carbs * 0.9 &&
                      t.fat >= targets.fat * 0.9
            day to hit
        }.toMap()
    }

    val hitColor = Basil
    val missColor = MaterialTheme.colorScheme.error.copy(alpha = 0.45f)
    val futureColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
    val emptyColor = MaterialTheme.colorScheme.surfaceVariant
    val todayRing = MaterialTheme.colorScheme.primary

    Box(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp)).padding(12.dp)) {
        val rows = (firstDayOffset + daysInMonth + 6) / 7
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            for (row in 0 until rows) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    for (col in 0..6) {
                        val day = row * 7 + col - firstDayOffset + 1
                        val valid = day in 1..daysInMonth
                        val date = if (valid) month.atDay(day) else null
                        val isFuture = date?.isAfter(today) == true
                        val isToday = date == today
                        val circleColor = when {
                            !valid -> Color.Transparent
                            isFuture -> futureColor
                            hitDays[day] == true -> hitColor
                            hitDays[day] == false -> missColor
                            else -> emptyColor
                        }
                        Canvas(Modifier.weight(1f).aspectRatio(1f)) {
                            val r = size.minDimension / 2f
                            drawCircle(circleColor, r)
                            if (isToday) drawCircle(todayRing, r, style = Stroke(2.5.dp.toPx()))
                        }
                    }
                }
            }
            Spacer(Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                LegendDot(hitColor, "Goal hit")
                LegendDot(missColor, "Missed")
                LegendDot(futureColor, "Upcoming")
            }
        }
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(Modifier.size(8.dp).background(color, CircleShape))
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
    }
}

@Composable
private fun QuickLogDialog(
    mealTypes: List<String>,
    recipes: List<Recipe>,
    ingById: Map<String, Ingredient>,
    defaultMealType: String,
    onDismiss: () -> Unit,
    onSave: (String, MealEntry) -> Unit
) {
    var selectedMealType by remember { mutableStateOf(defaultMealType) }
    var mode by remember { mutableStateOf("recipe") }
    var selectedRecipeId by remember { mutableStateOf<String?>(null) }
    var kcalText by remember { mutableStateOf("") }

    val filteredRecipes = remember(recipes, selectedMealType) {
        recipes.filter { it.mealTypes.isEmpty() || it.mealTypes.contains(selectedMealType) }
    }
    val selectedRecipe = filteredRecipes.find { it.id == selectedRecipeId }

    Dialog(onDismiss) {
        Column(
            Modifier
                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(20.dp))
                .padding(20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text("Quick Log · Today", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(12.dp))
            Text("Meal type", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
            Spacer(Modifier.height(4.dp))
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                mealTypes.forEach { mt -> FilterChip(selected = selectedMealType == mt, onClick = { selectedMealType = mt }, label = { Text(mt) }) }
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                FilterChip(selected = mode == "recipe", onClick = { mode = "recipe" }, label = { Text("Recipe") })
                FilterChip(selected = mode == "quick", onClick = { mode = "quick" }, label = { Text("Calories only") })
            }
            Spacer(Modifier.height(12.dp))
            if (mode == "recipe") {
                SearchableDropdownField(
                    label = "Search $selectedMealType recipes",
                    options = filteredRecipes,
                    selectedName = selectedRecipe?.name ?: "",
                    displayName = { it.name },
                    onSelect = { selectedRecipeId = it.id },
                    modifier = Modifier.fillMaxWidth()
                )
                selectedRecipe?.let {
                    val m = recipeMacros(it, ingById)
                    Spacer(Modifier.height(8.dp))
                    MacroChipRow(m.kcal, m.protein, m.carbs, m.fat)
                }
            } else {
                OutlinedTextField(value = kcalText, onValueChange = { kcalText = it }, label = { Text("Calories") }, modifier = Modifier.fillMaxWidth())
            }
            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onDismiss, Modifier.weight(1f)) { Text("Cancel") }
                Button(
                    onClick = {
                        val entry = if (mode == "recipe") selectedRecipeId?.let { MealEntry(recipeId = it) }
                                    else kcalText.toDoubleOrNull()?.let { MealEntry(quickKcal = it) }
                        entry?.let { onSave(selectedMealType, it) }
                    },
                    modifier = Modifier.weight(1f),
                    enabled = (mode == "recipe" && selectedRecipeId != null) || (mode == "quick" && kcalText.toDoubleOrNull() != null)
                ) { Text("Log it") }
            }
        }
    }
}

// ── Diet Goals Calculator: one continuously-scrollable screen, minimal text ──

private data class GoalDefaults(val proteinPct: Float, val fatPct: Float)

private fun defaultsFor(goal: String): GoalDefaults = when (goal) {
    "lose" -> GoalDefaults(35f, 25f)
    "gain" -> GoalDefaults(30f, 25f)
    else -> GoalDefaults(30f, 30f)
}

@Composable
fun DietGoalsCalculatorDialog(
    weightGoal: String,
    onDismiss: () -> Unit,
    onApply: (DayTargets, String) -> Unit
) {
    var age by remember { mutableStateOf("") }
    var isMale by remember { mutableStateOf(true) }
    var weightKg by remember { mutableStateOf("") }
    var heightCm by remember { mutableStateOf("") }
    var activityLevel by remember { mutableStateOf(1.55) }
    var goal by remember { mutableStateOf(weightGoal) }
    var proteinPct by remember { mutableStateOf(defaultsFor(weightGoal).proteinPct) }
    var fatPct by remember { mutableStateOf(defaultsFor(weightGoal).fatPct) }

    val carbPct = (100f - proteinPct - fatPct).coerceAtLeast(0f)
    val totalPct = proteinPct + fatPct + carbPct
    val pctError = (totalPct - 100f).absoluteValue > 0.01f

    val activityOptions = listOf(1.2 to "Sedentary", 1.375 to "Light", 1.55 to "Moderate", 1.725 to "Active", 1.9 to "Extreme")

    val tdee = remember(age, isMale, weightKg, heightCm, activityLevel) {
        val w = weightKg.toDoubleOrNull() ?: return@remember null
        val h = heightCm.toDoubleOrNull() ?: return@remember null
        val a = age.toDoubleOrNull() ?: return@remember null
        val bmr = if (isMale) 10 * w + 6.25 * h - 5 * a + 5 else 10 * w + 6.25 * h - 5 * a - 161
        bmr * activityLevel
    }

    val targetKcal = tdee?.let {
        when (goal) { "lose" -> it - 400.0; "gain" -> it + 350.0; else -> it }
    }

    val resultTargets = targetKcal?.let { k ->
        DayTargets(
            kcal = k,
            protein = k * proteinPct / 100.0 / 4.0,
            carbs = k * carbPct / 100.0 / 4.0,
            fat = k * fatPct / 100.0 / 9.0
        )
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface)
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
        ) {
            Spacer(Modifier.height(8.dp))
            Text("Diet Goals Calculator", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(20.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = age, onValueChange = { age = it }, label = { Text("Age") }, modifier = Modifier.weight(1f), singleLine = true)
                OutlinedTextField(value = weightKg, onValueChange = { weightKg = it }, label = { Text("kg") }, modifier = Modifier.weight(1f), singleLine = true)
                OutlinedTextField(value = heightCm, onValueChange = { heightCm = it }, label = { Text("cm") }, modifier = Modifier.weight(1f), singleLine = true)
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = isMale, onClick = { isMale = true }, label = { Text("Male") })
                FilterChip(selected = !isMale, onClick = { isMale = false }, label = { Text("Female") })
            }

            Spacer(Modifier.height(20.dp))
            Text("Activity", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
            Spacer(Modifier.height(6.dp))
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                activityOptions.forEach { (factor, label) ->
                    FilterChip(selected = activityLevel == factor, onClick = { activityLevel = factor }, label = { Text(label) })
                }
            }

            Spacer(Modifier.height(16.dp))
            Text("Goal", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("lose" to "Lose", "maintain" to "Maintain", "gain" to "Gain").forEach { (g, label) ->
                    FilterChip(
                        selected = goal == g,
                        onClick = {
                            goal = g
                            val d = defaultsFor(g)
                            proteinPct = d.proteinPct
                            fatPct = d.fatPct
                        },
                        label = { Text(label) }
                    )
                }
            }

            if (resultTargets != null) {
                Spacer(Modifier.height(24.dp))
                Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                Spacer(Modifier.height(20.dp))

                Text("${resultTargets.kcal.toInt()} kcal / day", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = Basil)
                Spacer(Modifier.height(16.dp))

                PercentBarRow("Protein", proteinPct, ProteinColor)
                PercentBarRow("Fat", fatPct, FatColor)
                PercentBarRow("Carbs", carbPct, CarbsColor)

                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Total: ${totalPct.toInt()}%", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                    if (pctError) {
                        Text("must be 100%", style = MaterialTheme.typography.bodyMedium, color = Basil)
                    }
                }

                Spacer(Modifier.height(16.dp))
                MacroChipRow(resultTargets.kcal, resultTargets.protein, resultTargets.carbs, resultTargets.fat)
            }

            Spacer(Modifier.height(28.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onDismiss, Modifier.weight(1f)) { Text("Close") }
                Button(
                    onClick = { resultTargets?.let { onApply(it, goal) } },
                    modifier = Modifier.weight(1f),
                    enabled = resultTargets != null && !pctError
                ) { Text("Apply") }
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun PercentBarRow(label: String, value: Float, color: Color) {
    Column(Modifier.padding(vertical = 4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            MacroIcon(label.lowercase(), color, 16.dp)
            Spacer(Modifier.width(6.dp))
            Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            Text("${value.toInt()}%", style = MaterialTheme.typography.bodyMedium, color = color, fontWeight = FontWeight.Medium)
        }
        Spacer(Modifier.height(4.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .height(8.dp)
                .background(color.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
        ) {
            Box(
                Modifier
                    .width(100.dp * (value / 100f))
                    .height(8.dp)
                    .background(color, RoundedCornerShape(4.dp))
            )
        }
    }
}
