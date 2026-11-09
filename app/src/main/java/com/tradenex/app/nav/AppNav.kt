package com.tradenex.app.nav

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.foundation.layout.Box
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
import com.tradenex.app.ui.screens.InsightsScreen
import com.tradenex.app.ui.screens.EducationScreen
import com.tradenex.app.ui.screens.HomeScreen
import com.tradenex.app.ui.screens.MoreScreen
import com.tradenex.app.ui.screens.PerformanceScreen
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

    TxBackground {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
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
                    composable("insights") { InsightsScreen(nav) }
                    composable("tags") { TagsScreen(nav) }
                    composable("settings") { SettingsScreen(nav) }
                }
            }
            if (route != "add") {
                TxBottomBar(current = route) { target ->
                    nav.navigate(target) {
                        launchSingleTop = true
                        popUpTo("home")
                    }
                }
            }
        }
    }
}
