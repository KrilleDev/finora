package app.finora.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.util.UUID

@Serializable
data class BackupFile(
    val format: String = "finora-android",
    val version: Int = 1,
    val expenses: List<ExpenseEntity>,
    val budgets: List<BudgetEntity>,
    val categories: List<CategoryEntity>,
)

data class ParsedBackup(
    val expenses: List<ExpenseEntity>,
    val budgets: List<BudgetEntity>,
    val categories: List<CategoryEntity>,
)

object Backup {
    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true; encodeDefaults = true }

    // iOS speichert Datum als Sekunden seit 2001-01-01 (Swift-Standard).
    private const val APPLE_EPOCH_OFFSET_SECONDS = 978_307_200L

    fun export(
        expenses: List<ExpenseEntity>,
        budgets: List<BudgetEntity>,
        categories: List<CategoryEntity>,
    ): String = json.encodeToString(BackupFile(expenses = expenses, budgets = budgets, categories = categories))

    /** Liest sowohl das Android-Format als auch den JSON-Export der iOS-App. */
    fun parse(text: String, knownCategoryIds: Set<String>): ParsedBackup {
        val root = json.parseToJsonElement(text)
        return when (root) {
            is JsonObject -> {
                val file = json.decodeFromString<BackupFile>(text)
                ParsedBackup(file.expenses, file.budgets, file.categories.filter { it.isCustom })
            }
            is JsonArray -> ParsedBackup(parseIos(root, knownCategoryIds), emptyList(), emptyList())
            else -> error("Unbekanntes Dateiformat")
        }
    }

    private fun parseIos(array: JsonArray, known: Set<String>): List<ExpenseEntity> = array.mapNotNull { el ->
        val o = el.jsonObject
        val title = o["title"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
        val price = o["price"]?.jsonPrimitive?.doubleOrNull ?: return@mapNotNull null
        val dateSeconds = o["date"]?.jsonPrimitive?.doubleOrNull ?: return@mapNotNull null
        val rawCategory = o["categoryID"]?.jsonPrimitive?.contentOrNull
            ?: o["category"]?.jsonPrimitive?.contentOrNull?.let { "builtin-$it" }
        val categoryId = if (rawCategory != null && rawCategory in known) rawCategory else BuiltIns.OTHERS_ID
        val type = if (o["type"]?.jsonPrimitive?.contentOrNull == "income")
            TransactionType.INCOME.name else TransactionType.EXPENSE.name

        ExpenseEntity(
            id = o["id"]?.jsonPrimitive?.contentOrNull ?: UUID.randomUUID().toString(),
            title = title,
            amount = price,
            dateMillis = ((dateSeconds + APPLE_EPOCH_OFFSET_SECONDS) * 1000).toLong(),
            categoryId = categoryId,
            type = type,
            notes = o["notes"]?.jsonPrimitive?.contentOrNull,
        )
    }
}
