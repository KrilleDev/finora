package app.finora.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable
import java.util.UUID

enum class TransactionType { EXPENSE, INCOME }

enum class AppTheme(val label: String) {
    SYSTEM("System"), LIGHT("Hell"), DARK("Dunkel")
}

@Serializable
@Entity(tableName = "expenses")
data class ExpenseEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val title: String,
    val amount: Double,
    val dateMillis: Long,
    val categoryId: String,
    val type: String = TransactionType.EXPENSE.name,
    val notes: String? = null,
) {
    val isIncome: Boolean get() = type == TransactionType.INCOME.name
}

/** Budget pro Monat, Schlüssel im Format "yyyy-MM". */
@Serializable
@Entity(tableName = "budgets")
data class BudgetEntity(
    @PrimaryKey val monthYear: String,
    val amount: Double,
)

@Serializable
@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey val id: String,
    val name: String,
    val iconKey: String,
    val colorHex: String,
    val isCustom: Boolean,
    val sortOrder: Int,
    val hidden: Boolean = false,
)

data class BuiltInCategory(val id: String, val name: String, val iconKey: String, val colorHex: String)

/** Gleiche IDs und Farben wie in der iOS-App ("builtin-food" usw.), damit Importe passen. */
object BuiltIns {
    const val OTHERS_ID = "builtin-others"
    const val FOOD_ID = "builtin-food"

    val list = listOf(
        BuiltInCategory("builtin-food", "Lebensmittel", "cart", "#34C759"),
        BuiltInCategory("builtin-eatingOut", "Essen gehen", "restaurant", "#00C7BE"),
        BuiltInCategory("builtin-rent", "Miete", "home", "#AF52DE"),
        BuiltInCategory("builtin-shopping", "Shopping", "bag", "#FF9500"),
        BuiltInCategory("builtin-entertainment", "Unterhaltung", "tv", "#FF2D55"),
        BuiltInCategory("builtin-transportation", "Transport", "car", "#007AFF"),
        BuiltInCategory("builtin-utilities", "Nebenkosten", "bolt", "#FFCC00"),
        BuiltInCategory("builtin-subscriptions", "Abos", "repeat", "#30B0C7"),
        BuiltInCategory("builtin-healthcare", "Gesundheit", "heart", "#FF3B30"),
        BuiltInCategory("builtin-education", "Bildung", "book", "#5856D6"),
        BuiltInCategory(OTHERS_ID, "Sonstiges", "more", "#8E8E93"),
    )
}

data class Currency(val code: String, val symbol: String, val name: String)

val AvailableCurrencies = listOf(
    Currency("EUR", "€", "Euro"),
    Currency("USD", "$", "US-Dollar"),
    Currency("GBP", "£", "Britisches Pfund"),
    Currency("CHF", "Fr", "Schweizer Franken"),
    Currency("MDL", "L", "Moldauischer Leu"),
    Currency("JPY", "¥", "Japanischer Yen"),
    Currency("CAD", "$", "Kanadischer Dollar"),
    Currency("AUD", "$", "Australischer Dollar"),
    Currency("CNY", "¥", "Chinesischer Yuan"),
    Currency("INR", "₹", "Indische Rupie"),
    Currency("PLN", "zł", "Polnischer Złoty"),
    Currency("TRY", "₺", "Türkische Lira"),
)

@Serializable
@Entity(tableName = "subscriptions")
data class SubscriptionEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val title: String,
    val amount: Double,
    val nextDueDateMillis: Long,
    val frequency: String, // "Monthly", "Yearly", "One-time"
    val isActive: Boolean = true,
    val categoryId: String
)
