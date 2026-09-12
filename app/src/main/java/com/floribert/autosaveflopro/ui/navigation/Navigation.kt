package com.floribert.autosaveflopro.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.floribert.autosaveflopro.ui.screens.home.HomeScreen
import com.floribert.autosaveflopro.ui.screens.history.HistoryScreen
import com.floribert.autosaveflopro.ui.screens.saved.SavedContactsScreen
import com.floribert.autosaveflopro.ui.screens.permission.PermissionScreen
import com.floribert.autosaveflopro.ui.screens.settings.SettingsScreen
import com.floribert.autosaveflopro.ui.screens.subscription.SubscriptionScreen
import com.floribert.autosaveflopro.ui.screens.admin.AdminDashboard
import com.floribert.autosaveflopro.ui.screens.status.StatusDownloaderScreen
import com.floribert.autosaveflopro.ui.screens.downloader.VideoDownloaderScreen
import com.floribert.autosaveflopro.ui.screens.tools.BulkSmsScreen
import com.floribert.autosaveflopro.ui.screens.tools.QrGeneratorScreen
import com.floribert.autosaveflopro.ui.screens.magic.WhatsAppDirectScreen
import com.floribert.autosaveflopro.ui.screens.magic.BusinessCardScreen
import com.floribert.autosaveflopro.ui.screens.guide.GuideScreen

sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object History : Screen("history")
    data object SavedContacts : Screen("saved_contacts")
    data object Permissions : Screen("permissions")
    data object Settings : Screen("settings")
    data object Subscription : Screen("subscription")
    data object Admin : Screen("admin")
    data object StatusDownloader : Screen("status_downloader")
    data object VideoDownloader : Screen("video_downloader")
    data object BulkSms : Screen("bulk_sms")
    data object QrGenerator : Screen("qr_generator")
    data object WhatsAppDirect : Screen("whatsapp_direct")
    data object BusinessCard : Screen("business_card")
    data object Guide : Screen("guide")
}

@Composable
fun AppNavigation(navController: NavHostController) {
    NavHost(navController, startDestination = Screen.Home.route) {
        composable(Screen.Home.route) {
            HomeScreen(
                onNavigateToPermissions = { navController.navigate(Screen.Permissions.route) },
                onNavigateToHistory = { navController.navigate(Screen.History.route) },
                onNavigateToSavedContacts = { navController.navigate(Screen.SavedContacts.route) },
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                onNavigateToSubscription = { navController.navigate(Screen.Subscription.route) },
                onNavigateToStatus = { navController.navigate(Screen.StatusDownloader.route) },
                onNavigateToVideoDownloader = { navController.navigate(Screen.VideoDownloader.route) },
                onNavigateToBulkSms = { navController.navigate(Screen.BulkSms.route) },
                onNavigateToQr = { navController.navigate(Screen.QrGenerator.route) },
                onNavigateToDirectChat = { navController.navigate(Screen.WhatsAppDirect.route) },
                onNavigateToBio = { navController.navigate(Screen.BusinessCard.route) },
                onNavigateToGuide = { navController.navigate(Screen.Guide.route) }
            )
        }
        composable(Screen.History.route) { HistoryScreen(onBackClicked = { navController.popBackStack() }) }
        composable(Screen.SavedContacts.route) { SavedContactsScreen(onBackClicked = { navController.popBackStack() }) }
        composable(Screen.Permissions.route) { PermissionScreen(onBackClicked = { navController.popBackStack() }) }
        composable(Screen.Settings.route) {
            SettingsScreen(
                onBackClicked = { navController.popBackStack() },
                onNavigateToPermissions = { navController.navigate(Screen.Permissions.route) },
                onNavigateToAdmin = { navController.navigate(Screen.Admin.route) }
            )
        }
        composable(Screen.Subscription.route) { SubscriptionScreen(onBackClicked = { navController.popBackStack() }) }
        composable(Screen.Admin.route) { AdminDashboard(onBackClicked = { navController.popBackStack() }) }
        composable(Screen.StatusDownloader.route) { StatusDownloaderScreen(onBackClicked = { navController.popBackStack() }) }
        composable(Screen.VideoDownloader.route) { VideoDownloaderScreen(onBackClicked = { navController.popBackStack() }) }
        composable(Screen.BulkSms.route) { BulkSmsScreen(onBackClicked = { navController.popBackStack() }) }
        composable(Screen.QrGenerator.route) { QrGeneratorScreen(onBackClicked = { navController.popBackStack() }) }
        composable(Screen.WhatsAppDirect.route) { WhatsAppDirectScreen(onBackClicked = { navController.popBackStack() }) }
        composable(Screen.BusinessCard.route) { BusinessCardScreen(onBackClicked = { navController.popBackStack() }) }
        composable(Screen.Guide.route) { GuideScreen(onBackClicked = { navController.popBackStack() }) }
    }
}
