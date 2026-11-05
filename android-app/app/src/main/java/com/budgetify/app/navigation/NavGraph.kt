package com.budgetify.app.navigation

import android.widget.Toast
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.budgetify.app.AppContainer
import com.budgetify.app.R
import com.budgetify.app.data.repository.SaveOutcome
import com.budgetify.app.ui.addexpense.AddExpenseScreen
import com.budgetify.app.ui.addexpense.AddExpenseViewModel
import com.budgetify.app.ui.budgets.BudgetsScreen
import com.budgetify.app.ui.budgets.BudgetsViewModel
import com.budgetify.app.ui.common.ViewModelFactory
import com.budgetify.app.ui.home.HomeScreen
import com.budgetify.app.ui.home.HomeViewModel
import com.budgetify.app.ui.login.LoginScreen
import com.budgetify.app.ui.login.LoginViewModel
import com.budgetify.app.ui.register.RegisterScreen
import com.budgetify.app.ui.register.RegisterViewModel
import com.budgetify.app.ui.rewards.RewardsScreen
import com.budgetify.app.ui.rewards.RewardsViewModel
import com.budgetify.app.ui.settings.SettingsScreen
import com.budgetify.app.ui.settings.SettingsViewModel

object Routes {
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val HOME = "home"
    const val BUDGETS = "budgets"
    const val REWARDS = "rewards"
    const val SETTINGS = "settings"
    const val ADD_EXPENSE = "add_expense"
}

private data class TabItem(val route: String, val label: Int, val icon: ImageVector)

private val tabs = listOf(
    TabItem(Routes.HOME, R.string.nav_home, Icons.Filled.Home),
    TabItem(Routes.BUDGETS, R.string.nav_budgets, Icons.Filled.ShoppingCart),
    TabItem(Routes.REWARDS, R.string.nav_rewards, Icons.Filled.Star),
    TabItem(Routes.SETTINGS, R.string.nav_settings, Icons.Filled.Settings)
)

@Composable
fun BudgetifyNavGraph(container: AppContainer) {
    val navController = rememberNavController()
    val context = LocalContext.current
    val startDestination = if (container.session.isLoggedIn()) Routes.HOME else Routes.LOGIN

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val sessionExpired by container.session.expired.collectAsState()
    LaunchedEffect(sessionExpired) {
        if (sessionExpired) {
            Toast.makeText(context, context.getString(R.string.session_expired), Toast.LENGTH_LONG).show()
            navController.navigate(Routes.LOGIN) { popUpTo(0) { inclusive = true } }
            container.session.acknowledgeExpired()
        }
    }

    val showBottomBar = tabs.any { it.route == currentRoute }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    tabs.forEach { tab ->
                        NavigationBarItem(
                            selected = currentRoute == tab.route,
                            onClick = {
                                navController.navigate(tab.route) {
                                    popUpTo(Routes.HOME) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(tab.icon, contentDescription = null) },
                            label = { Text(stringResource(tab.label)) }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding)
        ) {
            composable(Routes.LOGIN) {
                val vm: LoginViewModel = viewModel(
                    factory = ViewModelFactory { LoginViewModel(container.authRepository, container::afterLogin, container.prefs, container.api) }
                )
                LoginScreen(
                    viewModel = vm,
                    onLoginSuccess = {
                        navController.navigate(Routes.HOME) { popUpTo(Routes.LOGIN) { inclusive = true } }
                    },
                    onNavigateToRegister = { navController.navigate(Routes.REGISTER) }
                )
            }

            composable(Routes.REGISTER) {
                val vm: RegisterViewModel = viewModel(
                    factory = ViewModelFactory { RegisterViewModel(container.authRepository, container::afterLogin) }
                )
                RegisterScreen(
                    viewModel = vm,
                    onRegisterSuccess = {
                        navController.navigate(Routes.HOME) { popUpTo(Routes.LOGIN) { inclusive = true } }
                    },
                    onNavigateToLogin = { navController.popBackStack() }
                )
            }

            composable(Routes.HOME) {
                val vm: HomeViewModel = viewModel(
                    factory = ViewModelFactory {
                        HomeViewModel(container.budgetRepository, container.session, container.networkMonitor, container.prefs)
                    }
                )
                HomeScreen(viewModel = vm, onAddExpense = { navController.navigate(Routes.ADD_EXPENSE) })
            }

            composable(Routes.BUDGETS) {
                val vm: BudgetsViewModel = viewModel(
                    factory = ViewModelFactory { BudgetsViewModel(container.budgetRepository, container.prefs) }
                )
                BudgetsScreen(viewModel = vm)
            }

            composable(Routes.REWARDS) {
                val vm: RewardsViewModel = viewModel(
                    factory = ViewModelFactory { RewardsViewModel(container.budgetRepository) }
                )
                RewardsScreen(viewModel = vm)
            }

            composable(Routes.SETTINGS) {
                val vm: SettingsViewModel = viewModel(
                    factory = ViewModelFactory {
                        SettingsViewModel(
                            container.settingsRepository, container.authRepository,
                            container.api, container.prefs, container.session
                        )
                    }
                )
                SettingsScreen(
                    viewModel = vm,
                    onLoggedOut = {
                        navController.navigate(Routes.LOGIN) { popUpTo(0) { inclusive = true } }
                    }
                )
            }

            composable(Routes.ADD_EXPENSE) {
                val vm: AddExpenseViewModel = viewModel(
                    factory = ViewModelFactory { AddExpenseViewModel(container.budgetRepository) }
                )
                AddExpenseScreen(
                    viewModel = vm,
                    onBack = { navController.popBackStack() },
                    onSaved = { outcome ->
                        val message = if (outcome == SaveOutcome.SYNCED) R.string.saved_synced else R.string.saved_offline
                        Toast.makeText(context, context.getString(message), Toast.LENGTH_SHORT).show()
                        navController.popBackStack()
                    }
                )
            }
        }
    }
}
