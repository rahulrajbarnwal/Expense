package com.icit.expense

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.google.firebase.auth.FirebaseAuth
import com.icit.expense.core.font.AppFont
import com.icit.expense.notification.NotificationScheduler
import com.icit.expense.ui.AddExpenseScreen
import com.icit.expense.ui.AnalyticsScreen
import com.icit.expense.ui.AnalyticsViewModel
import com.icit.expense.ui.AppDeepLinks
import com.icit.expense.ui.BudgetPlannerScreen
import com.icit.expense.ui.CategoryManagementScreen
import com.icit.expense.ui.ExpenseHomeScreen
import com.icit.expense.ui.ExpenseViewModel
import com.icit.expense.ui.FeedbackScreen
import com.icit.expense.ui.FeedbackViewModel
import com.icit.expense.ui.HistoryScreen
import com.icit.expense.ui.LockScreen
import com.icit.expense.ui.LoginScreen
import com.icit.expense.ui.NavDestination
import com.icit.expense.ui.NotificationsScreen
import com.icit.expense.ui.ProfileScreen
import com.icit.expense.ui.SecurityScreen
import com.icit.expense.ui.theme.ExpenseTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : androidx.fragment.app.FragmentActivity() {

    /**
     * Destination requested by the launching intent (app shortcut, deep link or notification),
     * or null for a plain app icon tap. Consumed once the user reaches the home screen — the app
     * must not jump past the lock or login screen just because a deep link asked for it.
     */
    private var pendingDestination by mutableStateOf<NavDestination?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // savedInstanceState != null means a config change replayed the original intent — already handled.
        if (savedInstanceState == null) {
            pendingDestination = AppDeepLinks.resolve(intent)
        }

        setContent {
            val viewModel: ExpenseViewModel = viewModel()
            val biometricEnabled by viewModel.biometricEnabled.collectAsState()
            
            // Keep the splash screen on-screen until biometricEnabled is loaded
            splashScreen.setKeepOnScreenCondition {
                biometricEnabled == null
            }

            // Notification Permission Handling
            val permissionLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestPermission()
            ) { _ -> }

            LaunchedEffect(Unit) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }

            // Sync data if user is already logged in
            LaunchedEffect(Unit) {
                if (FirebaseAuth.getInstance().currentUser != null) {
                    viewModel.syncData()
                }
            }

            val themeMode by viewModel.themeMode.collectAsState()
            val fontIndex by viewModel.fontIndex.collectAsState()
            val hasSkippedLogin by viewModel.hasSkippedLogin.collectAsState()
            
            val isUserLoggedIn = FirebaseAuth.getInstance().currentUser != null
            var isAppUnlocked by remember { mutableStateOf(false) }
            
            // If biometric is explicitly disabled, the app is "unlocked"
            LaunchedEffect(biometricEnabled) {
                if (biometricEnabled == false) {
                    isAppUnlocked = true
                }
            }

            val startDestination = remember(isUserLoggedIn, hasSkippedLogin, biometricEnabled, isAppUnlocked) {
                if (isUserLoggedIn || hasSkippedLogin) {
                    if (biometricEnabled == true && !isAppUnlocked) {
                        NavDestination.Lock
                    } else {
                        NavDestination.ExpenseHome
                    }
                } else {
                    NavDestination.Login
                }
            }

            val darkTheme = when (themeMode) {
                1 -> false // Off
                2 -> true // On
                else -> isSystemInDarkTheme() // Auto
            }

            val appFont = AppFont.entries.getOrElse(fontIndex) { AppFont.DEFAULT }
            
            ExpenseTheme(darkTheme = darkTheme, appFont = appFont) {
                ExpenseApp(
                    viewModel = viewModel,
                    startDestination = startDestination,
                    pendingDestination = pendingDestination,
                    onDeepLinkHandled = { pendingDestination = null },
                    onUnlock = { isAppUnlocked = true }
                )
            }
        }
    }

    /** The launcher activity is single-task-ish in practice: a shortcut tapped while running lands here. */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingDestination = AppDeepLinks.resolve(intent)
    }
}

@Composable
fun ExpenseApp(
    viewModel: ExpenseViewModel,
    startDestination: NavDestination,
    pendingDestination: NavDestination? = null,
    onDeepLinkHandled: () -> Unit = {},
    onUnlock: () -> Unit
) {
    // Re-initialize backstack if startDestination changes significantly (e.g. Lock -> Home)
    val backStack = rememberNavBackStack(startDestination)

    // Ensure the backStack matches the intended start destination if it was initialized too early
    LaunchedEffect(startDestination) {
        if (backStack.isNotEmpty() && backStack[0] != startDestination) {
            if (startDestination == NavDestination.Lock) {
                backStack.add(0, NavDestination.Lock)
            }
        }
    }

    // Deep links and shortcuts wait for home: keyed on the top of the stack so this re-runs after the
    // user gets through Lock or Login, whichever path they took.
    LaunchedEffect(pendingDestination, backStack.lastOrNull()) {
        val destination = pendingDestination ?: return@LaunchedEffect
        if (backStack.lastOrNull() == NavDestination.ExpenseHome) {
            backStack.add(destination)
            onDeepLinkHandled()
        }
    }

    NavDisplay(
        backStack = backStack,
        onBack = { 
            if (backStack.isNotEmpty()) {
                backStack.removeAt(backStack.size - 1) 
            }
        },
        entryDecorators = listOf(rememberSaveableStateHolderNavEntryDecorator()),
        entryProvider = entryProvider {
            entry<NavDestination.Login> {
                val coroutineScope = rememberCoroutineScope()
                val context = LocalContext.current
                LoginScreen(
                    onLoginSuccess = {
                        viewModel.syncData()
                        // Reschedule with defaults upon login
                        coroutineScope.launch {
                            NotificationScheduler.scheduleDailyNotifications(context, viewModel.settingsRepository)
                        }
                        // Clear backstack so user can't go back to login
                        while (backStack.isNotEmpty()) backStack.removeAt(backStack.size - 1)
                        backStack.add(NavDestination.ExpenseHome)
                    },
                    onSkip = {
                        viewModel.setSkippedLogin(true)
                        // Clear backstack
                        while (backStack.isNotEmpty()) backStack.removeAt(backStack.size - 1)
                        backStack.add(NavDestination.ExpenseHome)
                    }
                )
            }
            entry<NavDestination.ExpenseHome> {
                ExpenseHomeScreen(
                    viewModel = viewModel,
                    onAddExpenseClick = {
                        backStack.add(NavDestination.AddExpense())
                    },
                    onHistoryClick = {
                        backStack.add(NavDestination.History)
                    },
                    onEditTransaction = { transaction ->
                        backStack.add(NavDestination.AddExpense(transaction))
                    },
                    onProfileClick = {
                        backStack.add(NavDestination.Profile)
                    },
                    onCategoryClick = {
                        backStack.add(NavDestination.CategoryManagement)
                    },
                    onBudgetClick = {
                        backStack.add(NavDestination.BudgetPlanner)
                    },
                    onAnalyticsClick = {
                        backStack.add(NavDestination.Analytics)
                    },
                    onNotificationsClick = {
                        backStack.add(NavDestination.Notifications)
                    },
                    onSecurityClick = {
                        backStack.add(NavDestination.Security)
                    },
                    onLoginClick = {
                        backStack.add(NavDestination.Login)
                    }
                )
            }
            entry<NavDestination.Lock> {
                LockScreen(
                    onAuthenticated = {
                        onUnlock()
                        while (backStack.isNotEmpty()) backStack.removeAt(backStack.size - 1)
                        backStack.add(NavDestination.ExpenseHome)
                    }
                )
            }
            entry<NavDestination.Profile> {
                val context = LocalContext.current
                ProfileScreen(
                    onBack = {
                        if (backStack.isNotEmpty()) {
                            backStack.removeAt(backStack.size - 1)
                        }
                    },
                    onFeedbackClick = {
                        backStack.add(NavDestination.Feedback)
                    },
                    onLogout = {
                        viewModel.logout {
                            FirebaseAuth.getInstance().signOut()
                            // Cancel all scheduled notifications and reset
                            NotificationScheduler.cancelAllNotifications(context)
                            // Clear backstack and go to login
                            while (backStack.isNotEmpty()) backStack.removeAt(backStack.size - 1)
                            backStack.add(NavDestination.Login)
                        }
                    }
                )
            }
            entry<NavDestination.CategoryManagement> {
                CategoryManagementScreen(
                    viewModel = viewModel,
                    onBack = {
                        if (backStack.isNotEmpty()) {
                            backStack.removeAt(backStack.size - 1)
                        }
                    }
                )
            }
            entry<NavDestination.BudgetPlanner> {
                BudgetPlannerScreen(
                    viewModel = viewModel,
                    onBack = {
                        if (backStack.isNotEmpty()) {
                            backStack.removeAt(backStack.size - 1)
                        }
                    }
                )
            }
            entry<NavDestination.Analytics> {
                val analyticsViewModel: AnalyticsViewModel = viewModel()
                AnalyticsScreen(
                    viewModel = analyticsViewModel,
                    onBack = {
                        if (backStack.isNotEmpty()) {
                            backStack.removeAt(backStack.size - 1)
                        }
                    }
                )
            }
            entry<NavDestination.Notifications> {
                NotificationsScreen(
                    viewModel = viewModel,
                    onBack = {
                        if (backStack.isNotEmpty()) {
                            backStack.removeAt(backStack.size - 1)
                        }
                    }
                )
            }
            entry<NavDestination.Security> {
                SecurityScreen(
                    viewModel = viewModel,
                    onBack = {
                        if (backStack.isNotEmpty()) {
                            backStack.removeAt(backStack.size - 1)
                        }
                    }
                )
            }
            entry<NavDestination.Feedback> {
                val feedbackViewModel: FeedbackViewModel = viewModel()
                FeedbackScreen(
                    viewModel = feedbackViewModel,
                    onBack = {
                        if (backStack.isNotEmpty()) {
                            backStack.removeAt(backStack.size - 1)
                        }
                    }
                )
            }
            entry<NavDestination.AddExpense> { destination ->
                AddExpenseScreen(
                    viewModel = viewModel,
                    transaction = destination.transaction,
                    onBack = {
                        if (backStack.isNotEmpty()) {
                            backStack.removeAt(backStack.size - 1)
                        }
                    }
                )
            }
            entry<NavDestination.History> {
                HistoryScreen(
                    viewModel = viewModel,
                    onBack = {
                        if (backStack.isNotEmpty()) {
                            backStack.removeAt(backStack.size - 1)
                        }
                    },
                    onEditTransaction = { transaction ->
                        backStack.add(NavDestination.AddExpense(transaction))
                    }
                )
            }
        }
    )
}
