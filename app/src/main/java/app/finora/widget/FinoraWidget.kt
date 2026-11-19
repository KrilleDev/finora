package app.finora.widget

import android.content.Context
import android.content.Intent
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.actionStartActivity
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import app.finora.MainActivity
import app.finora.data.Repository
import app.finora.data.yearMonth
import app.finora.ui.formatMonth
import app.finora.ui.moneyFormatter
import java.time.YearMonth

class FinoraWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val (expenses, budgets, settings) = Repository(context).snapshotForWidget()
        val month = YearMonth.now()
        val spent = expenses.filter { !it.isIncome && it.yearMonth() == month }.sumOf { it.amount }
        val budget = budgets.firstOrNull { it.monthYear == month.toString() }?.amount ?: 0.0
        val money = moneyFormatter(settings.currency)

        provideContent {
            GlanceTheme {
                val addIntent = Intent(context, MainActivity::class.java).apply {
                    action = MainActivity.ACTION_ADD
                    putExtra(MainActivity.EXTRA_ADD, true)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                }
                Column(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .background(GlanceTheme.colors.widgetBackground)
                        .padding(16.dp)
                        .clickable(actionStartActivity<MainActivity>()),
                ) {
                    Text(
                        formatMonth(month),
                        style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 12.sp),
                    )
                    Text(
                        money(spent),
                        style = TextStyle(color = GlanceTheme.colors.onSurface, fontSize = 26.sp, fontWeight = FontWeight.Bold),
                    )
                    if (budget > 0) {
                        val left = budget - spent
                        Text(
                            if (left >= 0) "Noch ${money(left)} von ${money(budget)}" else "Budget um ${money(-left)} überschritten",
                            style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 12.sp),
                        )
                    } else {
                        Text(
                            "Kein Budget gesetzt",
                            style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 12.sp),
                        )
                    }
                    Spacer(GlanceModifier.height(10.dp))
                    Row(
                        modifier = GlanceModifier
                            .fillMaxWidth()
                            .background(GlanceTheme.colors.primary)
                            .cornerRadius(20.dp)
                            .padding(vertical = 8.dp)
                            .clickable(actionStartActivity(addIntent)),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            "+ Ausgabe",
                            style = TextStyle(color = GlanceTheme.colors.onPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium),
                        )
                    }
                }
            }
        }
    }
}

class FinoraWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = FinoraWidget()
}
