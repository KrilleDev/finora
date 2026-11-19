package app.finora.data

import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.time.temporal.WeekFields
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max

fun ExpenseEntity.localDate(): LocalDate =
    Instant.ofEpochMilli(dateMillis).atZone(ZoneId.systemDefault()).toLocalDate()

fun ExpenseEntity.yearMonth(): YearMonth = YearMonth.from(localDate())

fun LocalDate.toMillis(): Long = atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

// --- Legacy Month Analytics for Home Screen & Simple View ---

data class DaySpending(val date: LocalDate, val amount: Double)
data class MonthSpending(val month: YearMonth, val amount: Double)
data class CategoryTotal(val categoryId: String, val amount: Double)

data class CategoryTrend(val categoryId: String, val current: Double, val previous: Double) {
    val percentChange: Double
        get() = if (previous > 0) (current - previous) / previous * 100 else 0.0
}

enum class InsightKind { POSITIVE, NEUTRAL, NEGATIVE }
data class Insight(val kind: InsightKind, val title: String, val text: String)

data class MonthAnalytics(
    val month: YearMonth,
    val spent: Double,
    val income: Double,
    val byCategory: List<CategoryTotal>,
    val budget: Double,
    val daysRemaining: Int,
    val remainingPerDay: Double,
    val dailyAverage: Double,
    val projected: Double,
    val last7Days: List<DaySpending>,
    val monthlyTrend: List<MonthSpending>,
    val categoryTrends: List<CategoryTrend>,
    val insights: List<Insight>,
) {
    val balance: Double get() = income - spent
    val budgetProgress: Float get() = if (budget > 0) (spent / budget).toFloat() else 0f
}

private fun List<ExpenseEntity>.expensesOnly() = filter { !it.isIncome }

private fun List<ExpenseEntity>.totalsByCategory(): Map<String, Double> =
    expensesOnly().groupBy { it.categoryId }.mapValues { (_, v) -> v.sumOf { it.amount } }

fun computeAnalytics(
    all: List<ExpenseEntity>,
    categoryNames: Map<String, String>,
    budgets: Map<String, Double>,
    month: YearMonth,
    today: LocalDate,
    money: (Double) -> String,
): MonthAnalytics {
    val inMonth = all.filter { it.yearMonth() == month }
    val spent = inMonth.expensesOnly().sumOf { it.amount }
    val income = inMonth.filter { it.isIncome }.sumOf { it.amount }
    val byCategory = inMonth.totalsByCategory()
        .map { CategoryTotal(it.key, it.value) }
        .sortedByDescending { it.amount }

    val budget = budgets[month.toString()] ?: 0.0
    val length = month.lengthOfMonth()
    val start = month.atDay(1)
    val end = month.atEndOfMonth()

    val daysRemaining = when {
        end < today -> 0
        start > today -> length
        else -> ChronoUnit.DAYS.between(today, end).toInt()
    }
    val remainingPerDay = when {
        daysRemaining == 0 -> 0.0
        start > today -> budget / length
        else -> max(0.0, budget - spent) / daysRemaining
    }

    val elapsedDays = when {
        start > today -> 0
        end < today -> length
        else -> today.dayOfMonth
    }
    val dailyAverage = if (elapsedDays > 0) spent / elapsedDays else 0.0
    val projected = if (elapsedDays > 0) dailyAverage * length else 0.0

    val refDay = if (month == YearMonth.from(today)) today else end
    val last7 = (6 downTo 0).map { i ->
        val d = refDay.minusDays(i.toLong())
        DaySpending(d, all.expensesOnly().filter { it.localDate() == d }.sumOf { it.amount })
    }

    val trend = (5 downTo 0).map { i ->
        val ym = month.minusMonths(i.toLong())
        MonthSpending(ym, all.expensesOnly().filter { it.yearMonth() == ym }.sumOf { it.amount })
    }

    val previousTotals = all.filter { it.yearMonth() == month.minusMonths(1) }.totalsByCategory()
    val currentTotals = inMonth.totalsByCategory()
    val categoryTrends = (previousTotals.keys + currentTotals.keys).map {
        CategoryTrend(it, currentTotals[it] ?: 0.0, previousTotals[it] ?: 0.0)
    }

    val insights = buildInsights(budget, spent, daysRemaining, projected, categoryTrends, categoryNames, money)

    return MonthAnalytics(
        month, spent, income, byCategory, budget, daysRemaining, remainingPerDay,
        dailyAverage, projected, last7, trend, categoryTrends, insights,
    )
}

private fun buildInsights(
    budget: Double,
    spent: Double,
    daysRemaining: Int,
    projected: Double,
    trends: List<CategoryTrend>,
    names: Map<String, String>,
    money: (Double) -> String,
): List<Insight> {
    val out = mutableListOf<Insight>()
    if (budget > 0) {
        val used = spent / budget * 100
        when {
            used >= 90 -> out += Insight(InsightKind.NEGATIVE, "Budget-Warnung", "Du hast ${used.toInt()} % deines Monatsbudgets verbraucht.")
            used >= 75 -> out += Insight(InsightKind.NEUTRAL, "Budget-Hinweis", "Du hast ${used.toInt()} % deines Monatsbudgets verbraucht.")
            daysRemaining < 7 && used < 60 -> out += Insight(InsightKind.POSITIVE, "Unter Budget", "Stark! Du liegst diesen Monat deutlich unter deinem Budget.")
        }
    }

    trends.filter { it.previous > 0 && it.percentChange > 30 }
        .maxByOrNull { it.percentChange }
        ?.let {
            out += Insight(
                InsightKind.NEGATIVE, "Ausgaben gestiegen",
                "${names[it.categoryId] ?: "Sonstiges"}: ${it.percentChange.toInt()} % mehr als im Vormonat.",
            )
        }

    trends.filter { it.previous > 0 && it.current < it.previous && abs(it.percentChange) > 20 }
        .minByOrNull { it.percentChange }
        ?.let {
            out += Insight(
                InsightKind.POSITIVE, "Ausgaben gesenkt",
                "${names[it.categoryId] ?: "Sonstiges"}: ${abs(it.percentChange).toInt()} % weniger als im Vormonat.",
            )
        }

    if (budget > 0 && projected > budget) {
        out += Insight(
            InsightKind.NEGATIVE, "Prognose: Überschreitung",
            "Bei diesem Tempo überschreitest du dein Budget um ${money(projected - budget)}.",
        )
    }
    return out
}

// --- New Dashboard Analytics ---

enum class AnalyticsTimeFilter(val label: String) {
    THIS_WEEK("Diese Woche"),
    THIS_MONTH("Dieser Monat"),
    LAST_3_MONTHS("Letzte 3 Monate"),
    THIS_YEAR("Dieses Jahr"),
    ALL_TIME("Gesamter Zeitraum"),
    CUSTOM("Individuell"),
}

data class IncomeExpensePeriod(
    val label: String,
    val income: Double,
    val expense: Double,
    val startDate: LocalDate,
    val endDate: LocalDate,
) {
    val net: Double get() = income - expense
}

data class BalancePoint(
    val label: String,
    val balance: Double,
    val date: LocalDate,
)

data class CategoryDistribution(
    val categoryId: String,
    val categoryName: String,
    val colorHex: String,
    val iconKey: String,
    val amount: Double,
    val percentage: Double,
    val transactionCount: Int,
)

data class DashboardAnalyticsResult(
    val filter: AnalyticsTimeFilter,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val netBalance: Double,
    val totalIncome: Double,
    val totalExpense: Double,
    val netBalanceChangePercent: Double,
    val incomeChangePercent: Double,
    val expenseChangePercent: Double,
    val savingsRate: Double,
    val monthlyAverageExpense: Double,
    val highestIncome: ExpenseEntity?,
    val highestExpense: ExpenseEntity?,
    val top5Expenses: List<ExpenseEntity>,
    val incomeExpenseComparison: List<IncomeExpensePeriod>,
    val balanceTrend: List<BalancePoint>,
    val categoryDistribution: List<CategoryDistribution>,
    val monthlyOverview: List<IncomeExpensePeriod>,
    val prevTotalIncome: Double,
    val prevTotalExpense: Double,
    val prevNetBalance: Double,
    val hasData: Boolean,
)

fun calculateTimeRange(
    filter: AnalyticsTimeFilter,
    customStart: LocalDate?,
    customEnd: LocalDate?,
    today: LocalDate = LocalDate.now(),
    allExpenses: List<ExpenseEntity> = emptyList(),
): Pair<ClosedRange<LocalDate>, ClosedRange<LocalDate>> {
    val currentRange: ClosedRange<LocalDate>
    val prevRange: ClosedRange<LocalDate>

    when (filter) {
        AnalyticsTimeFilter.THIS_WEEK -> {
            val start = today.with(WeekFields.of(Locale.getDefault()).firstDayOfWeek)
            val end = start.plusDays(6)
            currentRange = start..end
            prevRange = start.minusWeeks(1)..end.minusWeeks(1)
        }
        AnalyticsTimeFilter.THIS_MONTH -> {
            val start = today.withDayOfMonth(1)
            val end = today.withDayOfMonth(today.lengthOfMonth())
            currentRange = start..end
            val pStart = start.minusMonths(1)
            val pEnd = pStart.withDayOfMonth(pStart.lengthOfMonth())
            prevRange = pStart..pEnd
        }
        AnalyticsTimeFilter.LAST_3_MONTHS -> {
            val start = today.minusMonths(3).plusDays(1)
            val end = today
            currentRange = start..end
            val days = ChronoUnit.DAYS.between(start, end) + 1
            prevRange = start.minusDays(days)..start.minusDays(1)
        }
        AnalyticsTimeFilter.THIS_YEAR -> {
            val start = LocalDate.of(today.year, 1, 1)
            val end = LocalDate.of(today.year, 12, 31)
            currentRange = start..end
            prevRange = LocalDate.of(today.year - 1, 1, 1)..LocalDate.of(today.year - 1, 12, 31)
        }
        AnalyticsTimeFilter.ALL_TIME -> {
            val earliest = allExpenses.minOfOrNull { it.localDate() } ?: today.minusMonths(6)
            val start = if (earliest.isAfter(today)) today.minusMonths(6) else earliest
            val end = today
            currentRange = start..end
            val days = max(30L, ChronoUnit.DAYS.between(start, end) + 1)
            prevRange = start.minusDays(days)..start.minusDays(1)
        }
        AnalyticsTimeFilter.CUSTOM -> {
            val start = customStart ?: today.minusMonths(1)
            val end = customEnd ?: today
            val validStart = if (start.isAfter(end)) end else start
            currentRange = validStart..end
            val days = max(1L, ChronoUnit.DAYS.between(validStart, end) + 1)
            prevRange = validStart.minusDays(days)..validStart.minusDays(1)
        }
    }
    return Pair(currentRange, prevRange)
}

fun computeDashboardAnalytics(
    all: List<ExpenseEntity>,
    categories: List<CategoryEntity>,
    filter: AnalyticsTimeFilter,
    customStart: LocalDate?,
    customEnd: LocalDate?,
    today: LocalDate = LocalDate.now(),
): DashboardAnalyticsResult {
    val categoryMap = categories.associateBy { it.id }
    val (currentRange, prevRange) = calculateTimeRange(filter, customStart, customEnd, today, all)
    val startDate = currentRange.start
    val endDate = currentRange.endInclusive

    val currentTransactions = all.filter { it.localDate() in currentRange }
    val prevTransactions = all.filter { it.localDate() in prevRange }

    val totalIncome = currentTransactions.filter { it.isIncome }.sumOf { it.amount }
    val totalExpense = currentTransactions.filter { !it.isIncome }.sumOf { it.amount }
    val netBalance = totalIncome - totalExpense

    val prevTotalIncome = prevTransactions.filter { it.isIncome }.sumOf { it.amount }
    val prevTotalExpense = prevTransactions.filter { !it.isIncome }.sumOf { it.amount }
    val prevNetBalance = prevTotalIncome - prevTotalExpense

    val netBalanceChangePercent = if (prevNetBalance != 0.0) {
        ((netBalance - prevNetBalance) / abs(prevNetBalance)) * 100
    } else if (netBalance > 0) 100.0 else 0.0

    val incomeChangePercent = if (prevTotalIncome > 0) {
        ((totalIncome - prevTotalIncome) / prevTotalIncome) * 100
    } else if (totalIncome > 0) 100.0 else 0.0

    val expenseChangePercent = if (prevTotalExpense > 0) {
        ((totalExpense - prevTotalExpense) / prevTotalExpense) * 100
    } else if (totalExpense > 0) 100.0 else 0.0

    val savingsRate = if (totalIncome > 0) {
        max(0.0, (totalIncome - totalExpense) / totalIncome * 100)
    } else 0.0

    val daysInPeriod = max(1L, ChronoUnit.DAYS.between(startDate, endDate) + 1)
    val monthsInPeriod = max(1.0, daysInPeriod / 30.4375)
    val monthlyAverageExpense = totalExpense / monthsInPeriod

    val highestIncome = currentTransactions.filter { it.isIncome }.maxByOrNull { it.amount }
    val highestExpense = currentTransactions.filter { !it.isIncome }.maxByOrNull { it.amount }
    val top5Expenses = currentTransactions.filter { !it.isIncome }.sortedByDescending { it.amount }.take(5)

    // Category Distribution
    val expensesByCategory = currentTransactions.filter { !it.isIncome }.groupBy { it.categoryId }
    val categoryDistributions = expensesByCategory.map { (catId, list) ->
        val amount = list.sumOf { it.amount }
        val cat = categoryMap[catId]
        val name = cat?.name ?: if (catId == BuiltIns.OTHERS_ID) "Sonstiges" else "Unbekannt"
        val color = cat?.colorHex ?: "#8E8E93"
        val icon = cat?.iconKey ?: "more"
        val percentage = if (totalExpense > 0) (amount / totalExpense) * 100 else 0.0
        CategoryDistribution(
            categoryId = catId,
            categoryName = name,
            colorHex = color,
            iconKey = icon,
            amount = amount,
            percentage = percentage,
            transactionCount = list.size,
        )
    }.sortedByDescending { it.amount }

    // Grouping strategy for charts
    val periods = generatePeriods(startDate, endDate)
    val incomeExpenseComparison = periods.map { p ->
        val txs = currentTransactions.filter { it.localDate() in p.first..p.second }
        val inc = txs.filter { it.isIncome }.sumOf { it.amount }
        val exp = txs.filter { !it.isIncome }.sumOf { it.amount }
        IncomeExpensePeriod(p.third, inc, exp, p.first, p.second)
    }

    // Cumulative Balance Trend Line
    val priorNetBalance = all.filter { it.localDate().isBefore(startDate) }
        .sumOf { if (it.isIncome) it.amount else -it.amount }

    var runningBalance = priorNetBalance
    val balancePoints = mutableListOf<BalancePoint>()
    periods.forEach { p ->
        val txs = currentTransactions.filter { it.localDate() in p.first..p.second }
        val pNet = txs.sumOf { if (it.isIncome) it.amount else -it.amount }
        runningBalance += pNet
        balancePoints.add(BalancePoint(p.third, runningBalance, p.second))
    }

    // Monthly Overview
    val startMonth = YearMonth.from(startDate)
    val endMonth = YearMonth.from(endDate)
    val monthlyOverview = mutableListOf<IncomeExpensePeriod>()
    var currM = startMonth
    while (!currM.isAfter(endMonth)) {
        val mStart = currM.atDay(1)
        val mEnd = currM.atEndOfMonth()
        val mTxs = all.filter { it.yearMonth() == currM }
        val inc = mTxs.filter { it.isIncome }.sumOf { it.amount }
        val exp = mTxs.filter { !it.isIncome }.sumOf { it.amount }
        val label = currM.format(DateTimeFormatter.ofPattern("MMM yyyy", Locale.getDefault()))
        monthlyOverview.add(IncomeExpensePeriod(label, inc, exp, mStart, mEnd))
        currM = currM.plusMonths(1)
    }

    return DashboardAnalyticsResult(
        filter = filter,
        startDate = startDate,
        endDate = endDate,
        netBalance = netBalance,
        totalIncome = totalIncome,
        totalExpense = totalExpense,
        netBalanceChangePercent = netBalanceChangePercent,
        incomeChangePercent = incomeChangePercent,
        expenseChangePercent = expenseChangePercent,
        savingsRate = savingsRate,
        monthlyAverageExpense = monthlyAverageExpense,
        highestIncome = highestIncome,
        highestExpense = highestExpense,
        top5Expenses = top5Expenses,
        incomeExpenseComparison = incomeExpenseComparison,
        balanceTrend = balancePoints,
        categoryDistribution = categoryDistributions,
        monthlyOverview = monthlyOverview.reversed(),
        prevTotalIncome = prevTotalIncome,
        prevTotalExpense = prevTotalExpense,
        prevNetBalance = prevNetBalance,
        hasData = currentTransactions.isNotEmpty(),
    )
}

private fun generatePeriods(startDate: LocalDate, endDate: LocalDate): List<Triple<LocalDate, LocalDate, String>> {
    val days = ChronoUnit.DAYS.between(startDate, endDate) + 1
    val list = mutableListOf<Triple<LocalDate, LocalDate, String>>()

    if (days <= 14) {
        var curr = startDate
        val fmt = DateTimeFormatter.ofPattern("EEE, d.M.", Locale.getDefault())
        val shortFmt = DateTimeFormatter.ofPattern("EEE", Locale.getDefault())
        while (!curr.isAfter(endDate)) {
            val label = if (days <= 7) curr.format(shortFmt) else curr.format(fmt)
            list.add(Triple(curr, curr, label))
            curr = curr.plusDays(1)
        }
    } else if (days <= 90) {
        var curr = startDate
        var weekNum = 1
        val fmt = DateTimeFormatter.ofPattern("d.M.", Locale.getDefault())
        while (!curr.isAfter(endDate)) {
            val wEnd = minOf(curr.plusDays(6), endDate)
            val label = "${curr.format(fmt)} - ${wEnd.format(fmt)}"
            list.add(Triple(curr, wEnd, label))
            curr = wEnd.plusDays(1)
            weekNum++
        }
    } else {
        var currMonth = YearMonth.from(startDate)
        val endMonth = YearMonth.from(endDate)
        val fmt = DateTimeFormatter.ofPattern("MMM", Locale.getDefault())
        while (!currMonth.isAfter(endMonth)) {
            val mStart = maxOf(startDate, currMonth.atDay(1))
            val mEnd = minOf(endDate, currMonth.atEndOfMonth())
            val label = currMonth.format(fmt)
            list.add(Triple(mStart, mEnd, label))
            currMonth = currMonth.plusMonths(1)
        }
    }
    return list
}
