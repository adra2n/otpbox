package com.otpbox.ui.nav

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.otpbox.ui.add.AddScreen
import com.otpbox.ui.detail.DetailScreen
import com.otpbox.ui.home.HomeScreen
import com.otpbox.ui.importer.ImportScreen
import com.otpbox.ui.password.PasswordDetailScreen
import com.otpbox.ui.password.PasswordListScreen
import com.otpbox.ui.scan.ScanScreen
import com.otpbox.ui.settings.SettingsScreen

@Composable
fun OtpNavHost(navController: NavHostController = rememberNavController()) {
    NavHost(navController = navController, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            HomeScreen(
                onAddManual = { navController.navigate(Routes.ADD) },
                onScan = { navController.navigate(Routes.SCAN) },
                onImport = { navController.navigate(Routes.IMPORT) },
                onSettings = { navController.navigate(Routes.SETTINGS) },
                onOpenDetail = { id -> navController.navigate(Routes.detail(id)) }
            )
        }
        composable(Routes.SCAN) {
            ScanScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.ADD) {
            AddScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.IMPORT) {
            ImportScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(onBack = { navController.popBackStack() })
        }
        composable(
            Routes.DETAIL,
            arguments = listOf(navArgument("id") { type = NavType.StringType })
        ) { backStackEntry ->
            val id = backStackEntry.arguments?.getString("id").orEmpty()
            DetailScreen(id = id, onBack = { navController.popBackStack() })
        }
        composable(Routes.PASSWORDS) {
            PasswordListScreen(
                onAdd = { navController.navigate(Routes.passwordDetail(null)) },
                onSettings = { navController.navigate(Routes.SETTINGS) },
                onOpenDetail = { id -> navController.navigate(Routes.passwordDetail(id)) }
            )
        }
        composable(
            Routes.PASSWORD_DETAIL,
            arguments = listOf(navArgument("id") {
                type = NavType.StringType
                nullable = true
                defaultValue = null
            })
        ) { backStackEntry ->
            val id = backStackEntry.arguments?.getString("id")?.takeIf { it != "new" }
            PasswordDetailScreen(id = id, onBack = { navController.popBackStack() })
        }
    }
}
