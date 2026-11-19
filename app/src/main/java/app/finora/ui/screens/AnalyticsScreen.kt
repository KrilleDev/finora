package app.finora.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.finora.data.AnalyticsTimeFilter
import app.finora.data.CategoryDistribution
import app.finora.data.DashboardAnalyticsResult
import app.finora.data.ExpenseEntity
import app.finora.data.localDate
import app.finora.ui.MainViewModel
import app.finora.ui.components.*
import app.finora.ui.formatDate
import app.finora.ui.moneyFormatter
import app.finora.ui.parseColor
import app.finora.ui.theme.IncomeGreen
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AnalyticsScreen(
    vm: MainViewModel,
    onOpenExpense: (String) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val analytics by vm.dashboardAnalytics.collectAsStateWithLifecycle()
    val activeFilter by vm.analyticsFilter.collectAsStateWithLifecycle()
    val categories by vm.categories.collectAsStateWithLifecycle()
    val allExpenses by vm.expenses.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()

    val money = remember(settings.currency) { moneyFormatter(settings.currency) }
    val catMap = remember(categories) { categories.associateBy { it.id } }

    var showCustomDateDialog by remember { mutableStateOf(false) }
    var selectedCategoryForSheet by remember { mutableStateOf<CategoryDistribution?>(null) }

    Column(modifier = modifier.fillMaxSize()) {
        // Top Bar Header
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(Modifier.padding(vertical = 12.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column {
                        Text(
                            "Analytics Dashboard",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        val rangeFmt = DateTimeFormatter.ofPattern("d. MMM yyyy", Locale.getDefault())
                        Text(
                            "${analytics.startDate.format(rangeFmt)} – ${analytics.endDate.format(rangeFmt)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    if (activeFilter == AnalyticsTimeFilter.CUSTOM) {
                        IconButton(onClick = { showCustomDateDialog = true }) {
                            Icon(Icons.Filled.DateRange, "Zeitraum wählen", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                // Time Filter Horizontal Chips
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(AnalyticsTimeFilter.entries.toTypedArray()) { filter ->
                        FilterChip(
                            selected = activeFilter == filter,
                            onClick = {
                                if (filter == AnalyticsTimeFilter.CUSTOM) {
                                    showCustomDateDialog = true
                                } else {
                                    vm.setAnalyticsFilter(filter)
                                }
                            },
                            label = { Text(filter.label) },
                            leadingIcon = if (filter == AnalyticsTimeFilter.CUSTOM) {
                                { Icon(Icons.Filled.CalendarMonth, null, modifier = Modifier.size(16.dp)) }
                            } else null,
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            ),
                        )
                    }
                }
            }
        }

        // Dashboard Content
        if (!analytics.hasData) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(24.dp),
                ) {
                    Icon(
                        Icons.Filled.Insights,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    )
                    Text(
                        "Keine Daten im gewählten Zeitraum",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        "Wähle einen anderen Zeitraum aus oder füge neue Buchungen hinzu.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    )
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp, 16.dp, 16.dp, 96.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                // Section 1: Financial Overview (Kompakte Finanzübersicht)
                item {
                    FinancialOverviewCard(analytics, money)
                }

                // Section 2: Diagrams Cards
                item {
                    SectionCard(
                        title = "Einnahmen & Ausgaben",
                        trailing = {
                            Text(
                                "Tippe auf Balken für Details",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        },
                    ) {
                        InteractiveIncomeExpenseBarChart(
                            periods = analytics.incomeExpenseComparison,
                            moneyFormatter = money,
                            chartHeight = 200.dp,
                        )
                    }
                }

                item {
                    SectionCard(
                        title = "Kontostandsentwicklung",
                        trailing = {
                            Text(
                                "Tippe auf Punkte für Details",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        },
                    ) {
                        InteractiveBalanceLineChart(
                            points = analytics.balanceTrend,
                            moneyFormatter = money,
                            chartHeight = 190.dp,
                        )
                    }
                }

                item {
                    SectionCard(
                        title = "Ausgabenverteilung",
                        trailing = {
                            Text(
                                "Kategorie antippen",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        },
                    ) {
                        InteractiveDonutChart(
                            categories = analytics.categoryDistribution,
                            moneyFormatter = money,
                            onCategoryClick = { catId ->
                                analytics.categoryDistribution.find { it.categoryId == catId }?.let {
                                    selectedCategoryForSheet = it
                                }
                            },
                        )
                    }
                }

                // Section 3: Performance & Key Metrics
                item {
                    KeyMetricsGrid(analytics, money)
                }

                // Section 4: Top 5 Largest Expenses
                if (analytics.top5Expenses.isNotEmpty()) {
                    item {
                        SectionCard(title = "Top 5 größte Ausgaben") {
                            analytics.top5Expenses.forEach { exp ->
                                val cat = catMap[exp.categoryId]
                                TransactionRow(
                                    expense = exp,
                                    category = cat,
                                    money = money,
                                    onClick = { onOpenExpense(exp.id) },
                                )
                            }
                        }
                    }
                }

                // Section 5: Monthly Overview Table
                if (analytics.monthlyOverview.isNotEmpty()) {
                    item {
                        SectionCard(title = "Monatsübersicht") {
                            analytics.monthlyOverview.forEach { period ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Column {
                                        Text(period.label, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Text("Einn: ${money(period.income)}", style = MaterialTheme.typography.bodySmall, color = IncomeGreen)
                                            Text("Ausg: ${money(period.expense)}", style = MaterialTheme.typography.bodySmall, color = ExpenseColor)
                                        }
                                    }
                                    Text(
                                        money(period.net),
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = if (period.net >= 0) IncomeGreen else MaterialTheme.colorScheme.error,
                                    )
                                }
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                            }
                        }
                    }
                }
            }
        }
    }

    // Custom Date Range Dialog
    if (showCustomDateDialog) {
        CustomDateRangeDialog(
            initialStart = analytics.startDate,
            initialEnd = analytics.endDate,
            onDismiss = { showCustomDateDialog = false },
            onConfirm = { start, end ->
                vm.setCustomDateRange(start, end)
                showCustomDateDialog = false
            },
        )
    }

    // Category Transactions Bottom Sheet
    selectedCategoryForSheet?.let { catDist ->
        val catTransactions = remember(allExpenses, analytics.startDate, analytics.endDate, catDist.categoryId) {
            allExpenses.filter {
                it.categoryId == catDist.categoryId &&
                    it.localDate() in analytics.startDate..analytics.endDate
            }.sortedByDescending { it.dateMillis }
        }

        ModalBottomSheet(
            onDismissRequest = { selectedCategoryForSheet = null },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
                    .padding(bottom = 24.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(bottom = 16.dp),
                ) {
                    CategoryBubble(catMap[catDist.categoryId], size = 44.dp)
                    Column(Modifier.weight(1f)) {
                        Text(catDist.categoryName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text(
                            "${money(catDist.amount)} (${catDist.percentage.toInt()} % aller Ausgaben)",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                Text(
                    "Transaktionen (${catTransactions.size})",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 8.dp),
                )

                if (catTransactions.isEmpty()) {
                    EmptyHint("Keine Buchungen in dieser Kategorie")
                } else {
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 350.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        items(catTransactions, key = { it.id }) { exp ->
                            TransactionRow(
                                expense = exp,
                                category = catMap[exp.categoryId],
                                money = money,
                                onClick = {
                                    selectedCategoryForSheet = null
                                    onOpenExpense(exp.id)
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FinancialOverviewCard(
    analytics: DashboardAnalyticsResult,
    money: (Double) -> String,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        shape = MaterialTheme.shapes.extraLarge,
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "Aktueller Nettosaldo",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                // Percent change pill badge
                val isPositiveChange = analytics.netBalanceChangePercent >= 0
                val badgeBg = if (isPositiveChange) IncomeGreen.copy(alpha = 0.15f) else MaterialTheme.colorScheme.errorContainer
                val badgeFg = if (isPositiveChange) IncomeGreen else MaterialTheme.colorScheme.error

                Surface(
                    shape = CircleShape,
                    color = badgeBg,
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = if (isPositiveChange) Icons.Filled.TrendingUp else Icons.Filled.TrendingDown,
                            contentDescription = null,
                            tint = badgeFg,
                            modifier = Modifier.size(14.dp),
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "${if (isPositiveChange) "+" else ""}${analytics.netBalanceChangePercent.toInt()} %",
                            style = MaterialTheme.typography.labelSmall,
                            color = badgeFg,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }

            Spacer(Modifier.height(4.dp))

            Text(
                money(analytics.netBalance),
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = if (analytics.netBalance >= 0) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.error,
            )

            Spacer(Modifier.height(16.dp))

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                // Income summary
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(8.dp).background(IncomeGreen, CircleShape))
                        Spacer(Modifier.width(6.dp))
                        Text("Gesamte Einnahmen", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text(
                        money(analytics.totalIncome),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = IncomeGreen,
                    )
                }

                // Expense summary
                Column(horizontalAlignment = Alignment.End) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(8.dp).background(ExpenseColor, CircleShape))
                        Spacer(Modifier.width(6.dp))
                        Text("Gesamte Ausgaben", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text(
                        money(analytics.totalExpense),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = ExpenseColor,
                    )
                }
            }
        }
    }
}

@Composable
private fun KeyMetricsGrid(
    analytics: DashboardAnalyticsResult,
    money: (Double) -> String,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            // Sparquote
            MetricCard(
                title = "Sparquote",
                value = "${analytics.savingsRate.toInt()} %",
                subtitle = "des Gesamteinkommens",
                modifier = Modifier.weight(1f),
                valueColor = if (analytics.savingsRate >= 20) IncomeGreen else MaterialTheme.colorScheme.onSurface,
            )

            // Ø Monatliche Ausgaben
            MetricCard(
                title = "Ø Monatsausgabe",
                value = money(analytics.monthlyAverageExpense),
                subtitle = "pro Monat im Zeitraum",
                modifier = Modifier.weight(1f),
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            // Höchste Einnahme
            MetricCard(
                title = "Höchste Einnahme",
                value = analytics.highestIncome?.let { money(it.amount) } ?: "—",
                subtitle = analytics.highestIncome?.title ?: "Keine Einnahme",
                modifier = Modifier.weight(1f),
                valueColor = IncomeGreen,
            )

            // Höchste Ausgabe
            MetricCard(
                title = "Höchste Ausgabe",
                value = analytics.highestExpense?.let { money(it.amount) } ?: "—",
                subtitle = analytics.highestExpense?.title ?: "Keine Ausgabe",
                modifier = Modifier.weight(1f),
                valueColor = ExpenseColor,
            )
        }
    }
}

@Composable
private fun MetricCard(
    title: String,
    value: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    valueColor: Color? = null,
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        shape = MaterialTheme.shapes.large,
    ) {
        Column(Modifier.padding(14.dp)) {
            Text(title, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(4.dp))
            Text(
                value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = valueColor ?: MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
            )
            Text(
                subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                maxLines = 1,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CustomDateRangeDialog(
    initialStart: LocalDate,
    initialEnd: LocalDate,
    onDismiss: () -> Unit,
    onConfirm: (start: LocalDate, end: LocalDate) -> Unit,
) {
    var startDate by remember { mutableStateOf(initialStart) }
    var endDate by remember { mutableStateOf(initialEnd) }
    var pickingStart by remember { mutableStateOf(true) }
    var showPicker by remember { mutableStateOf(false) }

    val dateFmt = DateTimeFormatter.ofPattern("dd.MM.yyyy", Locale.getDefault())

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Individuellen Zeitraum wählen") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Start- und Enddatum für die Auswertung festlegen:")

                OutlinedButton(
                    onClick = { pickingStart = true; showPicker = true },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("Startdatum:")
                        Text(startDate.format(dateFmt), fontWeight = FontWeight.Bold)
                    }
                }

                OutlinedButton(
                    onClick = { pickingStart = false; showPicker = true },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("Enddatum:")
                        Text(endDate.format(dateFmt), fontWeight = FontWeight.Bold)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = !startDate.isAfter(endDate),
                onClick = { onConfirm(startDate, endDate) }
            ) {
                Text("Übernehmen")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Abbrechen") }
        },
    )

    if (showPicker) {
        val initialMs = (if (pickingStart) startDate else endDate)
            .atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = initialMs)

        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { ms ->
                        val picked = Instant.ofEpochMilli(ms).atZone(ZoneOffset.UTC).toLocalDate()
                        if (pickingStart) startDate = picked else endDate = picked
                    }
                    showPicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showPicker = false }) { Text("Abbrechen") } },
        ) {
            DatePicker(state = pickerState)
        }
    }
}
