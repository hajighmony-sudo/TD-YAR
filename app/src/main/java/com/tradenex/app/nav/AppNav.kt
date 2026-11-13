package com.tradenex.app.nav

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.tradenex.app.TxAiFlutterLaunch
import com.tradenex.app.ui.components.TxBackground
import com.tradenex.app.ui.components.TxBottomBar
import com.tradenex.app.ui.screens.AccountsScreen
import com.tradenex.app.ui.screens.AddTradeScreen
import com.tradenex.app.ui.screens.CalendarScreen
import com.tradenex.app.ui.screens.CalculatorScreen
import com.tradenex.app.ui.screens.RiskLabScreen
import com.tradenex.app.ui.screens.MetaTraderScreen
import com.tradenex.app.ui.screens.TvShareScreen
import com.tradenex.app.ui.screens.InsightsScreen
import com.tradenex.app.ui.screens.EducationScreen
import com.tradenex.app.ui.screens.HomeScreen
import com.tradenex.app.ui.screens.MoreScreen
import com.tradenex.app.ui.screens.NotificationCenterScreen
import com.tradenex.app.ui.screens.NotificationSettingsScreen
import com.tradenex.app.ui.screens.PerformanceScreen
import com.tradenex.app.ui.screens.PositionsScreen
import com.tradenex.app.ui.screens.PsychologyScreen
import com.tradenex.app.ui.screens.SessionsScreen
import com.tradenex.app.ui.screens.SettingsScreen
import com.tradenex.app.ui.screens.StrategiesScreen
import com.tradenex.app.ui.screens.TagsScreen
import com.tradenex.app.ui.screens.TradesScreen
import com.tradenex.app.ui.screens.TxAiScreen
import com.tradenex.app.ui.screens.WeeklyPlanScreen

@Composable
fun AppNav(startDestination: String = "home") {
    val nav = rememberNavController()
    val entry by nav.currentBackStackEntryAsState()
    val route = entry?.destination?.route ?: "home"

    var drawerOpen by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    TxBackground {
      Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            Row(Modifier.fillMaxWidth().height(44.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                androidx.compose.material3.IconButton(onClick = { drawerOpen = !drawerOpen }) {
                    com.tradenex.app.ui.components.HamburgerIcon(drawerOpen)
                }
                IconButton(onClick = { nav.navigate("notifications") { launchSingleTop = true } }) {
                    Icon(Icons.Outlined.Notifications, contentDescription = "مرکز اعلان‌ها")
                }
            }
            Box(Modifier.weight(1f)) {
                NavHost(
                    navController = nav,
                    startDestination = startDestination,
                    enterTransition = { fadeIn(tween(320)) + slideInHorizontally(tween(320)) { it / 10 } },
                    exitTransition = { fadeOut(tween(200)) },
                    popEnterTransition = { fadeIn(tween(320)) },
                    popExitTransition = { fadeOut(tween(200)) },
                ) {
                    composable("home") { HomeScreen(nav) }
                    composable("accounts") { AccountsScreen(nav) }
                    composable("trades") { TradesScreen(nav) }
                    composable("positions") { PositionsScreen(nav) }
                    composable("weekly") { WeeklyPlanScreen(nav) }
                    composable("weekly_reminder") { WeeklyPlanScreen(nav, openToday = true) }
                    composable("perf") { PerformanceScreen(nav) }
                    composable("tx_ai") {
                        val context = LocalContext.current
                        LaunchedEffect(Unit) {
                            val opened = TxAiFlutterLaunch.open(context)
                            if (opened) {
                                nav.popBackStack()
                            } else {
                                nav.navigate("tx_ai_legacy") {
                                    popUpTo("tx_ai") { inclusive = true }
                                }
                            }
                        }
                    }
                    composable("tx_ai_legacy") { TxAiScreen(nav) }
                    composable("edu") { EducationScreen(nav) }
                    composable("more") { MoreScreen(nav) }
                    composable("add") { AddTradeScreen(nav) }
                    composable("sessions") { SessionsScreen(nav) }
                    composable("strategies") { StrategiesScreen(nav) }
                    composable("calendar") { CalendarScreen(nav) }
                    composable("calculator") { CalculatorScreen(nav) }
                    composable("risk_lab") { RiskLabScreen(nav) }
                    composable("metatrader") { MetaTraderScreen(nav) }
                    composable("tv_share") { TvShareScreen(nav) }
                    composable("insights") { InsightsScreen(nav) }
                    composable("psychology") { PsychologyScreen(nav) }
                    composable("tags") { TagsScreen(nav) }
                    composable("settings") { SettingsScreen(nav) }
                    composable("notifications") { NotificationCenterScreen(onOpen = { target -> nav.navigate(target) { launchSingleTop = true } }, onSettings = { nav.navigate("notification_settings") }) }
                    composable("notification_settings") { NotificationSettingsScreen() }
                }
            }
            if (route != "add" && route != "notifications" && route != "notification_settings") {
                TxBottomBar(current = route) { target ->
                    nav.navigate(target) {
                        launchSingleTop = true
                        popUpTo("home")
                    }
                }
            }
        }
        com.tradenex.app.ui.components.UserDrawer(open = drawerOpen, onClose = { drawerOpen = false }) { target ->
            nav.navigate(target) { launchSingleTop = true }
        }
      }
    }
}
