package app.finora.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseDao {
    @Query("SELECT * FROM expenses ORDER BY dateMillis DESC")
    fun observeAll(): Flow<List<ExpenseEntity>>

    @Query("SELECT * FROM expenses")
    suspend fun all(): List<ExpenseEntity>

    @Query("SELECT * FROM expenses WHERE id = :id")
    suspend fun get(id: String): ExpenseEntity?

    @Upsert
    suspend fun upsert(e: ExpenseEntity)

    @Upsert
    suspend fun upsertAll(list: List<ExpenseEntity>)

    @Query("DELETE FROM expenses WHERE id IN (:ids)")
    suspend fun delete(ids: List<String>)

    @Query("DELETE FROM expenses")
    suspend fun clear()

    @Query("UPDATE expenses SET categoryId = :to WHERE categoryId = :from")
    suspend fun reassign(from: String, to: String)
}

@Dao
interface BudgetDao {
    @Query("SELECT * FROM budgets")
    fun observeAll(): Flow<List<BudgetEntity>>

    @Query("SELECT * FROM budgets")
    suspend fun all(): List<BudgetEntity>

    @Upsert
    suspend fun upsert(b: BudgetEntity)

    @Upsert
    suspend fun upsertAll(list: List<BudgetEntity>)

    @Query("DELETE FROM budgets WHERE monthYear = :key")
    suspend fun remove(key: String)

    @Query("DELETE FROM budgets")
    suspend fun clear()
}

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories ORDER BY sortOrder, name")
    fun observeAll(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories")
    suspend fun all(): List<CategoryEntity>

    @Query("SELECT COUNT(*) FROM categories")
    suspend fun count(): Int

    @Query("SELECT COALESCE(MAX(sortOrder), 0) FROM categories")
    suspend fun maxOrder(): Int

    @Upsert
    suspend fun upsert(c: CategoryEntity)

    @Upsert
    suspend fun upsertAll(list: List<CategoryEntity>)

    @Query("DELETE FROM categories WHERE id = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM categories WHERE isCustom = 1")
    suspend fun clearCustom()
}

@Dao
interface SubscriptionDao {
    @Query("SELECT * FROM subscriptions ORDER BY nextDueDateMillis ASC")
    fun observeAll(): Flow<List<SubscriptionEntity>>

    @Query("SELECT * FROM subscriptions")
    suspend fun all(): List<SubscriptionEntity>

    @Upsert
    suspend fun upsert(s: SubscriptionEntity)

    @Upsert
    suspend fun upsertAll(list: List<SubscriptionEntity>)

    @Query("DELETE FROM subscriptions WHERE id = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM subscriptions WHERE id IN (:ids)")
    suspend fun delete(ids: List<String>)

    @Query("DELETE FROM subscriptions")
    suspend fun clear()
}

@Database(
    entities = [ExpenseEntity::class, BudgetEntity::class, CategoryEntity::class, SubscriptionEntity::class],
    version = 2,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun expenses(): ExpenseDao
    abstract fun budgets(): BudgetDao
    abstract fun categories(): CategoryDao
    abstract fun subscriptions(): SubscriptionDao

    companion object {
        @Volatile private var instance: AppDatabase? = null

        fun get(context: Context): AppDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext, AppDatabase::class.java, "finora.db"
            ).fallbackToDestructiveMigration().build().also { instance = it }
        }
    }
}
