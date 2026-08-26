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
import com.tenderbuds.schoolapp.data.Fee
import com.tenderbuds.schoolapp.data.SessionManager
import com.tenderbuds.schoolapp.data.Student
import com.tenderbuds.schoolapp.ui.screens.AddEditFeeScreen
import kotlinx.coroutines.launch

@Composable
fun AddEditFeeRoute(navController: NavHostController, feeId: String?) {
    val token = SessionManager.token
    val isEditMode = feeId != null

    var students by remember { mutableStateOf<List<Student>>(emptyList()) }
    var initialFee by remember { mutableStateOf<Fee?>(null) }
    var isLoadingInitial by remember { mutableStateOf(true) }
    var isSaving by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(feeId) {
        val currentToken = token
        if (currentToken != null) {
            when (val studentsResult = ApiClient.getStudents(currentToken)) {
                is ApiResult.Success -> students = studentsResult.data
                is ApiResult.Failure -> errorMessage = studentsResult.message
            }
            if (feeId != null) {
                when (val feeResult = ApiClient.getFee(currentToken, feeId)) {
                    is ApiResult.Success -> initialFee = feeResult.data
                    is ApiResult.Failure -> errorMessage = feeResult.message
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

    AddEditFeeScreen(
        isEditMode = isEditMode,
        students = students,
        initialFee = initialFee,
        isLoading = isSaving,
        errorMessage = errorMessage,
        onSaveClick = { form ->
            val currentToken = token
            if (currentToken != null) {
                isSaving = true
                errorMessage = null
                scope.launch {
                    val result = if (isEditMode && feeId != null) {
                        ApiClient.updateFee(
                            currentToken, feeId, form.regNo, form.feeType, form.month,
                            form.totalAmount, form.amountPaid, form.paymentDate
                        )
                    } else {
                        ApiClient.createFee(
                            currentToken, form.regNo, form.feeType, form.month,
                            form.totalAmount, form.amountPaid, form.paymentDate
                        )
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
