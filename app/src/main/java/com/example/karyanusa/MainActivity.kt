package com.example.karyanusa

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.ui.Alignment
import androidx.core.view.WindowCompat
import androidx.navigation.NavBackStackEntry
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.karyanusa.component.auth.LoginScreen
import com.example.karyanusa.component.auth.RegisterScreen
import com.example.karyanusa.component.kursus.DetailPage
import com.example.karyanusa.component.kursus.KursusPage
import com.example.karyanusa.component.kursus.MateriPage
import com.example.karyanusa.component.kursus.VideoPage
import com.example.karyanusa.ui.theme.KaryaNusaTheme

@OptIn(ExperimentalAnimationApi::class)
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            KaryaNusaTheme {
                val navController = rememberNavController()

                NavHost(
                    navController = navController,
                    startDestination = "login"
                ) {
                    composable("login",
                        enterTransition = { fadeIn(animationSpec = tween(300)) },
                        exitTransition = { fadeOut(animationSpec = tween(300)) }
                    ) {
                        LoginScreen(navController)
                    }

                    composable("register",
                        enterTransition = { slideInHorizontally(initialOffsetX = { 1000 }) + fadeIn() },
                        exitTransition = { slideOutHorizontally(targetOffsetX = { -1000 }) + fadeOut() }
                    ) {
                        RegisterScreen(navController)
                    }

                    composable("kursus",
                        enterTransition = { fadeIn(animationSpec = tween(400)) },
                        exitTransition = { fadeOut(animationSpec = tween(300)) }
                    ) {
                        KursusPage(navController)
                    }

                    composable("detail/{id}",
                        enterTransition = { slideInHorizontally(initialOffsetX = { 1000 }, animationSpec = tween(400)) },
                        exitTransition = { slideOutHorizontally(targetOffsetX = { -1000 }, animationSpec = tween(400)) }
                    ) { backStackEntry ->
                        val id = backStackEntry.arguments?.getString("id")?.toInt() ?: 0
                        DetailPage(navController, kursusId = id)
                    }

                    composable("materi/{kursusId}",
                        enterTransition = { fadeIn(animationSpec = tween(400)) },
                        exitTransition = { fadeOut(animationSpec = tween(400)) }
                    ) { backStack ->
                        val kursusId = backStack.arguments?.getString("kursusId")?.toIntOrNull() ?: 0
                        MateriPage(navController, kursusId)
                    }

                    composable("video/{videoFile}",
                        enterTransition = { fadeIn(animationSpec = tween(400)) },
                        exitTransition = { fadeOut(animationSpec = tween(400)) }
                    ) { backStackEntry ->
                        val videoFile = backStackEntry.arguments?.getString("videoFile") ?: ""
                        VideoPage(navController, videoFile)
                    }
                }
            }
        }
    }
}
