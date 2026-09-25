package com.paul.nutritiontracker.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.paul.nutritiontracker.NutritionViewModel
import com.paul.nutritiontracker.data.BodyMetricEntry
import com.paul.nutritiontracker.ui.theme.Basil
import com.paul.nutritiontracker.ui.theme.Blueberry
import com.paul.nutritiontracker.ui.theme.Butter
import com.paul.nutritiontracker.ui.theme.Tangerine
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BodyMetricsScreen(modifier: Modifier, data: Map<String, BodyMetricEntry>, vm: NutritionViewModel, onBack: () -> Unit) {
    val today = remember { LocalDate.now() }
    var showLogDialog by remember { mutableStateOf(false) }

    // Build sorted history (last 60 days)
    val history = remember(data) {
        data.entries
            .mapNotNull { (k, v) -> runCatching { LocalDate.parse(k) to v }.getOrNull() }
            .sortedBy { it.first }
            .takeLast(60)
    }

    val weightSeries = remember(history) { history.mapNotNull { (d, e) -> e.weightKg?.let { d to it } } }
    val fatSeries = remember(history) { history.mapNotNull { (d, e) -> e.bodyFatPct?.let { d to it } } }
    val muscleSeries = remember(history) { history.mapNotNull { (d, e) -> e.muscleMassKg?.let { d to it } } }
    val waterSeries = remember(history) { history.mapNotNull { (d, e) -> e.waterPct?.let { d to it } } }

    val latestEntry = history.lastOrNull()?.second

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Body Metrics") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, "Back") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showLogDialog = true }, containerColor = MaterialTheme.colorScheme.primary) {
                Icon(Icons.Filled.Add, "Log measurements")
            }
        }
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            // Latest snapshot summary
            if (latestEntry != null) {
                val dateLabel = history.last().first.format(DateTimeFormatter.ofPattern("d MMM yyyy"))
                Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Latest · $dateLabel", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f))
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            latestEntry.weightKg?.let { MetricStat("Weight", "${"%.1f".format(it)} kg", KcalColor) }
                            latestEntry.bodyFatPct?.let { MetricStat("Body fat", "${"%.1f".format(it)}%", Tangerine) }
                            latestEntry.muscleMassKg?.let { MetricStat("Muscle", "${"%.1f".format(it)} kg", Basil) }
                            latestEntry.waterPct?.let { MetricStat("Water", "${"%.1f".format(it)}%", Blueberry) }
                        }
                    }
                }
            } else {
                EmptyState("No measurements yet. Tap + to log your first entry.")
            }

            Spacer(Modifier.height(20.dp))

            if (weightSeries.size >= 2) {
                MetricChart("Weight (kg)", weightSeries, KcalColor)
                Spacer(Modifier.height(16.dp))
            }
            if (fatSeries.size >= 2) {
                MetricChart("Body fat %", fatSeries, Tangerine)
                Spacer(Modifier.height(16.dp))
            }
            if (muscleSeries.size >= 2) {
                MetricChart("Muscle mass (kg)", muscleSeries, Basil)
                Spacer(Modifier.height(16.dp))
            }
            if (waterSeries.size >= 2) {
                MetricChart("Water %", waterSeries, Blueberry)
                Spacer(Modifier.height(16.dp))
            }

            // History log entries
            if (history.isNotEmpty()) {
                Text("Log", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                history.reversed().forEach { (date, entry) ->
                    Surface(color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(date.format(DateTimeFormatter.ofPattern("EEE d MMM yyyy")), fontWeight = FontWeight.SemiBold)
                                Spacer(Modifier.height(4.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    entry.weightKg?.let { Text("${"%.1f".format(it)} kg", style = MaterialTheme.typography.bodyMedium, color = KcalColor) }
                                    entry.bodyFatPct?.let { Text("${"%.1f".format(it)}% fat", style = MaterialTheme.typography.bodyMedium, color = Tangerine) }
                                    entry.muscleMassKg?.let { Text("${"%.1f".format(it)} kg muscle", style = MaterialTheme.typography.bodyMedium, color = Basil) }
                                    entry.waterPct?.let { Text("${"%.1f".format(it)}% water", style = MaterialTheme.typography.bodyMedium, color = Blueberry) }
                                }
                                if (entry.notes.isNotBlank()) {
                                    Spacer(Modifier.height(2.dp))
                                    Text(entry.notes, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                                }
                            }
                            ConfirmDeleteButton(onConfirm = { vm.deleteBodyMetric(date) })
                        }
                    }
                }
            }
            Spacer(Modifier.height(80.dp))
        }
    }

    if (showLogDialog) {
        LogMetricsDialog(
            today = today,
            existing = data[today.toString()],
            onDismiss = { showLogDialog = false },
            onSave = { entry -> vm.setBodyMetric(today, entry); showLogDialog = false }
        )
    }
}

@Composable
private fun MetricStat(label: String, value: String, color: Color) {
    Column {
        Text(value, fontWeight = FontWeight.Bold, color = color)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f))
    }
}

@Composable
private fun MetricChart(title: String, points: List<Pair<LocalDate, Double>>, color: Color) {
    val trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
    val minVal = points.minOf { it.second }
    val maxVal = points.maxOf { it.second }
    val range = (maxVal - minVal).coerceAtLeast(0.01)

    Column(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp)).padding(12.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(title, fontWeight = FontWeight.SemiBold)
            Text("${"%.1f".format(minVal)} – ${"%.1f".format(maxVal)}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
        }
        Spacer(Modifier.height(8.dp))
        Canvas(modifier = Modifier.fillMaxWidth().height(100.dp)) {
            val w = size.width
            val h = size.height
            val stepX = if (points.size > 1) w / (points.size - 1).toFloat() else w
            val offsets = points.mapIndexed { i, (_, v) ->
                Offset(i * stepX, h - ((v - minVal) / range * h).toFloat().coerceIn(4f, h - 4f))
            }
            // Shaded area under curve
            if (offsets.size >= 2) {
                val path = androidx.compose.ui.graphics.Path().apply {
                    moveTo(offsets.first().x, h)
                    offsets.forEach { lineTo(it.x, it.y) }
                    lineTo(offsets.last().x, h)
                    close()
                }
                drawPath(path, color = color.copy(alpha = 0.12f))
            }
            // Track line
            for (i in 0 until offsets.size - 1) drawLine(trackColor, offsets[i], offsets[i + 1], 1.dp.toPx())
            // Data line
            for (i in 0 until offsets.size - 1) drawLine(color, offsets[i], offsets[i + 1], 2.5.dp.toPx(), cap = StrokeCap.Round)
            // Dots
            offsets.forEach { drawCircle(color, 4.dp.toPx(), it) }
            // Latest value dot highlighted
            drawCircle(Color.White, 2.5.dp.toPx(), offsets.last())
            drawCircle(color, 4.dp.toPx(), offsets.last(), style = Stroke(1.5.dp.toPx()))
        }
    }
}

@Composable
private fun LogMetricsDialog(today: LocalDate, existing: BodyMetricEntry?, onDismiss: () -> Unit, onSave: (BodyMetricEntry) -> Unit) {
    var weight by remember { mutableStateOf(existing?.weightKg?.toString() ?: "") }
    var bodyFat by remember { mutableStateOf(existing?.bodyFatPct?.toString() ?: "") }
    var muscle by remember { mutableStateOf(existing?.muscleMassKg?.toString() ?: "") }
    var water by remember { mutableStateOf(existing?.waterPct?.toString() ?: "") }
    var notes by remember { mutableStateOf(existing?.notes ?: "") }

    Dialog(onDismiss) {
        Column(Modifier.background(MaterialTheme.colorScheme.surface, RoundedCornerShape(20.dp)).padding(20.dp).verticalScroll(rememberScrollState())) {
            Text("Log · ${today.format(DateTimeFormatter.ofPattern("d MMM yyyy"))}", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = weight, onValueChange = { weight = it }, label = { Text("Weight (kg)") }, modifier = Modifier.weight(1f))
                OutlinedTextField(value = bodyFat, onValueChange = { bodyFat = it }, label = { Text("Body fat %") }, modifier = Modifier.weight(1f))
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = muscle, onValueChange = { muscle = it }, label = { Text("Muscle (kg)") }, modifier = Modifier.weight(1f))
                OutlinedTextField(value = water, onValueChange = { water = it }, label = { Text("Water %") }, modifier = Modifier.weight(1f))
            }
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("Notes") }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onDismiss, Modifier.weight(1f)) { Text("Cancel") }
                Button(onClick = { onSave(BodyMetricEntry(weight.toDoubleOrNull(), bodyFat.toDoubleOrNull(), muscle.toDoubleOrNull(), water.toDoubleOrNull(), notes)) }, Modifier.weight(1f)) { Text("Save") }
            }
        }
    }
}
