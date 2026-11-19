package app.finora.ui

import androidx.compose.ui.graphics.Color
import java.text.NumberFormat
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Currency
import java.util.Locale

fun moneyFormatter(code: String): (Double) -> String {
    val nf = NumberFormat.getCurrencyInstance(Locale.getDefault())
    runCatching { nf.currency = Currency.getInstance(code) }
    return { nf.format(it) }
}

fun formatDate(date: LocalDate): String =
    date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(Locale.getDefault()))

fun formatDayHeader(date: LocalDate, today: LocalDate = LocalDate.now()): String = when (date) {
    today -> "Heute"
    today.minusDays(1) -> "Gestern"
    else -> date.format(DateTimeFormatter.ofPattern("EEEE, d. MMMM", Locale.getDefault()))
}

fun formatMonth(m: YearMonth): String =
    m.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault()))

fun shortMonth(m: YearMonth): String =
    m.format(DateTimeFormatter.ofPattern("MMM", Locale.getDefault()))

fun shortWeekday(d: LocalDate): String =
    d.format(DateTimeFormatter.ofPattern("EEE", Locale.getDefault()))

fun parseColor(hex: String): Color =
    runCatching { Color(android.graphics.Color.parseColor(hex)) }.getOrDefault(Color.Gray)
