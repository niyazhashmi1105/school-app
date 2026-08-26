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
import com.tenderbuds.schoolapp.data.SessionManager
import com.tenderbuds.schoolapp.data.Stock
import com.tenderbuds.schoolapp.ui.navigation.Destinations
import com.tenderbuds.schoolapp.ui.screens.StockListScreen
import kotlinx.coroutines.launch

@Composable
fun StockRoute(navController: NavHostController) {
    val token = SessionManager.token

    var stockItems by remember { mutableStateOf<List<Stock>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var latestRequestId by remember { mutableStateOf(0) }
    val scope = rememberCoroutineScope()

    fun load() {
        val currentToken = token ?: return
        val requestId = ++latestRequestId
        isLoading = true
        errorMessage = null
        scope.launch {
            when (val result = ApiClient.getStockList(currentToken)) {
                is ApiResult.Success -> if (requestId == latestRequestId) stockItems = result.data
                is ApiResult.Failure -> if (requestId == latestRequestId) errorMessage = result.message
            }
            if (requestId == latestRequestId) isLoading = false
        }
    }

    LaunchedEffect(Unit) { load() }

    StockListScreen(
        stockItems = stockItems,
        isLoading = isLoading,
        errorMessage = errorMessage,
        onRefresh = { load() },
        onRetry = { load() },
        onAddClick = { navController.navigate(Destinations.addStock()) },
        onEditClick = { stock -> navController.navigate(Destinations.editStock(stock.id)) },
        onDeleteConfirm = { stock ->
            val currentToken = token
            if (currentToken != null) {
                scope.launch {
                    when (val result = ApiClient.deleteStock(currentToken, stock.id)) {
                        is ApiResult.Success -> load()
                        is ApiResult.Failure -> errorMessage = result.message
                    }
                }
            }
        }
    )
}
