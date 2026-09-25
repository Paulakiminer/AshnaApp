package com.paul.nutritiontracker.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.paul.nutritiontracker.data.AppData
import com.paul.nutritiontracker.data.FoodContribution
import com.paul.nutritiontracker.data.dateContributions
import com.paul.nutritiontracker.ui.theme.Tangerine
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NutritionBreakdownScreen(modifier: Modifier, data: AppData, onBack: () -> Unit) {
    val today = remember { LocalDate.now() }
    val recById = remember(data.recipes) { data.recipes.associateBy { it.id } }
    val ingById = remember(data.ingredients) { data.ingredients.associateBy { it.id } }

    val contributions = remember(data, today) { dateContributions(today, data, recById, ingById) }

    val totalProtein = contributions.sumOf { it.macros.protein }
    val totalCarbs = contributions.sumOf { it.macros.carbs }
    val totalFat = contributions.sumOf { it.macros.fat }
    val totalSugar = contributions.sumOf { it.macros.sugar }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Nutrition Breakdown") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, "Back") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            Text(
                today.format(DateTimeFormatter.ofPattern("EEE, d MMM")),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
            Spacer(Modifier.height(12.dp))

            if (contributions.isEmpty()) {
                EmptyState("Nothing logged today yet.")
            } else {
                MacroSection("Protein", totalProtein, ProteinColor, "protein", contributions) { it.macros.protein }
                Spacer(Modifier.height(16.dp))
                MacroSection("Carbs", totalCarbs, CarbsColor, "carbs", contributions) { it.macros.carbs }
                Spacer(Modifier.height(16.dp))
                MacroSection("Fat", totalFat, FatColor, "fat", contributions) { it.macros.fat }
                Spacer(Modifier.height(16.dp))
                MacroSection("Sugar", totalSugar, Tangerine, null, contributions) { it.macros.sugar }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun MacroSection(
    label: String,
    total: Double,
    color: Color,
    iconType: String?,
    contributions: List<FoodContribution>,
    valueOf: (FoodContribution) -> Double
) {
    val sorted = remember(contributions) {
        contributions.filter { valueOf(it) > 0.05 }.sortedByDescending { valueOf(it) }
    }

    Column(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp))
            .padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (iconType != null) {
                MacroIcon(iconType, color, 20.dp)
                Spacer(Modifier.width(6.dp))
            } else {
                Box(Modifier.size(10.dp).background(color, RoundedCornerShape(5.dp)))
                Spacer(Modifier.width(8.dp))
            }
            Text(label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.weight(1f))
            Text("${"%.1f".format(total)}g", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = color)
        }
        if (sorted.isEmpty()) {
            Spacer(Modifier.height(6.dp))
            Text("No contribution today.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
        } else {
            Spacer(Modifier.height(10.dp))
            sorted.forEach { c ->
                val value = valueOf(c)
                val pct = if (total > 0) (value / total * 100).toInt() else 0
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(c.name, style = MaterialTheme.typography.bodyMedium)
                        Text(c.mealType, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                    }
                    Text("${"%.1f".format(value)}g", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, color = color)
                    Spacer(Modifier.width(8.dp))
                    Text("$pct%", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f), modifier = Modifier.width(32.dp))
                }
                Box(Modifier.fillMaxWidth().height(4.dp).background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(2.dp))) {
                    Box(
                        Modifier
                            .fillMaxWidth(fraction = (pct / 100f).coerceIn(0f, 1f))
                            .height(4.dp)
                            .background(color, RoundedCornerShape(2.dp))
                    )
                }
                Spacer(Modifier.height(6.dp))
            }
        }
    }
}
