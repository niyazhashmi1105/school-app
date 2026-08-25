package com.tenderbuds.schoolapp.ui.routes

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.navigation.NavHostController
import com.tenderbuds.schoolapp.data.ApiClient
import com.tenderbuds.schoolapp.data.ApiResult
import com.tenderbuds.schoolapp.data.SessionManager
import com.tenderbuds.schoolapp.ui.navigation.Destinations
import com.tenderbuds.schoolapp.ui.screens.LoginScreen
import kotlinx.coroutines.launch

@Composable
fun LoginRoute(navController: NavHostController) {
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    LoginScreen(
        isLoading = isLoading,
        errorMessage = errorMessage,
        onLoginClick = { username, password ->
            errorMessage = null
            isLoading = true
            scope.launch {
                when (val result = ApiClient.login(username, password)) {
                    is ApiResult.Success -> {
                        SessionManager.signIn(result.data)
                        navController.navigate(Destinations.MAIN) {
                            popUpTo(Destinations.LOGIN) { inclusive = true }
                        }
                    }
                    is ApiResult.Failure -> errorMessage = result.message
                }
                isLoading = false
            }
        },
        onSignUpClick = { navController.navigate(Destinations.SIGN_UP) },
        onForgotPasswordClick = { username -> navController.navigate(Destinations.forgotPassword(username)) }
    )
}
