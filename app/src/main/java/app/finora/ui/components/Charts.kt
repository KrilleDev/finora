package app.finora.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.finora.data.BalancePoint
import app.finora.data.CategoryDistribution
import app.finora.data.IncomeExpensePeriod
import app.finora.ui.parseColor
import app.finora.ui.theme.IncomeGreen
import kotlin.math.atan2
import kotlin.math.sqrt

val ExpenseColor = Color(0xFFB34A26) // Terracotta/brown matching Image 3

// --- Legacy Simple Chart Data Models & Composables (Used in Home Screen) ---

data class BarPoint(val label: String, val value: Double, val highlight: Boolean = false)
data class Slice(val label: String, val value: Double, val color: Color)

@Composable
fun BarChart(
    points: List<BarPoint>,
    modifier: Modifier = Modifier,
    barColor: Color = MaterialTheme.colorScheme.primary,
    chartHeight: Dp = 140.dp,
    valueFormatter: ((Double) -> String)? = null,
) {
    val max = (points.maxOfOrNull { it.value } ?: 0.0).coerceAtLeast(0.0001)
    val muted = MaterialTheme.colorScheme.surfaceVariant
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        points.forEach { p ->
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                if (valueFormatter != null && p.value > 0) {
                    Text(
                        valueFormatter(p.value),
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Canvas(Modifier.fillMaxWidth().height(chartHeight)) {
                    drawRoundRect(
                        color = muted,
                        size = size,
                        cornerRadius = CornerRadius(8.dp.toPx()),
                    )
                    val h = (size.height * (p.value / max)).toFloat()
                    if (h > 0f) {
                        drawRoundRect(
                            color = if (p.highlight) barColor else barColor.copy(alpha = 0.55f),
                            topLeft = Offset(0f, size.height - h),
                            size = Size(size.width, h),
                            cornerRadius = CornerRadius(8.dp.toPx()),
                        )
                    }
                }
                Text(
                    p.label,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(top = 4.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
fun DonutChart(
    slices: List<Slice>,
    modifier: Modifier = Modifier,
    centerTop: String = "",
    centerBottom: String = "",
    diameter: Dp = 180.dp,
) {
    val total = slices.sumOf { it.value }
    val track = MaterialTheme.colorScheme.surfaceVariant
    Box(modifier = modifier.size(diameter), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(diameter)) {
            val stroke = 26.dp.toPx()
            val inset = stroke / 2
            val arcSize = Size(size.width - stroke, size.height - stroke)
            drawArc(
                color = track, startAngle = 0f, sweepAngle = 360f, useCenter = false,
                topLeft = Offset(inset, inset), size = arcSize, style = Stroke(stroke),
            )
            if (total > 0) {
                var start = -90f
                slices.forEach { s ->
                    val sweep = (s.value / total * 360).toFloat()
                    drawArc(
                        color = s.color, startAngle = start, sweepAngle = (sweep - 1.5f).coerceAtLeast(0.5f),
                        useCenter = false, topLeft = Offset(inset, inset), size = arcSize,
                        style = Stroke(stroke, cap = StrokeCap.Butt),
                    )
                    start += sweep
                }
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (centerTop.isNotEmpty()) Text(centerTop, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (centerBottom.isNotEmpty()) Text(centerBottom, style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
fun LegendRow(color: Color, label: String, trailing: String, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(10.dp).background(color, CircleShape))
        Text(label, Modifier.padding(start = 10.dp).weight(1f), style = MaterialTheme.typography.bodyMedium)
        Text(trailing, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

// --- Interactive Dashboard Charts ---

@Composable
fun InteractiveIncomeExpenseBarChart(
    periods: List<IncomeExpensePeriod>,
    moneyFormatter: (Double) -> String,
    modifier: Modifier = Modifier,
    chartHeight: Dp = 220.dp,
) {
    if (periods.isEmpty()) return

    var selectedIndex by remember(periods) { mutableIntStateOf(-1) }
    val maxVal = maxOf(
        100.0,
        periods.maxOfOrNull { maxOf(it.income, it.expense) } ?: 100.0
    ) * 1.15

    val gridSteps = 4
    val gridValues = (0..gridSteps).map { i -> (maxVal / gridSteps) * i }

    Column(modifier = modifier.fillMaxWidth()) {
        // Tooltip for selected bar
        AnimatedVisibility(
            visible = selectedIndex in periods.indices,
            enter = fadeIn(tween(150)),
            exit = fadeOut(tween(150)),
        ) {
            val selected = periods.getOrNull(selectedIndex)
            if (selected != null) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column {
                            Text(selected.label, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Text(
                                "Netto: ${moneyFormatter(selected.net)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (selected.net >= 0) IncomeGreen else MaterialTheme.colorScheme.error,
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Einnahmen", style = MaterialTheme.typography.labelSmall, color = IncomeGreen)
                                Text(moneyFormatter(selected.income), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Ausgaben", style = MaterialTheme.typography.labelSmall, color = ExpenseColor)
                                Text(moneyFormatter(selected.expense), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
        }

        Row(modifier = Modifier.fillMaxWidth().height(chartHeight)) {
            // Y-Axis Grid labels
            Column(
                modifier = Modifier.height(chartHeight).padding(end = 8.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.End,
            ) {
                gridValues.reversed().forEach { v ->
                    Text(
                        formatAxisValue(v),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        fontSize = 10.sp,
                    )
                }
            }

            // Canvas Chart
            Box(modifier = Modifier.weight(1f).height(chartHeight)) {
                val gridColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                val selectionBgColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                val pathEffect = remember { PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f) }

                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(chartHeight)
                        .pointerInput(periods) {
                            detectTapGestures { offset ->
                                val widthPerGroup = size.width / periods.size
                                val index = (offset.x / widthPerGroup).toInt().coerceIn(0, periods.lastIndex)
                                selectedIndex = if (selectedIndex == index) -1 else index
                            }
                        }
                ) {
                    val w = size.width
                    val h = size.height
                    val numGroups = periods.size
                    val groupWidth = w / numGroups

                    // Grid lines
                    for (i in 0..gridSteps) {
                        val y = h - (h * (i.toDouble() / gridSteps)).toFloat()
                        drawLine(
                            color = gridColor,
                            start = Offset(0f, y),
                            end = Offset(w, y),
                            strokeWidth = 1.dp.toPx(),
                            pathEffect = pathEffect,
                        )
                    }

                    // Dual Bars for each period
                    periods.forEachIndexed { i, p ->
                        val centerX = (i + 0.5f) * groupWidth
                        val barWidth = (groupWidth * 0.35f).coerceIn(8.dp.toPx(), 24.dp.toPx())
                        val gap = 4.dp.toPx()

                        val incHeight = (h * (p.income / maxVal)).toFloat()
                        val expHeight = (h * (p.expense / maxVal)).toFloat()

                        val isSelected = i == selectedIndex
                        val incColor = if (isSelected) IncomeGreen else IncomeGreen.copy(alpha = 0.85f)
                        val expColor = if (isSelected) ExpenseColor else ExpenseColor.copy(alpha = 0.85f)

                        // Income bar (left)
                        val incLeft = centerX - barWidth - gap / 2
                        if (incHeight > 0f) {
                            drawRoundRect(
                                color = incColor,
                                topLeft = Offset(incLeft, h - incHeight),
                                size = Size(barWidth, incHeight),
                                cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx()),
                            )
                        }

                        // Expense bar (right)
                        val expLeft = centerX + gap / 2
                        if (expHeight > 0f) {
                            drawRoundRect(
                                color = expColor,
                                topLeft = Offset(expLeft, h - expHeight),
                                size = Size(barWidth, expHeight),
                                cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx()),
                            )
                        }

                        // Selection indicator outline
                        if (isSelected) {
                            drawRoundRect(
                                color = selectionBgColor,
                                topLeft = Offset(centerX - groupWidth / 2, 0f),
                                size = Size(groupWidth, h),
                                cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx()),
                            )
                        }
                    }
                }
            }
        }

        // X-Axis Labels
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 44.dp, top = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            periods.forEachIndexed { i, p ->
                Text(
                    p.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (i == selectedIndex) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = if (i == selectedIndex) FontWeight.Bold else FontWeight.Normal,
                    fontSize = 11.sp,
                )
            }
        }

        // Legend
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(10.dp).background(IncomeGreen, CircleShape))
            Spacer(Modifier.width(6.dp))
            Text("Einnahmen", style = MaterialTheme.typography.bodySmall)

            Spacer(Modifier.width(24.dp))

            Box(Modifier.size(10.dp).background(ExpenseColor, CircleShape))
            Spacer(Modifier.width(6.dp))
            Text("Ausgaben", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
fun InteractiveBalanceLineChart(
    points: List<BalancePoint>,
    moneyFormatter: (Double) -> String,
    modifier: Modifier = Modifier,
    chartHeight: Dp = 200.dp,
) {
    if (points.isEmpty()) return

    var selectedIndex by remember(points) { mutableIntStateOf(-1) }
    val minVal = minOf(0.0, points.minOfOrNull { it.balance } ?: 0.0)
    val maxVal = maxOf(100.0, points.maxOfOrNull { it.balance } ?: 100.0)
    val range = maxOf(1.0, maxVal - minVal) * 1.15

    val gridSteps = 4
    val gridValues = (0..gridSteps).map { i -> minVal + (range / gridSteps) * i }

    Column(modifier = modifier.fillMaxWidth()) {
        // Tooltip for selected point
        AnimatedVisibility(
            visible = selectedIndex in points.indices,
            enter = fadeIn(tween(150)),
            exit = fadeOut(tween(150)),
        ) {
            val selected = points.getOrNull(selectedIndex)
            if (selected != null) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(selected.label, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Kontostand", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                moneyFormatter(selected.balance),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (selected.balance >= 0) IncomeGreen else MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                }
            }
        }

        Row(modifier = Modifier.fillMaxWidth().height(chartHeight)) {
            // Y-Axis Grid labels
            Column(
                modifier = Modifier.height(chartHeight).padding(end = 8.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.End,
            ) {
                gridValues.reversed().forEach { v ->
                    Text(
                        formatAxisValue(v),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        fontSize = 10.sp,
                    )
                }
            }

            // Canvas Line Chart
            Box(modifier = Modifier.weight(1f).height(chartHeight)) {
                val gridColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                val linePathColor = IncomeGreen
                val selectedPointBgColor = MaterialTheme.colorScheme.surface
                val pathEffect = remember { PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f) }

                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(chartHeight)
                        .pointerInput(points) {
                            detectTapGestures { offset ->
                                val widthPerPoint = size.width / maxOf(1, points.size - 1)
                                val index = ((offset.x + widthPerPoint / 2) / widthPerPoint).toInt().coerceIn(0, points.lastIndex)
                                selectedIndex = if (selectedIndex == index) -1 else index
                            }
                        }
                ) {
                    val w = size.width
                    val h = size.height

                    // Grid lines
                    for (i in 0..gridSteps) {
                        val y = h - (h * (i.toDouble() / gridSteps)).toFloat()
                        drawLine(
                            color = gridColor,
                            start = Offset(0f, y),
                            end = Offset(w, y),
                            strokeWidth = 1.dp.toPx(),
                            pathEffect = pathEffect,
                        )
                    }

                    // Compute Point Coordinates
                    val coords = points.mapIndexed { i, p ->
                        val x = if (points.size == 1) w / 2 else i * (w / (points.size - 1))
                        val norm = ((p.balance - minVal) / range).toFloat()
                        val y = h - (h * norm)
                        Offset(x, y)
                    }

                    // Draw Line Path
                    if (coords.size >= 2) {
                        val path = Path().apply {
                            moveTo(coords.first().x, coords.first().y)
                            for (i in 1 until coords.size) {
                                val p0 = coords[i - 1]
                                val p1 = coords[i]
                                val controlX1 = p0.x + (p1.x - p0.x) / 2
                                val controlY1 = p0.y
                                val controlX2 = p0.x + (p1.x - p0.x) / 2
                                val controlY2 = p1.y
                                cubicTo(controlX1, controlY1, controlX2, controlY2, p1.x, p1.y)
                            }
                        }

                        drawPath(
                            path = path,
                            color = linePathColor,
                            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round),
                        )
                    }

                    // Draw Points
                    coords.forEachIndexed { i, pt ->
                        val isSelected = i == selectedIndex
                        drawCircle(
                            color = if (isSelected) selectedPointBgColor else linePathColor,
                            radius = if (isSelected) 7.dp.toPx() else 4.5f.dp.toPx(),
                            center = pt,
                        )
                        drawCircle(
                            color = linePathColor,
                            radius = if (isSelected) 5.dp.toPx() else 3.dp.toPx(),
                            center = pt,
                        )
                    }
                }
            }
        }

        // X-Axis Labels
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 44.dp, top = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            points.forEachIndexed { i, p ->
                Text(
                    p.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (i == selectedIndex) IncomeGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = if (i == selectedIndex) FontWeight.Bold else FontWeight.Normal,
                    fontSize = 11.sp,
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun InteractiveDonutChart(
    categories: List<CategoryDistribution>,
    moneyFormatter: (Double) -> String,
    onCategoryClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    diameter: Dp = 200.dp,
) {
    if (categories.isEmpty()) return

    var selectedIndex by remember(categories) { mutableIntStateOf(-1) }
    val totalExpense = categories.sumOf { it.amount }

    val animProgress by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(600),
        label = "donutAnim"
    )

    Column(modifier = modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(modifier = Modifier.size(diameter), contentAlignment = Alignment.Center) {
            Canvas(
                modifier = Modifier
                    .size(diameter)
                    .pointerInput(categories) {
                        detectTapGestures { offset ->
                            val radius = size.width / 2f
                            val center = Offset(radius, radius)
                            val dx = offset.x - center.x
                            val dy = offset.y - center.y
                            val dist = sqrt(dx * dx + dy * dy)
                            val strokePx = 28.dp.toPx()

                            if (dist in (radius - strokePx..radius + strokePx / 2)) {
                                var angle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
                                if (angle < 0) angle += 360f
                                val normAngle = (angle + 90f) % 360f

                                var accum = 0f
                                var hitIndex = -1
                                categories.forEachIndexed { idx, cat ->
                                    val sweep = (cat.amount / totalExpense * 360).toFloat()
                                    if (normAngle in accum..(accum + sweep)) {
                                        hitIndex = idx
                                    }
                                    accum += sweep
                                }
                                selectedIndex = if (selectedIndex == hitIndex) -1 else hitIndex
                            }
                        }
                    }
            ) {
                val strokeWidth = 28.dp.toPx()
                val inset = strokeWidth / 2
                val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)

                // Outer track
                drawArc(
                    color = Color.Gray.copy(alpha = 0.15f),
                    startAngle = 0f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = Offset(inset, inset),
                    size = arcSize,
                    style = Stroke(strokeWidth),
                )

                if (totalExpense > 0) {
                    var startAngle = -90f
                    categories.forEachIndexed { i, cat ->
                        val sweep = ((cat.amount / totalExpense * 360) * animProgress).toFloat()
                        val isSelected = i == selectedIndex
                        val color = parseColor(cat.colorHex)
                        val stroke = if (isSelected) strokeWidth * 1.25f else strokeWidth

                        drawArc(
                            color = color,
                            startAngle = startAngle,
                            sweepAngle = (sweep - 2f).coerceAtLeast(0.5f),
                            useCenter = false,
                            topLeft = Offset(inset, inset),
                            size = arcSize,
                            style = Stroke(stroke, cap = StrokeCap.Butt),
                        )
                        startAngle += sweep
                    }
                }
            }

            // Center Content
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                val selected = categories.getOrNull(selectedIndex)
                if (selected != null) {
                    Text(selected.categoryName, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(moneyFormatter(selected.amount), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("${selected.percentage.toInt()} %", style = MaterialTheme.typography.labelSmall, color = parseColor(selected.colorHex))
                } else {
                    Text("Gesamtausgaben", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(moneyFormatter(totalExpense), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // Categories Legend Grid
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            categories.forEachIndexed { i, cat ->
                val isSelected = i == selectedIndex
                val color = parseColor(cat.colorHex)
                Surface(
                    onClick = {
                        if (selectedIndex == i) onCategoryClick(cat.categoryId) else selectedIndex = i
                    },
                    shape = CircleShape,
                    color = if (isSelected) color.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceContainerHigh,
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(Modifier.size(10.dp).background(color, CircleShape))
                        Spacer(Modifier.width(6.dp))
                        Text(cat.categoryName, style = MaterialTheme.typography.labelMedium)
                        Spacer(Modifier.width(6.dp))
                        Text("${cat.percentage.toInt()} %", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

private fun formatAxisValue(v: Double): String = when {
    v >= 1000 -> "${(v / 1000).toInt()}k €"
    else -> "${v.toInt()} €"
}
