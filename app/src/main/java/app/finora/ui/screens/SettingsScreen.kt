package app.finora.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.finora.data.AvailableCurrencies
import app.finora.ui.MainViewModel

@Composable
fun SettingsScreen(
    vm: MainViewModel,
    onOpenCategories: () -> Unit = {},
    onClose: (() -> Unit)? = null,
) {
    val settings by vm.settings.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        Text("Settings", style = MaterialTheme.typography.headlineMedium)

        // Language Selection
        Text("Language", style = MaterialTheme.typography.titleMedium)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(
                    selected = settings.language == "DE",
                    onClick = { vm.setLanguage("DE") }
                )
                Text("Deutsch")
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(
                    selected = settings.language == "EN",
                    onClick = { vm.setLanguage("EN") }
                )
                Text("English")
            }
        }

        // Currency Selection
        Text("Currency", style = MaterialTheme.typography.titleMedium)
        LazyColumn(modifier = Modifier.height(200.dp)) {
            items(AvailableCurrencies) { currency ->
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { vm.setCurrency(currency.code) }
                        .padding(vertical = 4.dp),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(currency.name)
                        Spacer(Modifier.weight(1f))
                        Text(currency.symbol)
                    }
                }
            }
        }

        // Categories Selection
        Text("Categories", style = MaterialTheme.typography.titleMedium)
        OutlinedButton(
            onClick = onOpenCategories,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Kategorien verwalten")
        }

        // Theme Selection
        Text("Appearance", style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            // Note: For simplicity, this is a placeholder for the theme switching logic
            // which would ideally use a toggle or a more complex selection.
            Text("Light Mode")
            Text("Dark Mode")
            Text("System Default")
        }

        if (onClose != null) {
            Spacer(Modifier.weight(1f))
            Button(onClick = onClose, modifier = Modifier.fillMaxWidth()) {
                Text("Back")
            }
        }
    }
}
