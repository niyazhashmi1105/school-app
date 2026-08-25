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
import com.tenderbuds.schoolapp.data.Student
import com.tenderbuds.schoolapp.ui.navigation.Destinations
import com.tenderbuds.schoolapp.ui.screens.StudentListScreen
import kotlinx.coroutines.launch

@Composable
fun StudentsRoute(navController: NavHostController) {
    val token = SessionManager.token

    var students by remember { mutableStateOf<List<Student>>(emptyList()) }
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
            when (val result = ApiClient.getStudents(currentToken, search)) {
                is ApiResult.Success -> if (requestId == latestRequestId) students = result.data
                is ApiResult.Failure -> if (requestId == latestRequestId) errorMessage = result.message
            }
            if (requestId == latestRequestId) isLoading = false
        }
    }

    LaunchedEffect(Unit) { load() }

    StudentListScreen(
        students = students,
        isLoading = isLoading,
        errorMessage = errorMessage,
        searchQuery = searchQuery,
        onSearchQueryChange = { query ->
            searchQuery = query
            load(query)
        },
        onRefresh = { load() },
        onRetry = { load() },
        onAddClick = { navController.navigate(Destinations.addStudent()) },
        onEditClick = { student -> navController.navigate(Destinations.editStudent(student.regNo)) },
        onDeleteConfirm = { student ->
            val currentToken = token
            if (currentToken != null) {
                scope.launch {
                    when (val result = ApiClient.deleteStudent(currentToken, student.regNo)) {
                        is ApiResult.Success -> load()
                        is ApiResult.Failure -> errorMessage = result.message
                    }
                }
            }
        }
    )
}
