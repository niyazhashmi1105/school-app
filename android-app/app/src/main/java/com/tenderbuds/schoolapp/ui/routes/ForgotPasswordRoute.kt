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
import com.tenderbuds.schoolapp.ui.screens.ForgotPasswordScreen
import kotlinx.coroutines.launch

@Composable
fun ForgotPasswordRoute(navController: NavHostController, prefilledUsername: String) {
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    ForgotPasswordScreen(
        username = prefilledUsername,
        isLoading = isLoading,
        errorMessage = errorMessage,
        successMessage = successMessage,
        onResetClick = { newPassword ->
            errorMessage = null
            isLoading = true
            scope.launch {
                when (val result = ApiClient.resetPassword(prefilledUsername, newPassword)) {
                    is ApiResult.Success -> successMessage = result.data
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
