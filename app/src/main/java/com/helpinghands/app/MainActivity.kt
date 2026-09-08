package com.helpinghands.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.helpinghands.app.data.model.AppUser
import com.helpinghands.app.data.repository.HelpingHandsRepository
import com.helpinghands.app.ui.screens.*
import com.helpinghands.app.ui.theme.HelpingHandsTheme

data class NavTab(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            HelpingHandsTheme {
                HelpingHandsAppRoot()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpingHandsAppRoot() {
    val context = LocalContext.current
    val repository = remember { HelpingHandsRepository(context) }

    var currentUser by remember { mutableStateOf<AppUser?>(null) }
    var currentRoute by remember { mutableStateOf("dashboard") }

    if (currentUser == null) {
        LoginScreen(
            onLoginSuccess = { user ->
                currentUser = user
                currentRoute = "dashboard"
            }
        )
    } else {
        val user = currentUser!!

        val navTabs = remember(user.role) {
            listOf(
                NavTab("dashboard", "होम", Icons.Filled.Dashboard, Icons.Outlined.Dashboard),
                NavTab("contributions", "अंशदान", Icons.Filled.Savings, Icons.Outlined.Savings),
                NavTab("loans", "ऋण", Icons.Filled.AccountBalance, Icons.Outlined.AccountBalance),
                NavTab("cash_book", "रोकड़ बही", Icons.Filled.AccountBalanceWallet, Icons.Outlined.AccountBalanceWallet),
                NavTab("members", "सदस्य", Icons.Filled.People, Icons.Outlined.People),
                NavTab("reports", "रिपोर्ट", Icons.Filled.Assessment, Icons.Outlined.Assessment),
                NavTab("settings", "सेटिंग्स", Icons.Filled.Settings, Icons.Outlined.Settings)
            )
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.ic_helping_hands_logo),
                                    contentDescription = "Logo",
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            Column {
                                Text(
                                    text = "THE HELPING HANDS",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "कांगहट्टी (मंदसौर) • 2026",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = { currentRoute = "settings" },
                            modifier = Modifier.testTag("nav_settings_action")
                        ) {
                            Icon(Icons.Default.Settings, contentDescription = "Settings")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        titleContentColor = MaterialTheme.colorScheme.onSurface
                    )
                )
            },
            bottomBar = {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 3.dp,
                    modifier = Modifier.testTag("bottom_nav_bar")
                ) {
                    navTabs.take(5).forEach { tab ->
                        val selected = currentRoute == tab.route
                        NavigationBarItem(
                            selected = selected,
                            onClick = { currentRoute = tab.route },
                            icon = {
                                Icon(
                                    imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                    contentDescription = tab.label
                                )
                            },
                            label = {
                                Text(
                                    text = tab.label,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            },
                            alwaysShowLabel = true,
                            modifier = Modifier.testTag("nav_tab_${tab.route}")
                        )
                    }
                }
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                when (currentRoute) {
                    "dashboard" -> DashboardScreen(
                        user = user,
                        repository = repository,
                        onNavigate = { route -> currentRoute = route }
                    )
                    "contributions" -> ContributionsScreen(
                        user = user,
                        repository = repository
                    )
                    "loans" -> LoansScreen(
                        user = user,
                        repository = repository
                    )
                    "cash_book" -> CashBookScreen(
                        user = user,
                        repository = repository
                    )
                    "members" -> MembersScreen(
                        user = user,
                        repository = repository
                    )
                    "reports" -> ReportsScreen(
                        user = user,
                        repository = repository
                    )
                    "settings" -> SettingsScreen(
                        user = user,
                        repository = repository,
                        onLogout = { currentUser = null }
                    )
                    else -> DashboardScreen(
                        user = user,
                        repository = repository,
                        onNavigate = { route -> currentRoute = route }
                    )
                }
            }
        }
    }
}
