package app.finora.data

import android.content.Context
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.util.UUID

class Repository(context: Context) {
    private val db = AppDatabase.get(context)
    val settingsStore = SettingsStore(context)

    val expenses: Flow<List<ExpenseEntity>> = db.expenses().observeAll()
    val categories: Flow<List<CategoryEntity>> = db.categories().observeAll()
    val budgets: Flow<List<BudgetEntity>> = db.budgets().observeAll()
    val settings: Flow<Settings> = settingsStore.settings
    val subscriptions: Flow<List<SubscriptionEntity>> = db.subscriptions().observeAll()

    suspend fun ensureSeeded() {
        if (db.categories().count() == 0) seedBuiltIns()
    }

    private suspend fun seedBuiltIns() {
        db.categories().upsertAll(
            BuiltIns.list.mapIndexed { i, c ->
                CategoryEntity(c.id, c.name, c.iconKey, c.colorHex, isCustom = false, sortOrder = i)
            }
        )
    }

    suspend fun getExpense(id: String) = db.expenses().get(id)
    suspend fun saveExpense(e: ExpenseEntity) = db.expenses().upsert(e)
    suspend fun deleteExpenses(ids: List<String>) = db.expenses().delete(ids)

    suspend fun setBudget(monthKey: String, amount: Double) {
        if (amount <= 0) db.budgets().remove(monthKey) else db.budgets().upsert(BudgetEntity(monthKey, amount))
    }

    suspend fun saveCategory(c: CategoryEntity) = db.categories().upsert(c)

    suspend fun addCategory(name: String, iconKey: String, colorHex: String) {
        val order = db.categories().maxOrder() + 1
        db.categories().upsert(
            CategoryEntity("custom-${UUID.randomUUID()}", name.trim(), iconKey, colorHex, true, order)
        )
    }

    /** Löscht eine eigene Kategorie; zugehörige Buchungen wandern zu "Sonstiges". */
    suspend fun deleteCustomCategory(id: String) {
        db.expenses().reassign(id, BuiltIns.OTHERS_ID)
        db.categories().delete(id)
    }

    suspend fun exportJson(): String = Backup.export(
        db.expenses().all(), db.budgets().all(), db.categories().all()
    )

    /** Gibt die Anzahl importierter Buchungen zurück oder wirft bei ungültiger Datei. */
    suspend fun importJson(text: String): Int {
        val known = db.categories().all()
        val parsed = Backup.parse(text, known.map { it.id }.toSet())
        // Eigene Kategorien nur ergänzen, vorhandene nicht überschreiben.
        val knownIds = known.map { it.id }.toSet()
        db.categories().upsertAll(parsed.categories.filter { it.id !in knownIds })
        db.expenses().upsertAll(parsed.expenses)
        db.budgets().upsertAll(parsed.budgets)
        return parsed.expenses.size
    }

    suspend fun resetAll() {
        db.expenses().clear()
        db.budgets().clear()
        db.categories().clearCustom()
        db.categories().upsertAll(
            BuiltIns.list.mapIndexed { i, c ->
                CategoryEntity(c.id, c.name, c.iconKey, c.colorHex, false, i)
            }
        )
        settingsStore.reset()
    }

    suspend fun snapshotForWidget(): Triple<List<ExpenseEntity>, List<BudgetEntity>, Settings> {
        return Triple(db.expenses().all(), db.budgets().all(), settings.first())
    }

    suspend fun saveSubscription(s: SubscriptionEntity) = db.subscriptions().upsert(s)
    suspend fun deleteSubscription(id: String) = db.subscriptions().delete(id)
    suspend fun deleteSubscriptions(ids: List<String>) = db.subscriptions().delete(ids)
}
