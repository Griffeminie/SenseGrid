package com.example.myapplication

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.google.firebase.database.DatabaseReference
import com.google.accompanist.navigation.animation.AnimatedNavHost
import com.google.accompanist.navigation.animation.composable
import androidx.compose.animation.*

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun AppNavigation(
    navController: NavHostController,
    uploadEnabledRef: DatabaseReference,
    readingsRef: DatabaseReference
) {

    AnimatedNavHost(
        navController = navController,
        startDestination = "main",
        enterTransition = {
            slideInHorizontally(initialOffsetX = { 1000 }) + fadeIn()
        },
        exitTransition = {
            slideOutHorizontally(targetOffsetX = { -300 }) + fadeOut()
        },
        popEnterTransition = {
            slideInHorizontally(initialOffsetX = { -1000 }) + fadeIn()
        },
        popExitTransition = {
            slideOutHorizontally(targetOffsetX = { 300 }) + fadeOut()
        }
    ) {

        composable("main") {
            MainScreen(
                navController = navController,
                uploadEnabledRef = uploadEnabledRef,
                readingsRef = readingsRef
            )
        }

        composable("info") {
            InfoPage(navController = navController, readingsRef = readingsRef)
        }
    }
}
