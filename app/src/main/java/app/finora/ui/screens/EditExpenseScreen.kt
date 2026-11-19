package app.finora.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.finora.data.BuiltIns
import app.finora.data.ExpenseEntity
import app.finora.data.TransactionType
import app.finora.data.localDate
import app.finora.data.toMillis
import app.finora.ui.MainViewModel
import app.finora.ui.components.CategoryBubble
import app.finora.ui.formatDate
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EditExpenseScreen(
    vm: MainViewModel,
    expenseId: String?,
    onClose: () -> Unit,
) {
    val categories by vm.categories.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()

    var loaded by remember { mutableStateOf(expenseId == null) }
    var original by remember { mutableStateOf<ExpenseEntity?>(null) }
    var title by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }
    var type by remember { mutableStateOf(TransactionType.EXPENSE) }
    var categoryId by remember { mutableStateOf<String?>(null) }
    var date by remember { mutableStateOf(LocalDate.now()) }
    var notes by remember { mutableStateOf("") }
    var showDatePicker by remember { mutableStateOf(false) }

    LaunchedEffect(expenseId) {
        if (expenseId != null) {
            vm.loadExpense(expenseId)?.let { e ->
                original = e
                title = e.title
                amountText = e.amount.toString().removeSuffix(".0")
                type = TransactionType.valueOf(e.type)
                categoryId = e.categoryId
                date = e.localDate()
                notes = e.notes.orEmpty()
            }
            loaded = true
        }
    }

    val visible = categories.filter {
        val isIncomeType = type == TransactionType.INCOME
        if (isIncomeType) {
            it.id.contains("income") || it.id.contains("salary") || it.id.contains("gift") || it.id == BuiltIns.OTHERS_ID || !it.hidden
        } else {
            !it.hidden
        }
    }

    val selectedId = categoryId ?: settings.defaultCategoryId.takeIf { id -> visible.any { it.id == id } }
        ?: visible.firstOrNull()?.id
    val amount = amountText.replace(',', '.').toDoubleOrNull()
    val canSave = loaded && amount != null && amount > 0 && selectedId != null

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (expenseId == null) "New Entry" else "Edit Entry") },
                navigationIcon = {
                    IconButton(onClick = onClose) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
                },
                actions = {
                    if (expenseId != null) {
                        IconButton(onClick = { vm.deleteExpenses(listOf(expenseId)); onClose() }) {
                            Icon(Icons.Filled.Delete, "Delete")
                        }
                    }
                    TextButton(
                        enabled = canSave,
                        onClick = {
                            val keepMillis = original?.takeIf { it.localDate() == date }?.dateMillis
                            val millis = keepMillis ?: date.atTime(LocalTime.now()).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                            val catName = categories.firstOrNull { it.id == selectedId }?.name ?: "Entry"
                            vm.saveExpense(
                                ExpenseEntity(
                                    id = original?.id ?: java.util.UUID.randomUUID().toString(),
                                    title = title.trim().ifEmpty { catName },
                                    amount = amount!!,
                                    dateMillis = millis,
                                    categoryId = selectedId!!,
                                    type = type.name,
                                    notes = notes.trim().ifEmpty { null },
                                )
                            )
                            onClose()
                        },
                    ) { Text("Save") }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier.padding(padding).padding(horizontal = 16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(type == TransactionType.EXPENSE, { type = TransactionType.EXPENSE }, label = { Text("Expense") })
                FilterChip(type == TransactionType.INCOME, { type = TransactionType.INCOME }, label = { Text("Income") })
            }

            OutlinedTextField(
                value = amountText,
                onValueChange = { amountText = it.filter { c -> c.isDigit() || c == '.' || c == ',' } },
                label = { Text("Amount (${settings.currency})") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Title") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            Text("Category", style = MaterialTheme.typography.titleSmall)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                visible.forEach { c ->
                    FilterChip(
                        selected = c.id == selectedId,
                        onClick = { categoryId = c.id },
                        label = { Text(c.name) },
                        leadingIcon = { CategoryBubble(c, size = 22.dp) },
                    )
                }
            }

            OutlinedButton(onClick = { showDatePicker = true }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Filled.CalendarMonth, null)
                Spacer(Modifier.width(8.dp))
                Text(formatDate(date))
            }

            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Notes (optional)") },
                minLines = 2,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(24.dp))
        }
    }

    if (showDatePicker) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let {
                        date = Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()
                    }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancel") } },
        ) { DatePicker(state = pickerState) }
    }
}
