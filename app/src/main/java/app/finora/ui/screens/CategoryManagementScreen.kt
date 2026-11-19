package app.finora.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.finora.data.CategoryEntity
import app.finora.ui.MainViewModel
import app.finora.ui.components.CategoryBubble
import app.finora.ui.components.CategoryIcons
import app.finora.ui.components.CategoryPalette
import app.finora.ui.components.iconFor
import app.finora.ui.parseColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryManagementScreen(vm: MainViewModel, onBack: () -> Unit) {
    val categories by vm.categories.collectAsStateWithLifecycle()
    var editing by remember { mutableStateOf<CategoryEntity?>(null) }
    var creating by remember { mutableStateOf(false) }
    var toDelete by remember { mutableStateOf<CategoryEntity?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Kategorien") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Zurück") } },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { creating = true }) { Icon(Icons.Filled.Add, "Neue Kategorie") }
        },
    ) { padding ->
        LazyColumn(Modifier.padding(padding), contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 96.dp)) {
            item {
                Text(
                    "Ausgeblendete Kategorien erscheinen nicht mehr bei neuen Buchungen, alte Buchungen bleiben erhalten.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
            }
            items(categories, key = { it.id }) { c ->
                Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    CategoryBubble(c)
                    Text(c.name, Modifier.weight(1f).padding(horizontal = 12.dp))
                    IconButton(onClick = { editing = c }) { Icon(Icons.Filled.Edit, "Bearbeiten") }
                    if (c.isCustom) {
                        IconButton(onClick = { toDelete = c }) { Icon(Icons.Filled.Delete, "Löschen") }
                    }
                    Switch(checked = !c.hidden, onCheckedChange = { vm.updateCategory(c.copy(hidden = !it)) })
                }
            }
        }
    }

    if (creating) {
        CategoryDialog(null, { creating = false }) { name, icon, color ->
            vm.addCategory(name, icon, color); creating = false
        }
    }
    editing?.let { c ->
        CategoryDialog(c, { editing = null }) { name, icon, color ->
            vm.updateCategory(c.copy(name = name.trim(), iconKey = icon, colorHex = color)); editing = null
        }
    }
    toDelete?.let { c ->
        AlertDialog(
            onDismissRequest = { toDelete = null },
            title = { Text("„${c.name}“ löschen?") },
            text = { Text("Zugehörige Buchungen werden „Sonstiges“ zugeordnet.") },
            confirmButton = {
                TextButton(onClick = { vm.deleteCategory(c.id); toDelete = null }) {
                    Text("Löschen", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { toDelete = null }) { Text("Abbrechen") } },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CategoryDialog(
    initial: CategoryEntity?,
    onDismiss: () -> Unit,
    onSave: (name: String, icon: String, color: String) -> Unit,
) {
    var name by remember { mutableStateOf(initial?.name.orEmpty()) }
    var icon by remember { mutableStateOf(initial?.iconKey ?: "more") }
    var color by remember { mutableStateOf(initial?.colorHex ?: CategoryPalette.first()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Neue Kategorie" else "Kategorie bearbeiten") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text("Name") }, singleLine = true)
                Text("Symbol", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    CategoryIcons.keys.forEach { key ->
                        val selected = key == icon
                        Box(
                            Modifier.size(40.dp).clip(CircleShape)
                                .background(if (selected) parseColor(color).copy(alpha = 0.25f) else MaterialTheme.colorScheme.surfaceVariant)
                                .clickable { icon = key },
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(iconFor(key), null, tint = if (selected) parseColor(color) else MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                Text("Farbe", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    CategoryPalette.forEach { hex ->
                        Box(
                            Modifier.size(32.dp).clip(CircleShape).background(parseColor(hex))
                                .then(
                                    if (hex == color) Modifier.border(BorderStroke(3.dp, MaterialTheme.colorScheme.onSurface), CircleShape)
                                    else Modifier
                                )
                                .clickable { color = hex },
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(enabled = name.isNotBlank(), onClick = { onSave(name, icon, color) }) { Text("Speichern") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Abbrechen") } },
    )
}
