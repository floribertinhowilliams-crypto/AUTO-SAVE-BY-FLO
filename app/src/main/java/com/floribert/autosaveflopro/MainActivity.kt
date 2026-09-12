package com.floribert.autosaveflopro

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.floribert.autosaveflopro.data.repository.LicenseRepository
import com.floribert.autosaveflopro.ui.navigation.AppNavigation
import com.floribert.autosaveflopro.ui.navigation.Screen
import com.floribert.autosaveflopro.ui.screens.lock.LockScreen
import com.floribert.autosaveflopro.ui.theme.AutoSaveContactsProTheme
import com.floribert.autosaveflopro.utils.Constants
import com.floribert.autosaveflopro.utils.UpdateManager
import com.floribert.autosaveflopro.data.model.UpdateInfo
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var licenseRepository: LicenseRepository
    @Inject lateinit var updateManager: UpdateManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            var updateInfo by remember { mutableStateOf<UpdateInfo?>(null) }
            
            // Check for updates on startup
            LaunchedEffect(Unit) {
                updateInfo = updateManager.checkForUpdate(Constants.UPDATE_JSON_URL)
            }

            val themeMode = licenseRepository.getThemeMode()
            val isDark = when(themeMode) {
                1 -> false
                2 -> true
                else -> androidx.compose.foundation.isSystemInDarkTheme()
            }
            AutoSaveContactsProTheme(darkTheme = isDark) {
                var isUnlocked by remember { 
                    mutableStateOf(!licenseRepository.isAppLockEnabled() || licenseRepository.getAppPin().isEmpty()) 
                }

                // Show Update Dialog if update is found
                updateInfo?.let { info ->
                    AlertDialog(
                        onDismissRequest = { updateInfo = null },
                        title = { Text("New Update Available! (v${info.versionName})") },
                        text = { 
                            Column {
                                Text("A new version of AutoSave Pro is available. Would you like to download it now?")
                                Spacer(Modifier.height(8.dp))
                                Text("What's New:", fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                                Text(info.changeLog)
                            }
                        },
                        confirmButton = {
                            Button(onClick = {
                                updateManager.downloadApk(info.apkUrl)
                                updateInfo = null
                            }) {
                                Text("Download & Update")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { updateInfo = null }) {
                                Text("Later")
                            }
                        }
                    )
                }

                if (isUnlocked) {
                    val navController = rememberNavController()
                    val navBackStackEntry by navController.currentBackStackEntryAsState()
                    val currentDestination = navBackStackEntry?.destination

                    // Fix: Handle Back Button to always return to Dashboard
                    BackHandler(enabled = currentDestination?.route != Screen.Home.route) {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }

                    Scaffold(
                        bottomBar = {
                            val navBackStackEntry by navController.currentBackStackEntryAsState()
                            val currentDestination = navBackStackEntry?.destination
                            
                            val items = listOf(
                                Screen.Home,
                                Screen.History,
                                Screen.SavedContacts,
                                Screen.Settings
                            )
                            
                            NavigationBar {
                                items.forEach { screen ->
                                    val isSelected = currentDestination?.hierarchy?.any { it.route == screen.route } == true
                                    NavigationBarItem(
                                        icon = { 
                                            Icon(
                                                when(screen) {
                                                    Screen.Home -> Icons.Default.Home
                                                    Screen.History -> Icons.Default.History
                                                    Screen.SavedContacts -> Icons.Default.Person
                                                    else -> Icons.Default.Settings
                                                }, 
                                                contentDescription = null
                                            ) 
                                        },
                                        label = { 
                                            Text(
                                                when(screen) {
                                                    Screen.Home -> stringResource(R.string.nav_home)
                                                    Screen.History -> stringResource(R.string.nav_history)
                                                    Screen.SavedContacts -> stringResource(R.string.nav_saved)
                                                    else -> stringResource(R.string.nav_settings)
                                                }
                                            ) 
                                        },
                                        selected = isSelected,
                                        onClick = {
                                            navController.navigate(screen.route) {
                                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    ) { innerPadding ->
                        Surface(
                            modifier = Modifier.fillMaxSize().padding(innerPadding),
                            color = MaterialTheme.colorScheme.background
                        ) {
                            AppNavigation(navController)
                        }
                    }
                } else {
                    LockScreen(onAuthenticated = { isUnlocked = true })
                }
            }
        }
    }
}
