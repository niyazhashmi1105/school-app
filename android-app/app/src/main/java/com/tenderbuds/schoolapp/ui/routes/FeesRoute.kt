package com.tenderbuds.schoolapp.ui.routes

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.navigation.NavHostController
import com.tenderbuds.schoolapp.data.ApiClient
import com.tenderbuds.schoolapp.data.ApiResult
import com.tenderbuds.schoolapp.data.Fee
import com.tenderbuds.schoolapp.data.FeeSummary
import com.tenderbuds.schoolapp.data.SessionManager
import com.tenderbuds.schoolapp.ui.navigation.Destinations
import com.tenderbuds.schoolapp.ui.screens.FeeListScreen
import kotlinx.coroutines.launch

@Composable
fun FeesRoute(navController: NavHostController) {
    val token = SessionManager.token

    var fees by remember { mutableStateOf<List<Fee>>(emptyList()) }
    var summary by remember { mutableStateOf<FeeSummary?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var latestRequestId by remember { mutableStateOf(0) }
    val scope = rememberCoroutineScope()

    fun load(search: String = searchQuery) {
        val currentToken = token ?: return
        val requestId = ++latestRequestId
        isLoading = true
        errorMessage = null
        scope.launch {
            when (val result = ApiClient.getFees(currentToken, search)) {
                is ApiResult.Success -> if (requestId == latestRequestId) fees = result.data
                is ApiResult.Failure -> if (requestId == latestRequestId) errorMessage = result.message
            }
            if (requestId == latestRequestId) isLoading = false
        }
        scope.launch {
            when (val result = ApiClient.getFeeSummary(currentToken)) {
                is ApiResult.Success -> if (requestId == latestRequestId) summary = result.data
                is ApiResult.Failure -> Unit // The list's own error message already covers a failed load.
            }
        }
    }

    LaunchedEffect(Unit) { load() }

    FeeListScreen(
        fees = fees,
        summary = summary,
        isLoading = isLoading,
        errorMessage = errorMessage,
        searchQuery = searchQuery,
        onSearchQueryChange = { query ->
            searchQuery = query
            load(query)
        },
        onRefresh = { load() },
        onRetry = { load() },
        onAddClick = { navController.navigate(Destinations.addFee()) },
        onEditClick = { fee -> navController.navigate(Destinations.editFee(fee.id)) },
        onDeleteConfirm = { fee ->
            val currentToken = token
            if (currentToken != null) {
                scope.launch {
                    when (val result = ApiClient.deleteFee(currentToken, fee.id)) {
                        is ApiResult.Success -> load()
                        is ApiResult.Failure -> errorMessage = result.message
                    }
                }
            }
        }
    )
}
