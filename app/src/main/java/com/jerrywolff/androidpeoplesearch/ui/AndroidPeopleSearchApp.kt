package com.jerrywolff.androidpeoplesearch.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.PrivacyTip
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

private object Route {
    const val HOME = "home"
    const val RESULTS = "results"
    const val DETAIL = "detail"
    const val SETTINGS = "settings"
    const val SUMMARY = "summary"
    const val PRIVACY = "privacy"
    const val ABOUT = "about"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AndroidPeopleSearchApp(viewModel: PeopleSearchViewModel = viewModel()) {
    val state by viewModel.state.collectAsState()
    val navController = rememberNavController()
    var menuOpen by remember { mutableStateOf(false) }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Android People Search") },
                actions = {
                    IconButton(onClick = { menuOpen = true }) { Icon(Icons.Outlined.MoreVert, "Open navigation") }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem({ Text("Sources & settings") }, { menuOpen = false; navController.navigate(Route.SETTINGS) }, leadingIcon = { Icon(Icons.Outlined.Settings, null) })
                        DropdownMenuItem({ Text("Privacy & safety") }, { menuOpen = false; navController.navigate(Route.PRIVACY) }, leadingIcon = { Icon(Icons.Outlined.PrivacyTip, null) })
                        DropdownMenuItem({ Text("About") }, { menuOpen = false; navController.navigate(Route.ABOUT) }, leadingIcon = { Icon(Icons.Outlined.Info, null) })
                    }
                },
            )
        },
    ) { padding ->
        NavHost(navController, Route.HOME, Modifier.fillMaxSize().padding(padding)) {
            composable(Route.HOME) { HomeScreen(state, viewModel::updateQuery, viewModel::clearQuery, { viewModel.search { navController.navigate(Route.RESULTS) } }, { navController.navigate(Route.SETTINGS) }) }
            composable(Route.RESULTS) { ResultsScreen(state, onBack = { navController.popBackStack() }, onSelect = { viewModel.select(it); navController.navigate(Route.DETAIL) }, onSummarize = { viewModel.summarize { navController.navigate(Route.SUMMARY) } }) }
            composable(Route.DETAIL) { state.selectedResult?.let { DetailScreen(state.query, it, viewModel::explain, navController::popBackStack) } }
            composable(Route.SETTINGS) { SettingsScreen(state, viewModel::setCategory, viewModel::setHistory, viewModel::setPrivacyMode, viewModel::clearLocalData, navController::popBackStack) }
            composable(Route.SUMMARY) { SummaryScreen(state.summary, navController::popBackStack) }
            composable(Route.PRIVACY) { PrivacyScreen(navController::popBackStack) }
            composable(Route.ABOUT) { AboutScreen(navController::popBackStack) }
        }
    }
}
