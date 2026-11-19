package app.finora.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Locale

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

data class Settings(
    val currency: String,
    val theme: AppTheme,
    val defaultCategoryId: String,
    val language: String, // "DE" or "EN"
)

fun systemCurrencyCode(): String =
    runCatching { java.util.Currency.getInstance(Locale.getDefault()).currencyCode }.getOrDefault("EUR")

class SettingsStore(private val context: Context) {
    private val currencyKey = stringPreferencesKey("currency")
    private val themeKey = stringPreferencesKey("theme")
    private val defaultCategoryKey = stringPreferencesKey("default_category")
    private val languageKey = stringPreferencesKey("language")

    val settings: Flow<Settings> = context.dataStore.data.map { p ->
        Settings(
            currency = p[currencyKey] ?: systemCurrencyCode(),
            theme = p[themeKey]?.let { runCatching { AppTheme.valueOf(it) }.getOrNull() } ?: AppTheme.SYSTEM,
            defaultCategoryId = p[defaultCategoryKey] ?: BuiltIns.FOOD_ID,
            language = p[languageKey] ?: "DE",
        )
    }

    suspend fun setCurrency(code: String) = context.dataStore.edit { it[currencyKey] = code }
    suspend fun setTheme(theme: AppTheme) = context.dataStore.edit { it[themeKey] = theme.name }
    suspend fun setDefaultCategory(id: String) = context.dataStore.edit { it[defaultCategoryKey] = id }
    suspend fun setLanguage(lang: String) = context.dataStore.edit { it[languageKey] = lang }
    suspend fun reset() = context.dataStore.edit { it.clear() }
}
