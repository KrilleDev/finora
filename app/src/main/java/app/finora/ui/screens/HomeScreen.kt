package app.finora.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.finora.data.BuiltIns
import app.finora.ui.MainViewModel
import app.finora.ui.components.*
import app.finora.ui.formatMonth
import app.finora.ui.moneyFormatter
import app.finora.ui.parseColor
import app.finora.ui.shortWeekday
import app.finora.ui.theme.DangerRed
import app.finora.ui.theme.IncomeGreen
import app.finora.ui.theme.WarnOrange

@Composable
fun HomeScreen(
    vm: MainViewModel,
    onOpenExpense: (String) -> Unit,
    onSeeAll: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val a by vm.homeAnalytics.collectAsStateWithLifecycle()
    val expenses by vm.expenses.collectAsStateWithLifecycle()
    val categories by vm.categories.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()
    val money = moneyFormatter(settings.currency)
    val catMap = categories.associateBy { it.id }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 96.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Text("Finora", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            a?.let {
                Text(formatMonth(it.month), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        a?.let { an ->
            item {
                SectionCard(title = "Monatsübersicht") {
                    Text(money(an.spent), style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
                    Text("ausgegeben", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text("Einnahmen", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(money(an.income), color = IncomeGreen, fontWeight = FontWeight.SemiBold)
                        }
                        Column(horizontalAlignment = androidx.compose.ui.Alignment.End) {
                            Text("Saldo", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                money(an.balance),
                                fontWeight = FontWeight.SemiBold,
                                color = if (an.balance < 0) DangerRed else MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }
                    if (an.budget > 0) {
                        val p = an.budgetProgress
                        LinearProgressIndicator(
                            progress = { p.coerceIn(0f, 1f) },
                            modifier = Modifier.fillMaxWidth().height(10.dp),
                            color = when {
                                p >= 0.9f -> DangerRed
                                p >= 0.75f -> WarnOrange
                                else -> MaterialTheme.colorScheme.primary
                            },
                            strokeCap = androidx.compose.ui.graphics.StrokeCap.Round,
                        )
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("${(p * 100).toInt()} % des Budgets", style = MaterialTheme.typography.bodySmall)
                            Text("${an.daysRemaining} Tage übrig", style = MaterialTheme.typography.bodySmall)
                        }
                        Text(
                            "Verfügbar pro Tag: ${money(an.remainingPerDay)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        Text(
                            "Lege unter Analytics → Budget ein Monatsbudget fest.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            item {
                SectionCard(title = "Letzte 7 Tage") {
                    if (an.last7Days.all { it.amount == 0.0 }) {
                        EmptyHint("Keine Ausgaben in den letzten 7 Tagen")
                    } else {
                        BarChart(
                            an.last7Days.mapIndexed { i, d ->
                                BarPoint(shortWeekday(d.date), d.amount, highlight = i == an.last7Days.lastIndex)
                            }
                        )
                        Text(
                            "Ø ${money(an.dailyAverage)} pro Tag in diesem Monat",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            item {
                SectionCard(title = "Ausgaben nach Kategorie") {
                    if (an.byCategory.isEmpty()) {
                        EmptyHint("Noch keine Ausgaben in diesem Monat")
                    } else {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                            DonutChart(
                                slices = an.byCategory.map {
                                    val c = catMap[it.categoryId]
                                    Slice(c?.name ?: "Sonstiges", it.amount, parseColor(c?.colorHex ?: "#8E8E93"))
                                },
                                centerTop = "Gesamt",
                                centerBottom = money(an.spent),
                            )
                        }
                        an.byCategory.take(5).forEach {
                            val c = catMap[it.categoryId]
                            LegendRow(
                                parseColor(c?.colorHex ?: "#8E8E93"),
                                c?.name ?: "Sonstiges",
                                "${money(it.amount)} · ${(it.amount / an.spent * 100).toInt()} %",
                            )
                        }
                    }
                }
            }
        }

        item {
            SectionCard(
                title = "Letzte Buchungen",
                trailing = { TextButton(onClick = onSeeAll) { Text("Alle anzeigen") } },
            ) {
                if (expenses.isEmpty()) {
                    EmptyHint("Noch keine Buchungen – tippe auf +")
                } else {
                    expenses.take(5).forEach { e ->
                        TransactionRow(e, catMap[e.categoryId] ?: catMap[BuiltIns.OTHERS_ID], money) { onOpenExpense(e.id) }
                    }
                }
            }
        }
    }
}
