package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Route
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.ui.screens.AiStoriesScreen
import com.example.ui.screens.ConvoyMembersScreen
import com.example.ui.screens.ConvoyNavScreen
import com.example.ui.screens.RouteDetailsScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.PrimaryNeon
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate850
import com.example.ui.theme.Slate900
import com.example.util.configureAppCheck
import com.example.viewmodel.ConvoyUiState
import com.example.viewmodel.ConvoyViewModel
import com.example.viewmodel.ConvoyViewModelFactory
import com.example.viewmodel.NavScreen

class MainActivity : ComponentActivity() {

    private val viewModel: ConvoyViewModel by viewModels {
        ConvoyViewModelFactory(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // STEP 1: Mandatory Firebase App Check configuration before UI or Firebase operations
        configureAppCheck(this, intent)

        enableEdgeToEdge()

        setContent {
            MyApplicationTheme(darkTheme = true) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Slate900
                ) {
                    val uiState by viewModel.uiState.collectAsState()
                    val isSpeaking by viewModel.isSpeaking.collectAsState()
                    val speechRate by viewModel.speechRate.collectAsState()

                    ConvoyAppRoot(
                        uiState = uiState,
                        isSpeaking = isSpeaking,
                        speechRate = speechRate,
                        viewModel = viewModel
                    )
                }
            }
        }
    }
}

@Composable
fun ConvoyAppRoot(
    uiState: ConvoyUiState,
    isSpeaking: Boolean,
    speechRate: Float,
    viewModel: ConvoyViewModel
) {
    // Handle Android system back button: return to Map screen when in sub-screens
    if (uiState.currentScreen != NavScreen.MAP) {
        BackHandler {
            viewModel.setScreen(NavScreen.MAP)
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            ConvoyBottomNavBar(
                currentScreen = uiState.currentScreen,
                onScreenSelected = { screen -> viewModel.setScreen(screen) }
            )
        }
    ) { innerPadding ->
        BoxModifierContent(
            uiState = uiState,
            isSpeaking = isSpeaking,
            speechRate = speechRate,
            viewModel = viewModel,
            modifier = Modifier.padding(innerPadding)
        )
    }
}

@Composable
private fun BoxModifierContent(
    uiState: ConvoyUiState,
    isSpeaking: Boolean,
    speechRate: Float,
    viewModel: ConvoyViewModel,
    modifier: Modifier = Modifier
) {
    when (uiState.currentScreen) {
        NavScreen.MAP -> {
            ConvoyNavScreen(
                uiState = uiState,
                isSpeaking = isSpeaking,
                speechRate = speechRate,
                viewModel = viewModel,
                modifier = modifier
            )
        }
        NavScreen.CONVOY_ROSTER -> {
            ConvoyMembersScreen(
                uiState = uiState,
                viewModel = viewModel,
                modifier = modifier
            )
        }
        NavScreen.AI_STORIES -> {
            AiStoriesScreen(
                uiState = uiState,
                speechRate = speechRate,
                isSpeaking = isSpeaking,
                viewModel = viewModel,
                modifier = modifier
            )
        }
        NavScreen.ROUTE_DETAILS -> {
            RouteDetailsScreen(
                uiState = uiState,
                viewModel = viewModel,
                modifier = modifier
            )
        }
    }
}

@Composable
fun ConvoyBottomNavBar(
    currentScreen: NavScreen,
    onScreenSelected: (NavScreen) -> Unit,
    modifier: Modifier = Modifier
) {
    NavigationBar(
        modifier = modifier.testTag("convoy_bottom_nav_bar"),
        containerColor = Slate850,
        tonalElevation = 8.dp
    ) {
        NavigationBarItem(
            selected = currentScreen == NavScreen.MAP,
            onClick = { onScreenSelected(NavScreen.MAP) },
            icon = {
                Icon(
                    imageVector = Icons.Default.Map,
                    contentDescription = "Map Screen"
                )
            },
            label = { Text("แผนที่นำทาง") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = PrimaryNeon,
                selectedTextColor = PrimaryNeon,
                indicatorColor = Slate800,
                unselectedIconColor = Slate400,
                unselectedTextColor = Slate400
            ),
            modifier = Modifier.testTag("nav_item_map")
        )

        NavigationBarItem(
            selected = currentScreen == NavScreen.CONVOY_ROSTER,
            onClick = { onScreenSelected(NavScreen.CONVOY_ROSTER) },
            icon = {
                Icon(
                    imageVector = Icons.Default.DirectionsCar,
                    contentDescription = "Convoy Roster Screen"
                )
            },
            label = { Text("ขบวนคาราวาน") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = PrimaryNeon,
                selectedTextColor = PrimaryNeon,
                indicatorColor = Slate800,
                unselectedIconColor = Slate400,
                unselectedTextColor = Slate400
            ),
            modifier = Modifier.testTag("nav_item_convoy")
        )

        NavigationBarItem(
            selected = currentScreen == NavScreen.AI_STORIES,
            onClick = { onScreenSelected(NavScreen.AI_STORIES) },
            icon = {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = "AI Stories Screen"
                )
            },
            label = { Text("ไกด์ AI บรรยาย") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = PrimaryNeon,
                selectedTextColor = PrimaryNeon,
                indicatorColor = Slate800,
                unselectedIconColor = Slate400,
                unselectedTextColor = Slate400
            ),
            modifier = Modifier.testTag("nav_item_ai_stories")
        )

        NavigationBarItem(
            selected = currentScreen == NavScreen.ROUTE_DETAILS,
            onClick = { onScreenSelected(NavScreen.ROUTE_DETAILS) },
            icon = {
                Icon(
                    imageVector = Icons.Default.Route,
                    contentDescription = "Route Details Screen"
                )
            },
            label = { Text("เส้นทาง & แวะ") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = PrimaryNeon,
                selectedTextColor = PrimaryNeon,
                indicatorColor = Slate800,
                unselectedIconColor = Slate400,
                unselectedTextColor = Slate400
            ),
            modifier = Modifier.testTag("nav_item_route")
        )
    }
}
