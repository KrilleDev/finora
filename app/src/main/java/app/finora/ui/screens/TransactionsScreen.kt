package app.finora.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.finora.data.BuiltIns
import app.finora.data.ExpenseEntity
import app.finora.data.TransactionType
import app.finora.data.localDate
import app.finora.ui.MainViewModel
import app.finora.ui.components.EmptyHint
import app.finora.ui.components.TransactionRow
import app.finora.ui.formatDayHeader
import app.finora.ui.moneyFormatter
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionsScreen(
    vm: MainViewModel,
    snackbar: SnackbarHostState,
    onOpenExpense: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val expenses by vm.expenses.collectAsStateWithLifecycle()
    val categories by vm.categories.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()
    val money = moneyFormatter(settings.currency)
    val catMap = categories.associateBy { it.id }
    val scope = rememberCoroutineScope()

    var query by remember { mutableStateOf("") }
    var typeFilter by remember { mutableStateOf<TransactionType?>(null) }
    var categoryFilter by remember { mutableStateOf<String?>(null) }

    val usedIds = expenses.map { it.categoryId }.toSet()
    val filterCategories = categories.filter { !it.hidden || it.id in usedIds }

    val filtered = expenses.filter { e ->
        (typeFilter == null || e.type == typeFilter!!.name) &&
            (categoryFilter == null || e.categoryId == categoryFilter) &&
            (query.isBlank() ||
                e.title.contains(query, ignoreCase = true) ||
                (e.notes?.contains(query, ignoreCase = true) == true))
    }
    val grouped = filtered.groupBy { it.localDate() }.toSortedMap(reverseOrder())

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 96.dp),
    ) {
        item {
            Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text("Buchungen durchsuchen") },
                    leadingIcon = { Icon(Icons.Filled.Search, null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.extraLarge,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(typeFilter == null, { typeFilter = null }, label = { Text("Alle") })
                    FilterChip(typeFilter == TransactionType.EXPENSE, { typeFilter = TransactionType.EXPENSE }, label = { Text("Ausgaben") })
                    FilterChip(typeFilter == TransactionType.INCOME, { typeFilter = TransactionType.INCOME }, label = { Text("Einnahmen") })
                }
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(filterCategories, key = { it.id }) { c ->
                        FilterChip(
                            selected = categoryFilter == c.id,
                            onClick = { categoryFilter = if (categoryFilter == c.id) null else c.id },
                            label = { Text(c.name) },
                        )
                    }
                }
            }
        }

        if (filtered.isEmpty()) {
            item { EmptyHint(if (expenses.isEmpty()) "Noch keine Buchungen" else "Keine Treffer") }
        }

        grouped.forEach { (day, list) ->
            item(key = "header-$day") {
                val total = list.filter { !it.isIncome }.sumOf { it.amount }
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp).padding(top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        formatDayHeader(day),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f),
                    )
                    if (total > 0) {
                        Text(money(total), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            items(list, key = { it.id }) { e ->
                SwipeRow(
                    onDelete = {
                        vm.deleteExpenses(listOf(e.id))
                        scope.launch {
                            val r = snackbar.showSnackbar("„${e.title}“ gelöscht", "Rückgängig", duration = SnackbarDuration.Short)
                            if (r == SnackbarResult.ActionPerformed) vm.saveExpense(e)
                        }
                    },
                ) {
                    TransactionRow(
                        e, catMap[e.categoryId] ?: catMap[BuiltIns.OTHERS_ID], money,
                        modifier = Modifier.padding(horizontal = 16.dp),
                    ) { onOpenExpense(e.id) }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeRow(onDelete: () -> Unit, content: @Composable () -> Unit) {
    val state = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) { onDelete(); true } else false
        }
    )
    SwipeToDismissBox(
        state = state,
        enableDismissFromStartToEnd = false,
        backgroundContent = {
            Box(
                Modifier.fillMaxSize().background(MaterialTheme.colorScheme.errorContainer).padding(end = 20.dp),
                contentAlignment = Alignment.CenterEnd,
            ) { Icon(Icons.Filled.Delete, "Löschen", tint = MaterialTheme.colorScheme.onErrorContainer) }
        },
    ) {
        Surface(color = MaterialTheme.colorScheme.background) { content() }
    }
}
