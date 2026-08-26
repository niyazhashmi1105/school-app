package com.tenderbuds.schoolapp.ui.routes

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import com.tenderbuds.schoolapp.data.ApiClient
import com.tenderbuds.schoolapp.data.ApiResult
import com.tenderbuds.schoolapp.data.SessionManager
import com.tenderbuds.schoolapp.data.Stock
import com.tenderbuds.schoolapp.ui.screens.AddEditStockScreen
import kotlinx.coroutines.launch

@Composable
fun AddEditStockRoute(navController: NavHostController, stockId: String?) {
    val token = SessionManager.token
    val isEditMode = stockId != null

    var existingStock by remember { mutableStateOf<List<Stock>>(emptyList()) }
    var initialStock by remember { mutableStateOf<Stock?>(null) }
    var isLoadingInitial by remember { mutableStateOf(true) }
    var isSaving by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(stockId) {
        val currentToken = token
        if (currentToken != null) {
            when (val listResult = ApiClient.getStockList(currentToken)) {
                is ApiResult.Success -> existingStock = listResult.data
                is ApiResult.Failure -> errorMessage = listResult.message
            }
            if (stockId != null) {
                when (val stockResult = ApiClient.getStock(currentToken, stockId)) {
                    is ApiResult.Success -> initialStock = stockResult.data
                    is ApiResult.Failure -> errorMessage = stockResult.message
                }
            }
        }
        isLoadingInitial = false
    }

    if (isLoadingInitial) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    AddEditStockScreen(
        isEditMode = isEditMode,
        existingStock = existingStock,
        initialStock = initialStock,
        isLoading = isSaving,
        errorMessage = errorMessage,
        onSaveClick = { stock ->
            val currentToken = token
            if (currentToken != null) {
                isSaving = true
                errorMessage = null
                scope.launch {
                    val result = if (isEditMode && stockId != null) {
                        ApiClient.updateStock(currentToken, stockId, stock)
                    } else {
                        ApiClient.createStock(currentToken, stock)
                    }
                    when (result) {
                        is ApiResult.Success -> navController.popBackStack()
                        is ApiResult.Failure -> errorMessage = result.message
                    }
                    isSaving = false
                }
            }
        },
        onBackClick = { navController.popBackStack() }
    )
}
