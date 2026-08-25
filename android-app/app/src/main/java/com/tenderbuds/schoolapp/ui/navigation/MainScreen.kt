package com.tenderbuds.schoolapp.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavHostController
import com.tenderbuds.schoolapp.ui.routes.DashboardRoute
import com.tenderbuds.schoolapp.ui.routes.StudentsRoute
import com.tenderbuds.schoolapp.ui.screens.ComingSoonScreen
import com.tenderbuds.schoolapp.ui.theme.BrandIndigo

private enum class MainTab(val label: String, val icon: ImageVector) {
    DASHBOARD("Dashboard", Icons.Filled.Dashboard),
    STUDENTS("Students", Icons.Filled.Groups),
    FEES("Fees", Icons.Filled.Payments),
    STOCK("Stock", Icons.Filled.Inventory2)
}

/**
 * The persistent tab bar (Dashboard | Student Records | Fee Management |
 * Stock Records) from the original web app's top nav, rebuilt as a bottom
 * navigation bar — the standard mobile pattern for a small set of sibling
 * top-level sections.
 */
@Composable
fun MainScreen(rootNavController: NavHostController) {
    var selectedTab by rememberSaveable { mutableStateOf(MainTab.DASHBOARD) }

    Scaffold(
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                MainTab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        icon = { Icon(tab.icon, contentDescription = tab.label) },
                        label = { Text(tab.label) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = BrandIndigo,
                            selectedTextColor = BrandIndigo,
                            indicatorColor = BrandIndigo.copy(alpha = 0.12f)
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            when (selectedTab) {
                MainTab.DASHBOARD -> DashboardRoute(rootNavController)
                MainTab.STUDENTS -> StudentsRoute(rootNavController)
                MainTab.FEES -> ComingSoonScreen("Fee Management")
                MainTab.STOCK -> ComingSoonScreen("Stock Records")
            }
        }
    }
}
