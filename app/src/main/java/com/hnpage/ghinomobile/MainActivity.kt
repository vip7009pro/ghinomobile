package com.hnpage.ghinomobile

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.hnpage.ghinomobile.screen.ContactDetailScreen
import com.hnpage.ghinomobile.screen.ContactListScreen
import com.hnpage.ghinomobile.screen.OverviewScreen
import com.hnpage.ghinomobile.screen.TransactionHistoryScreen
import com.hnpage.ghinomobile.ui.theme.GhinomobileTheme
import com.hnpage.ghinomobile.viewmodel.DebtViewModel

class MainActivity : ComponentActivity() {
    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestPermissionLauncher.launch(
            arrayOf(
                android.Manifest.permission.READ_CONTACTS,
                android.Manifest.permission.POST_NOTIFICATIONS
            )
        )
        enableEdgeToEdge()
        val viewModel = DebtViewModel(application)
        setContent {
            GhinomobileTheme { // Áp dụng theme
                DebtApp(viewModel)
            }
        }
    }
}

@Composable
fun DebtApp(viewModel: DebtViewModel) {
    val navController = rememberNavController()
    Scaffold(
        bottomBar = { BottomNavigationBar(navController) }
    ) { padding ->
        NavHost(navController, startDestination = "overview", Modifier.padding(padding)) {
            composable(
                route = "overview",
                enterTransition = {
                    slideInHorizontally(initialOffsetX = { 1000 }) + fadeIn(animationSpec = tween(500))
                },
                exitTransition = {
                    slideOutHorizontally(targetOffsetX = { -1000 }) + fadeOut(animationSpec = tween(500))
                },
                popEnterTransition = {
                    slideInHorizontally(initialOffsetX = { -1000 }) + fadeIn(animationSpec = tween(500))
                },
                popExitTransition = {
                    slideOutHorizontally(targetOffsetX = { 1000 }) + fadeOut(animationSpec = tween(500))
                }
            ) { OverviewScreen(viewModel) }
            composable(
                route = "history",
                enterTransition = {
                    slideInHorizontally(initialOffsetX = { 1000 }) + fadeIn(animationSpec = tween(500))
                },
                exitTransition = {
                    slideOutHorizontally(targetOffsetX = { -1000 }) + fadeOut(animationSpec = tween(500))
                },
                popEnterTransition = {
                    slideInHorizontally(initialOffsetX = { -1000 }) + fadeIn(animationSpec = tween(500))
                },
                popExitTransition = {
                    slideOutHorizontally(targetOffsetX = { 1000 }) + fadeOut(animationSpec = tween(500))
                }
            ) { TransactionHistoryScreen(viewModel) }
            composable(
                route = "contacts",
                enterTransition = {
                    slideInHorizontally(initialOffsetX = { 1000 }) + fadeIn(animationSpec = tween(500))
                },
                exitTransition = {
                    slideOutHorizontally(targetOffsetX = { -1000 }) + fadeOut(animationSpec = tween(500))
                },
                popEnterTransition = {
                    slideInHorizontally(initialOffsetX = { -1000 }) + fadeIn(animationSpec = tween(500))
                },
                popExitTransition = {
                    slideOutHorizontally(targetOffsetX = { 1000 }) + fadeOut(animationSpec = tween(500))
                }
            ) {
                ContactListScreen(viewModel, onContactClick = { phoneNumber ->
                    navController.navigate("contactDetail/$phoneNumber")
                })
            }
            composable(
                route = "contactDetail/{phoneNumber}",
                enterTransition = {
                    slideInHorizontally(initialOffsetX = { 1000 }) + fadeIn(animationSpec = tween(500))
                },
                exitTransition = {
                    slideOutHorizontally(targetOffsetX = { -1000 }) + fadeOut(animationSpec = tween(500))
                },
                popEnterTransition = {
                    slideInHorizontally(initialOffsetX = { -1000 }) + fadeIn(animationSpec = tween(500))
                },
                popExitTransition = {
                    slideOutHorizontally(targetOffsetX = { 1000 }) + fadeOut(animationSpec = tween(500))
                }
            ) { backStackEntry ->
                val phoneNumber = backStackEntry.arguments?.getString("phoneNumber") ?: ""
                ContactDetailScreen(
                    viewModel = viewModel,
                    phoneNumber = phoneNumber,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}

@Composable
fun BottomNavigationBar(navController: NavHostController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        NavigationBarItem(
            icon = { Icon(Icons.Default.Home, contentDescription = null) },
            label = { Text("Tổng quan") },
            selected = currentRoute == "overview",
            onClick = { navController.navigate("overview") { popUpTo("overview") { inclusive = true } } }
        )
        NavigationBarItem(
            icon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = null) },
            label = { Text("Lịch sử") },
            selected = currentRoute == "history",
            onClick = { navController.navigate("history") { popUpTo("overview") { inclusive = false } } }
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.Contacts, contentDescription = null) },
            label = { Text("Danh bạ") },
            selected = currentRoute == "contacts",
            onClick = { navController.navigate("contacts") { popUpTo("overview") { inclusive = false } } }
        )
    }
}