package com.paul.nutritiontracker.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.paul.nutritiontracker.ui.theme.Basil
import com.paul.nutritiontracker.ui.theme.Blueberry
import com.paul.nutritiontracker.ui.theme.Butter
import com.paul.nutritiontracker.ui.theme.Tangerine
import kotlinx.coroutines.delay

val KcalColor: Color get() = Tangerine
val ProteinColor: Color get() = Basil
val CarbsColor: Color get() = Blueberry
val FatColor: Color get() = Butter

// ── Vector icon drawing helpers (no emoji, no PNG dependency) ─────────────

/** Flexed arm / muscle — represents Protein */
private fun DrawScope.drawMuscleIcon(color: Color, iconSize: Dp) {
    val s = iconSize.toPx()
    val sw = s * 0.12f
    val path = Path().apply {
        moveTo(s * 0.15f, s * 0.72f)
        cubicTo(s * 0.05f, s * 0.50f, s * 0.10f, s * 0.28f, s * 0.32f, s * 0.22f)
        cubicTo(s * 0.38f, s * 0.10f, s * 0.58f, s * 0.08f, s * 0.65f, s * 0.22f)
        cubicTo(s * 0.80f, s * 0.30f, s * 0.88f, s * 0.48f, s * 0.82f, s * 0.70f)
        cubicTo(s * 0.78f, s * 0.82f, s * 0.62f, s * 0.86f, s * 0.50f, s * 0.80f)
        cubicTo(s * 0.36f, s * 0.74f, s * 0.22f, s * 0.80f, s * 0.15f, s * 0.72f)
        close()
    }
    drawPath(path, color = color)
    drawPath(path, color = color.copy(alpha = 0.4f), style = Stroke(width = sw, cap = StrokeCap.Round, join = StrokeJoin.Round))
}

/** Loaf of bread — represents Carbs */
private fun DrawScope.drawBreadIcon(color: Color, iconSize: Dp) {
    val s = iconSize.toPx()
    val body = Path().apply {
        moveTo(s * 0.10f, s * 0.85f)
        lineTo(s * 0.10f, s * 0.55f)
        cubicTo(s * 0.10f, s * 0.25f, s * 0.30f, s * 0.10f, s * 0.50f, s * 0.10f)
        cubicTo(s * 0.70f, s * 0.10f, s * 0.90f, s * 0.25f, s * 0.90f, s * 0.55f)
        lineTo(s * 0.90f, s * 0.85f)
        close()
    }
    drawPath(body, color = color)
    drawLine(color.copy(alpha = 0.5f), androidx.compose.ui.geometry.Offset(s * 0.10f, s * 0.85f), androidx.compose.ui.geometry.Offset(s * 0.90f, s * 0.85f), strokeWidth = s * 0.06f, cap = StrokeCap.Round)
    val slashColor = Color.White.copy(alpha = 0.55f)
    val sw = s * 0.06f
    drawLine(slashColor, androidx.compose.ui.geometry.Offset(s * 0.30f, s * 0.50f), androidx.compose.ui.geometry.Offset(s * 0.38f, s * 0.30f), sw, cap = StrokeCap.Round)
    drawLine(slashColor, androidx.compose.ui.geometry.Offset(s * 0.46f, s * 0.46f), androidx.compose.ui.geometry.Offset(s * 0.54f, s * 0.26f), sw, cap = StrokeCap.Round)
    drawLine(slashColor, androidx.compose.ui.geometry.Offset(s * 0.62f, s * 0.50f), androidx.compose.ui.geometry.Offset(s * 0.70f, s * 0.30f), sw, cap = StrokeCap.Round)
}

/** Oil drop — represents Fat */
private fun DrawScope.drawOilDropIcon(color: Color, iconSize: Dp) {
    val s = iconSize.toPx()
    val path = Path().apply {
        moveTo(s * 0.50f, s * 0.08f)
        cubicTo(s * 0.50f, s * 0.08f, s * 0.18f, s * 0.46f, s * 0.18f, s * 0.64f)
        cubicTo(s * 0.18f, s * 0.82f, s * 0.32f, s * 0.94f, s * 0.50f, s * 0.94f)
        cubicTo(s * 0.68f, s * 0.94f, s * 0.82f, s * 0.82f, s * 0.82f, s * 0.64f)
        cubicTo(s * 0.82f, s * 0.46f, s * 0.50f, s * 0.08f, s * 0.50f, s * 0.08f)
        close()
    }
    drawPath(path, color = color)
    drawLine(Color.White.copy(alpha = 0.6f), androidx.compose.ui.geometry.Offset(s * 0.36f, s * 0.58f), androidx.compose.ui.geometry.Offset(s * 0.40f, s * 0.72f), strokeWidth = s * 0.09f, cap = StrokeCap.Round)
}

@Composable
fun MacroIcon(type: String, color: Color, size: Dp = 18.dp) {
    Canvas(modifier = Modifier.size(size)) {
        when (type) {
            "protein" -> drawMuscleIcon(color, size)
            "carbs" -> drawBreadIcon(color, size)
            "fat" -> drawOilDropIcon(color, size)
            "kcal" -> drawFlameIcon(color, size)
        }
    }
}

/** Flame/fire — represents kcal energy */
private fun DrawScope.drawFlameIcon(color: Color, iconSize: Dp) {
    val s = iconSize.toPx()
    val path = Path().apply {
        moveTo(s * 0.50f, s * 0.92f)
        cubicTo(s * 0.50f, s * 0.78f, s * 0.38f, s * 0.78f, s * 0.38f, s * 0.58f)
        cubicTo(s * 0.38f, s * 0.30f, s * 0.62f, s * 0.30f, s * 0.62f, s * 0.58f)
        cubicTo(s * 0.62f, s * 0.78f, s * 0.50f, s * 0.78f, s * 0.50f, s * 0.92f)
        close()
        moveTo(s * 0.50f, s * 0.88f)
        cubicTo(s * 0.50f, s * 0.74f, s * 0.42f, s * 0.74f, s * 0.42f, s * 0.60f)
        cubicTo(s * 0.42f, s * 0.42f, s * 0.58f, s * 0.42f, s * 0.58f, s * 0.60f)
        cubicTo(s * 0.58f, s * 0.74f, s * 0.50f, s * 0.74f, s * 0.50f, s * 0.88f)
        close()
        moveTo(s * 0.50f, s * 0.20f)
        cubicTo(s * 0.30f, s * 0.42f, s * 0.30f, s * 0.70f, s * 0.50f, s * 0.92f)
        cubicTo(s * 0.70f, s * 0.70f, s * 0.70f, s * 0.42f, s * 0.50f, s * 0.20f)
        close()
    }
    drawPath(path, color = color)
}

@Composable
fun StatCard(label: String, value: String, modifier: Modifier = Modifier, accent: Color? = null) {
    Column(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(14.dp))
            .padding(vertical = 10.dp, horizontal = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = accent ?: MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
        Spacer(Modifier.height(2.dp))
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun EmptyState(text: String) {
    Box(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(14.dp))
            .padding(16.dp)
    ) {
        Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
    }
}

@Composable
fun ConfirmDeleteButton(onConfirm: () -> Unit) {
    var armed by remember { mutableStateOf(false) }
    LaunchedEffect(armed) { if (armed) { delay(3000); armed = false } }
    if (armed) {
        IconButton(onClick = { onConfirm(); armed = false }) {
            Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
        }
    } else {
        IconButton(onClick = { armed = true }) {
            Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
        }
    }
}

/** kcal / protein / carbs / fat, each with its own icon and color — every macro shown equally. */
@Composable
fun MacroChipRow(kcal: Double, protein: Double, carbs: Double, fat: Double) {
    val dim = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("${kcal.toInt()} kcal", style = MaterialTheme.typography.bodyMedium, color = KcalColor, fontWeight = FontWeight.Medium)
        Text("  ·  ", style = MaterialTheme.typography.bodyMedium, color = dim)
        MacroIcon("protein", ProteinColor, 14.dp)
        Spacer(Modifier.width(3.dp))
        Text("${"%.1f".format(protein)}g", style = MaterialTheme.typography.bodyMedium, color = ProteinColor, fontWeight = FontWeight.Medium)
        Text("  ·  ", style = MaterialTheme.typography.bodyMedium, color = dim)
        MacroIcon("carbs", CarbsColor, 14.dp)
        Spacer(Modifier.width(3.dp))
        Text("${"%.1f".format(carbs)}g", style = MaterialTheme.typography.bodyMedium, color = CarbsColor, fontWeight = FontWeight.Medium)
        Text("  ·  ", style = MaterialTheme.typography.bodyMedium, color = dim)
        MacroIcon("fat", FatColor, 14.dp)
        Spacer(Modifier.width(3.dp))
        Text("${"%.1f".format(fat)}g", style = MaterialTheme.typography.bodyMedium, color = FatColor, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun SettingsRow(title: String, subtitle: String, danger: Boolean = false, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Text(title, fontWeight = FontWeight.SemiBold, color = if (danger) MaterialTheme.colorScheme.error else Color.Unspecified)
            Spacer(Modifier.height(2.dp))
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
        }
    }
}

@Composable
fun SearchField(query: String, onQueryChange: (String) -> Unit, placeholder: String, modifier: Modifier = Modifier) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = { Text(placeholder) },
        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) { Icon(Icons.Filled.Close, contentDescription = "Clear") }
            }
        },
        singleLine = true,
        modifier = modifier.fillMaxWidth()
    )
}

@Composable
fun <T> SearchableDropdownField(
    label: String,
    options: List<T>,
    selectedName: String,
    displayName: (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier
) {
    var showPicker by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        OutlinedTextField(
            value = selectedName,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            trailingIcon = { Icon(Icons.Filled.Search, contentDescription = null) }
        )
        Box(
            Modifier
                .matchParentSize()
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { showPicker = true }
        )
    }

    if (showPicker) {
        var query by remember { mutableStateOf("") }
        val filtered = remember(query, options) {
            if (query.isBlank()) options else options.filter { displayName(it).contains(query, ignoreCase = true) }
        }
        Dialog(onDismissRequest = { showPicker = false }) {
            Column(
                Modifier
                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(20.dp))
                    .fillMaxHeight(0.8f)
                    .padding(16.dp)
            ) {
                Text(label, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(10.dp))
                SearchField(query, { query = it }, "Type to search...", Modifier.padding(bottom = 8.dp))
                if (filtered.isEmpty()) EmptyState("No matches.")
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(filtered) { item ->
                        Surface(
                            onClick = { onSelect(item); showPicker = false },
                            color = Color.Transparent,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(displayName(item), modifier = Modifier.padding(vertical = 14.dp, horizontal = 4.dp))
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(onClick = { showPicker = false }, modifier = Modifier.fillMaxWidth()) { Text("Close") }
            }
        }
    }
}
