package com.example.paseape.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.paseape.presentation.screens.account.AccountScreen
import com.example.paseape.presentation.screens.auth.LoginScreen
import com.example.paseape.presentation.screens.auth.google.GoogleRegisterScreen
import com.example.paseape.presentation.screens.dashboard.DashboardScreen
import com.example.paseape.presentation.screens.paseadores.PaseadoresScreen
import com.example.paseape.presentation.screens.splash.SplashScreen

@Composable
fun AppNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = ScreenRoute.SplashRoute,
        modifier = modifier
    ) {
        composable<ScreenRoute.SplashRoute> {
            SplashScreen(
                onNavigateNext = {
                    navController.navigate(ScreenRoute.AccountRoute) {
                        popUpTo<ScreenRoute.SplashRoute> { inclusive = true }
                    }
                }
            )
        }

        composable<ScreenRoute.AccountRoute> {
            AccountScreen(
                onNavigateToLogin = {
                    navController.navigate(ScreenRoute.LoginRoute)
                },
                onNavigateToGoogleRegister = {
                    navController.navigate(ScreenRoute.GoogleRegisterRoute)
                }
            )
        }

        composable<ScreenRoute.LoginRoute> {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(ScreenRoute.DashboardRoute) {
                        popUpTo<ScreenRoute.AccountRoute> { inclusive = true }
                    }
                },
                onNavigateToRegister = {
                    // Navegación a pantalla de registro futuro
                }
            )
        }

        composable<ScreenRoute.DashboardRoute> {
            DashboardScreen(
                onPaseadoresClick = {
                    navController.navigate(ScreenRoute.PaseadoresRoute)
                }
            )
        }

        composable<ScreenRoute.PaseadoresRoute> {
            PaseadoresScreen(
                onBack = {
                    navController.popBackStack()
                }
            )
        }

        composable<ScreenRoute.GoogleRegisterRoute> {
            GoogleRegisterScreen(
                onBack = { navController.popBackStack() },
                onRegistrationSuccess = {
                    navController.navigate(ScreenRoute.DashboardRoute) {
                        popUpTo<ScreenRoute.AccountRoute> { inclusive = true }
                    }
                }
            )
        }
    }
}
