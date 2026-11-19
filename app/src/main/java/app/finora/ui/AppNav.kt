package app.finora.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import app.finora.ui.screens.AnalyticsScreen
import app.finora.ui.screens.CategoryManagementScreen
import app.finora.ui.screens.EditExpenseScreen
import app.finora.ui.screens.HomeScreen
import app.finora.ui.screens.SettingsScreen
import app.finora.ui.screens.TransactionsScreen

private enum class Tab(val route: String, val label: String, val icon: ImageVector) {
    Home("home", "Start", Icons.Filled.Home),
    Analytics("analytics", "Analytics", Icons.Filled.PieChart),
    Transactions("transactions", "Buchungen", Icons.Filled.Receipt),
    Settings("settings", "Mehr", Icons.Filled.Settings),
}

private const val EDIT_ROUTE = "edit?id={id}"

@Composable
fun FinoraApp(vm: MainViewModel, addRequest: Int) {
    val nav = rememberNavController()
    val snackbar = remember { SnackbarHostState() }
    val backStack by nav.currentBackStackEntryAsState()
    val route = backStack?.destination?.route
    val onTab = Tab.entries.any { it.route == route }

    LaunchedEffect(addRequest) {
        if (addRequest > 0) nav.navigate("edit")
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            if (onTab) {
                NavigationBar {
                    Tab.entries.forEach { tab ->
                        NavigationBarItem(
                            selected = route == tab.route,
                            onClick = {
                                nav.navigate(tab.route) {
                                    popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(tab.icon, null) },
                            label = { Text(tab.label) },
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            if (route == Tab.Home.route || route == Tab.Transactions.route) {
                FloatingActionButton(onClick = { nav.navigate("edit") }) { Icon(Icons.Filled.Add, "Buchung hinzufügen") }
            }
        },
    ) { padding ->
        NavHost(nav, startDestination = Tab.Home.route, modifier = Modifier.padding(padding)) {
            composable(Tab.Home.route) {
                HomeScreen(
                    vm,
                    onOpenExpense = { nav.navigate("edit?id=$it") },
                    onSeeAll = { nav.navigate(Tab.Transactions.route) },
                )
            }
            composable(Tab.Analytics.route) {
                AnalyticsScreen(vm, onOpenExpense = { nav.navigate("edit?id=$it") })
            }
            composable(Tab.Transactions.route) {
                TransactionsScreen(vm, snackbar, onOpenExpense = { nav.navigate("edit?id=$it") })
            }
            composable(Tab.Settings.route) {
                SettingsScreen(vm, onOpenCategories = { nav.navigate("categories") })
            }
            composable(
                EDIT_ROUTE,
                arguments = listOf(navArgument("id") { type = NavType.StringType; nullable = true; defaultValue = null }),
            ) { entry ->
                EditExpenseScreen(vm, entry.arguments?.getString("id"), onClose = { nav.popBackStack() })
            }
            composable("categories") { CategoryManagementScreen(vm, onBack = { nav.popBackStack() }) }
        }
    }
}
