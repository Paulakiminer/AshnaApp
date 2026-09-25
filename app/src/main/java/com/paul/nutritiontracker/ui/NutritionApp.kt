@file:OptIn(ExperimentalMaterial3Api::class)

package com.paul.nutritiontracker.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.paul.nutritiontracker.NutritionViewModel
import com.paul.nutritiontracker.ui.screens.*

private enum class Tab(val label: String) {
    HOME("Home"), PLAN("Plan"), RECIPES("Recipes"), INGREDIENTS("Ingredients"), PREFERENCES("Preferences")
}

@Composable
fun NutritionApp(viewModel: NutritionViewModel) {
    var tab by remember { mutableStateOf(Tab.HOME) }
    var showMetricsScreen by remember { mutableStateOf(false) }
    var showBreakdownScreen by remember { mutableStateOf(false) }
    val data by viewModel.state.collectAsState()

    if (showMetricsScreen) {
        BodyMetricsScreen(modifier = Modifier, data = data.bodyMetrics, vm = viewModel, onBack = { showMetricsScreen = false })
        return
    }

    if (showBreakdownScreen) {
        NutritionBreakdownScreen(modifier = Modifier, data = data, onBack = { showBreakdownScreen = false })
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("NutriCalc", style = MaterialTheme.typography.titleLarge) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(selected = tab == Tab.HOME, onClick = { tab = Tab.HOME },
                    icon = { Icon(Icons.Filled.Home, null) }, label = { Text(Tab.HOME.label) })
                NavigationBarItem(selected = tab == Tab.PLAN, onClick = { tab = Tab.PLAN },
                    icon = { Icon(Icons.Filled.CalendarMonth, null) }, label = { Text(Tab.PLAN.label) })
                NavigationBarItem(selected = tab == Tab.RECIPES, onClick = { tab = Tab.RECIPES },
                    icon = { Icon(Icons.Filled.Restaurant, null) }, label = { Text(Tab.RECIPES.label) })
                NavigationBarItem(selected = tab == Tab.INGREDIENTS, onClick = { tab = Tab.INGREDIENTS },
                    icon = { Icon(Icons.Filled.Egg, null) }, label = { Text(Tab.INGREDIENTS.label) })
                NavigationBarItem(selected = tab == Tab.PREFERENCES, onClick = { tab = Tab.PREFERENCES },
                    icon = { Icon(Icons.Filled.Settings, null) }, label = { Text(Tab.PREFERENCES.label) })
            }
        }
    ) { padding ->
        val mod = Modifier.padding(padding)
        when (tab) {
            Tab.HOME -> HomeScreen(mod, data, viewModel, onOpenMetrics = { showMetricsScreen = true }, onOpenBreakdown = { showBreakdownScreen = true })
            Tab.PLAN -> PlanScreen(mod, data, viewModel)
            Tab.RECIPES -> RecipesScreen(mod, data, viewModel)
            Tab.INGREDIENTS -> IngredientsScreen(mod, data, viewModel)
            Tab.PREFERENCES -> PreferencesScreen(mod, data, viewModel)
        }
    }
}
