package com.tenderbuds.schoolapp.ui.routes

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import com.tenderbuds.schoolapp.data.Student
import com.tenderbuds.schoolapp.ui.screens.AddEditStudentScreen
import kotlinx.coroutines.launch

@Composable
fun AddEditStudentRoute(navController: NavHostController, regNo: String?) {
    val token = SessionManager.token
    val isEditMode = regNo != null

    var initialStudent by remember { mutableStateOf<Student?>(null) }
    var isLoadingInitial by remember { mutableStateOf(isEditMode) }
    var isSaving by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(regNo) {
        val currentToken = token
        if (regNo != null && currentToken != null) {
            when (val result = ApiClient.getStudent(currentToken, regNo)) {
                is ApiResult.Success -> initialStudent = result.data
                is ApiResult.Failure -> errorMessage = result.message
            }
            isLoadingInitial = false
        }
    }

    if (isLoadingInitial) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
        return
    }

    AddEditStudentScreen(
        isEditMode = isEditMode,
        initialStudent = initialStudent,
        isLoading = isSaving,
        errorMessage = errorMessage,
        onSaveClick = { student ->
            val currentToken = token
            if (currentToken != null) {
                isSaving = true
                errorMessage = null
                scope.launch {
                    val result = if (isEditMode && regNo != null) {
                        ApiClient.updateStudent(currentToken, regNo, student)
                    } else {
                        ApiClient.createStudent(currentToken, student)
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
