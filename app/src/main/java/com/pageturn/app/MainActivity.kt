package com.pageturn.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.pageturn.core.designsystem.theme.PageTurnTheme
import com.pageturn.feature.library.LibraryScreen
import com.pageturn.feature.reader.ReaderScreen
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import androidx.compose.runtime.collectAsState
import com.pageturn.core.common.preferences.UserPreferencesDataSource
import com.pageturn.core.common.preferences.UserSettings
import androidx.hilt.navigation.compose.hiltViewModel

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var preferencesDataSource: UserPreferencesDataSource

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Hide system status and navigation bars to prevent covering bottom navigation bar
        androidx.core.view.WindowCompat.setDecorFitsSystemWindows(window, false)
        val insetsController = androidx.core.view.WindowCompat.getInsetsController(window, window.decorView)
        insetsController.systemBarsBehavior = androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        insetsController.hide(androidx.core.view.WindowInsetsCompat.Type.systemBars())

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 101)
        }
        setContent {
            val settingsState = preferencesDataSource.userSettings.collectAsState(
                initial = UserSettings(16, "serif", "light")
            )
            PageTurnTheme(theme = settingsState.value.readingTheme) {
                val navController = rememberNavController()
                val viewModel: com.pageturn.feature.library.LibraryViewModel = androidx.hilt.navigation.compose.hiltViewModel()
                val isUserSignedIn = viewModel.isUserSignedIn.collectAsState().value
                val startDest = if (isUserSignedIn) "library" else "auth"
                
                NavHost(
                    navController = navController,
                    startDestination = startDest,
                    modifier = Modifier.fillMaxSize()
                ) {
                    composable("auth") {
                        com.pageturn.feature.library.AuthScreen(
                            onAuthSuccess = {
                                navController.navigate("library") {
                                    popUpTo("auth") { inclusive = true }
                                }
                            }
                        )
                    }
                    composable("library") {
                        LibraryScreen(
                            onBookClick = { bookId ->
                                navController.navigate("reader/$bookId?chapter=-1&paragraph=-1")
                            },
                            onSignOut = {
                                navController.navigate("auth") {
                                    popUpTo("library") { inclusive = true }
                                }
                            }
                        )
                    }
                    composable(
                        route = "reader/{bookId}?chapter={chapter}&paragraph={paragraph}",
                        arguments = listOf(
                            navArgument("bookId") { type = NavType.StringType },
                            navArgument("chapter") { type = NavType.IntType; defaultValue = 1 },
                            navArgument("paragraph") { type = NavType.IntType; defaultValue = 0 }
                        )
                    ) {
                        ReaderScreen(
                            onBackClick = {
                                navController.popBackStack()
                            }
                        )
                    }
                }
            }
        }
    }
}
