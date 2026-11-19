package app.finora.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.finora.data.AnalyticsTimeFilter
import app.finora.data.AppTheme
import app.finora.data.BuiltIns
import app.finora.data.CategoryEntity
import app.finora.data.DashboardAnalyticsResult
import app.finora.data.ExpenseEntity
import app.finora.data.MonthAnalytics
import app.finora.data.Repository
import app.finora.data.Settings
import app.finora.data.computeAnalytics
import app.finora.data.computeDashboardAnalytics
import app.finora.data.systemCurrencyCode
import app.finora.widget.FinoraWidget
import androidx.glance.appwidget.updateAll
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.YearMonth

class MainViewModel(private val app: Application) : AndroidViewModel(app) {
    private val repo = Repository(app)
    private val sharing = SharingStarted.WhileSubscribed(5_000)

    val expenses: StateFlow<List<ExpenseEntity>> =
        repo.expenses.stateIn(viewModelScope, sharing, emptyList())

    val categories: StateFlow<List<CategoryEntity>> =
        repo.categories.stateIn(viewModelScope, sharing, emptyList())

    val budgets: StateFlow<Map<String, Double>> =
        repo.budgets.map { list -> list.associate { it.monthYear to it.amount } }
            .stateIn(viewModelScope, sharing, emptyMap())

    val settings: StateFlow<Settings> =
        repo.settings.stateIn(
            viewModelScope, sharing,
            Settings(systemCurrencyCode(), AppTheme.SYSTEM, BuiltIns.FOOD_ID, "DE"),
        )

    val selectedMonth = MutableStateFlow(YearMonth.now())

    /** Filter options for the new Dashboard Analytics */
    val analyticsFilter = MutableStateFlow(AnalyticsTimeFilter.THIS_MONTH)
    val customStartDate = MutableStateFlow<LocalDate?>(null)
    val customEndDate = MutableStateFlow<LocalDate?>(null)

    /** Aktueller Monat – für den Home-Screen. */
    val homeAnalytics: StateFlow<MonthAnalytics?> = analyticsFor(MutableStateFlow(YearMonth.now()))

    /** Vom Nutzer gewählter Monat – für den Analytics-Screen (Legacy). */
    val analytics: StateFlow<MonthAnalytics?> = analyticsFor(selectedMonth)

    /** Das umfassende Dashboard Analytics Resultat */
    val dashboardAnalytics: StateFlow<DashboardAnalyticsResult> =
        combine(
            expenses,
            categories,
            analyticsFilter,
            customStartDate,
            customEndDate,
        ) { e, c, filter, cStart, cEnd ->
            computeDashboardAnalytics(
                all = e,
                categories = c,
                filter = filter,
                customStart = cStart,
                customEnd = cEnd,
                today = LocalDate.now(),
            )
        }.flowOn(Dispatchers.Default).stateIn(
            viewModelScope,
            sharing,
            computeDashboardAnalytics(
                all = emptyList(),
                categories = emptyList(),
                filter = AnalyticsTimeFilter.THIS_MONTH,
                customStart = null,
                customEnd = null,
            )
        )

    private fun analyticsFor(month: Flow<YearMonth>): StateFlow<MonthAnalytics?> =
        combine(expenses, categories, budgets, month, settings) { e, c, b, m, s ->
            computeAnalytics(
                all = e,
                categoryNames = c.associate { it.id to it.name },
                budgets = b,
                month = m,
                today = LocalDate.now(),
                money = moneyFormatter(s.currency),
            )
        }.flowOn(Dispatchers.Default).stateIn(viewModelScope, sharing, null)

    init {
        viewModelScope.launch { repo.ensureSeeded() }
        // Widget aktuell halten, sobald sich Daten ändern.
        viewModelScope.launch {
            combine(repo.expenses, repo.budgets, repo.settings) { _, _, _ -> }.collect {
                runCatching { FinoraWidget().updateAll(app) }
            }
        }
    }

    fun changeMonth(delta: Int) { selectedMonth.value = selectedMonth.value.plusMonths(delta.toLong()) }

    fun setAnalyticsFilter(filter: AnalyticsTimeFilter) {
        analyticsFilter.value = filter
    }

    fun setCustomDateRange(start: LocalDate, end: LocalDate) {
        customStartDate.value = start
        customEndDate.value = end
        analyticsFilter.value = AnalyticsTimeFilter.CUSTOM
    }

    // --- Buchungen ---
    suspend fun loadExpense(id: String) = repo.getExpense(id)
    fun saveExpense(e: ExpenseEntity) = viewModelScope.launch { repo.saveExpense(e) }
    fun deleteExpenses(ids: List<String>) = viewModelScope.launch { repo.deleteExpenses(ids) }

    // --- Budget ---
    fun setBudget(month: YearMonth, amount: Double) =
        viewModelScope.launch { repo.setBudget(month.toString(), amount) }

    // --- Kategorien ---
    fun addCategory(name: String, icon: String, color: String) =
        viewModelScope.launch { repo.addCategory(name, icon, color) }
    fun updateCategory(c: CategoryEntity) = viewModelScope.launch { repo.saveCategory(c) }
    fun deleteCategory(id: String) = viewModelScope.launch { repo.deleteCustomCategory(id) }

    // --- Einstellungen ---
    fun setCurrency(code: String) = viewModelScope.launch { repo.settingsStore.setCurrency(code) }
    fun setTheme(t: AppTheme) = viewModelScope.launch { repo.settingsStore.setTheme(t) }
    fun setDefaultCategory(id: String) = viewModelScope.launch { repo.settingsStore.setDefaultCategory(id) }
    fun setLanguage(lang: String) = viewModelScope.launch { repo.settingsStore.setLanguage(lang) }

    fun exportTo(uri: Uri, onDone: (Boolean) -> Unit) = viewModelScope.launch {
        val ok = runCatching {
            val text = repo.exportJson()
            withContext(Dispatchers.IO) {
                app.contentResolver.openOutputStream(uri, "wt")!!.use { it.write(text.toByteArray()) }
            }
        }.isSuccess
        onDone(ok)
    }

    fun importFrom(uri: Uri, onDone: (Int?) -> Unit) = viewModelScope.launch {
        val count = runCatching {
            val text = withContext(Dispatchers.IO) {
                app.contentResolver.openInputStream(uri)!!.use { it.readBytes().decodeToString() }
            }
            repo.importJson(text)
        }.getOrNull()
        onDone(count)
    }

    fun resetAll() = viewModelScope.launch { repo.resetAll() }
}
