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
import com.tenderbuds.schoolapp.ui.navigation.Destinations
import com.tenderbuds.schoolapp.ui.screens.SignUpScreen
import kotlinx.coroutines.launch

@Composable
fun SignUpRoute(navController: NavHostController) {
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isSuccess by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    SignUpScreen(
        isLoading = isLoading,
        errorMessage = errorMessage,
        isSuccess = isSuccess,
        onSignUpClick = { name, username, email, password ->
            errorMessage = null
            isLoading = true
            scope.launch {
                when (val result = ApiClient.signup(name, username, email, password)) {
                    is ApiResult.Success -> isSuccess = true
                    is ApiResult.Failure -> errorMessage = result.message
                }
                isLoading = false
            }
        },
        onBackToLoginClick = {
            navController.popBackStack(Destinations.LOGIN, inclusive = false)
        }
    )
}
