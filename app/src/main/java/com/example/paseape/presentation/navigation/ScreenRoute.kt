package com.example.paseape.presentation.navigation

import kotlinx.serialization.Serializable

sealed interface ScreenRoute {
    @Serializable
    data object SplashRoute : ScreenRoute

    @Serializable
    data object AccountRoute : ScreenRoute

    @Serializable
    data object LoginRoute : ScreenRoute

    @Serializable
    data object GoogleRegisterRoute : ScreenRoute

    @Serializable
    data object DashboardRoute : ScreenRoute

    @Serializable
    data object PaseadoresRoute : ScreenRoute
}
