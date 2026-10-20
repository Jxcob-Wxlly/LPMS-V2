package com.teampointblank.lpms.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.teampointblank.lpms.AppContainer
import com.teampointblank.lpms.ui.help.HelpScreen
import com.teampointblank.lpms.ui.home.HomeScreen
import com.teampointblank.lpms.ui.home.HomeViewModel
import com.teampointblank.lpms.ui.learners.LearnerDetailScreen
import com.teampointblank.lpms.ui.learners.LearnerDetailViewModel
import com.teampointblank.lpms.ui.learners.LearnersScreen
import com.teampointblank.lpms.ui.learners.LearnersViewModel
import com.teampointblank.lpms.ui.login.LoginScreen
import com.teampointblank.lpms.ui.login.LoginViewModel
import com.teampointblank.lpms.ui.login.RegisterScreen
import com.teampointblank.lpms.ui.login.RegisterViewModel
import com.teampointblank.lpms.ui.lpmsViewModel
import com.teampointblank.lpms.ui.parent.ChildReportScreen
import com.teampointblank.lpms.ui.parent.ChildReportViewModel
import com.teampointblank.lpms.ui.parent.ParentHomeScreen
import com.teampointblank.lpms.ui.parent.ParentHomeViewModel
import com.teampointblank.lpms.ui.sessions.AttendanceScreen
import com.teampointblank.lpms.ui.sessions.AttendanceViewModel
import com.teampointblank.lpms.ui.sessions.NewSessionScreen
import com.teampointblank.lpms.ui.sessions.NewSessionViewModel
import com.teampointblank.lpms.ui.sessions.SessionsScreen
import com.teampointblank.lpms.ui.sessions.SessionsViewModel
import com.teampointblank.lpms.ui.settings.ChangePasswordScreen
import com.teampointblank.lpms.ui.settings.ChangePasswordViewModel
import com.teampointblank.lpms.ui.settings.SettingsScreen
import com.teampointblank.lpms.ui.settings.SettingsViewModel

@Composable
fun LpmsNavHost(container: AppContainer) {
    val navController: NavHostController = rememberNavController()
    val auth by container.auth.state.collectAsStateWithLifecycle()
    val startDestination = remember {
        if (container.auth.state.value.isSignedIn) Routes.homeFor(container.auth.state.value.isParent) else Routes.LOGIN
    }

    // Signing in moves to Home; signing out (or an expired session) always returns to the sign-in screen.
    LaunchedEffect(auth.isSignedIn) {
        val current = navController.currentDestination?.route
        if (auth.isSignedIn) {
            if (current == Routes.LOGIN || current == Routes.REGISTER) {
                navController.navigate(Routes.homeFor(auth.isParent)) {
                    popUpTo(Routes.LOGIN) { inclusive = true }
                    launchSingleTop = true
                }
            }
        } else if (current != null && current != Routes.LOGIN && current != Routes.REGISTER && current != Routes.HELP) {
            navController.navigate(Routes.LOGIN) {
                popUpTo(navController.graph.id) { inclusive = true }
                launchSingleTop = true
            }
        }
    }

    val navigateToTab: (String) -> Unit = { route ->
        navController.navigate(route) {
            popUpTo(Routes.homeFor(container.auth.state.value.isParent)) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    NavHost(navController = navController, startDestination = startDestination) {

        composable(Routes.LOGIN) {
            val loginViewModel = lpmsViewModel { LoginViewModel(container.repository, container.settings, container.auth) }
            val settingsViewModel = lpmsViewModel { SettingsViewModel(container.settings, container.repository, container.auth) }
            LoginScreen(
                viewModel = loginViewModel,
                settingsViewModel = settingsViewModel,
                onOpenHelp = { navController.navigate(Routes.HELP) },
                onCreateAccount = { navController.navigate(Routes.REGISTER) }
            )
        }

        composable(Routes.REGISTER) {
            val viewModel = lpmsViewModel { RegisterViewModel(container.repository) }
            RegisterScreen(viewModel = viewModel, onSignIn = { navController.popBackStack() })
        }

        composable(Routes.NEW_SESSION) {
            val viewModel = lpmsViewModel { NewSessionViewModel(container.repository) }
            NewSessionScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
        }

        composable(Routes.HOME) {
            val viewModel = lpmsViewModel { HomeViewModel(container.repository) }
            HomeScreen(
                viewModel = viewModel,
                onNavigate = navigateToTab,
                onOpenSession = { id -> navController.navigate(Routes.session(id)) },
                onNewSession = { navController.navigate(Routes.NEW_SESSION) }
            )
        }

        composable(Routes.PARENT_HOME) {
            val viewModel = lpmsViewModel { ParentHomeViewModel(container.repository, container.settings) }
            ParentHomeScreen(
                viewModel = viewModel,
                onNavigate = navigateToTab,
                onOpenChild = { id -> navController.navigate(Routes.child(id)) }
            )
        }

        composable(
            route = Routes.CHILD,
            arguments = listOf(navArgument(Routes.ARG_LEARNER_ID) { type = NavType.IntType })
        ) { entry ->
            val learnerId = entry.arguments?.getInt(Routes.ARG_LEARNER_ID) ?: 0
            val viewModel = lpmsViewModel { ChildReportViewModel(container.repository, learnerId, container.settings) }
            ChildReportScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
        }

        composable(Routes.SESSIONS) {
            val viewModel = lpmsViewModel { SessionsViewModel(container.repository) }
            SessionsScreen(
                viewModel = viewModel,
                onNavigate = navigateToTab,
                onOpenSession = { id -> navController.navigate(Routes.session(id)) },
                onNewSession = { navController.navigate(Routes.NEW_SESSION) }
            )
        }

        composable(
            route = Routes.SESSION,
            arguments = listOf(navArgument(Routes.ARG_SESSION_ID) { type = NavType.IntType })
        ) { entry ->
            val sessionId = entry.arguments?.getInt(Routes.ARG_SESSION_ID) ?: 0
            val viewModel = lpmsViewModel { AttendanceViewModel(container.repository, sessionId) }
            AttendanceScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
        }

        composable(Routes.LEARNERS) {
            val viewModel = lpmsViewModel { LearnersViewModel(container.repository) }
            LearnersScreen(
                viewModel = viewModel,
                onNavigate = navigateToTab,
                onOpenLearner = { id -> navController.navigate(Routes.learner(id)) }
            )
        }

        composable(
            route = Routes.LEARNER,
            arguments = listOf(navArgument(Routes.ARG_LEARNER_ID) { type = NavType.IntType })
        ) { entry ->
            val learnerId = entry.arguments?.getInt(Routes.ARG_LEARNER_ID) ?: 0
            val viewModel = lpmsViewModel { LearnerDetailViewModel(container.repository, learnerId) }
            LearnerDetailScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
        }

        composable(Routes.SETTINGS) {
            val viewModel = lpmsViewModel { SettingsViewModel(container.settings, container.repository, container.auth) }
            SettingsScreen(
                viewModel = viewModel,
                onNavigate = navigateToTab,
                onChangePassword = { navController.navigate(Routes.CHANGE_PASSWORD) },
                onOpenHelp = { navController.navigate(Routes.HELP) }
            )
        }

        composable(Routes.CHANGE_PASSWORD) {
            val viewModel = lpmsViewModel { ChangePasswordViewModel(container.repository) }
            ChangePasswordScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
        }

        composable(Routes.HELP) {
            HelpScreen(onBack = { navController.popBackStack() })
        }
    }
}
