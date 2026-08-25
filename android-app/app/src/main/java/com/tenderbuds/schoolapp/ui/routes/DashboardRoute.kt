package com.tenderbuds.schoolapp.ui.routes

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import com.tenderbuds.schoolapp.data.ApiClient
import com.tenderbuds.schoolapp.data.ApiResult
import com.tenderbuds.schoolapp.data.ClassStockItem
import com.tenderbuds.schoolapp.data.DashboardSummary
import com.tenderbuds.schoolapp.data.SessionManager
import com.tenderbuds.schoolapp.data.StudentFeeStatus
import com.tenderbuds.schoolapp.ui.navigation.Destinations
import com.tenderbuds.schoolapp.ui.screens.DashboardScreen
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DashboardRoute(navController: NavHostController) {
    val token = SessionManager.token
    val userName = SessionManager.userName ?: "Admin"
    val context = LocalContext.current

    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var summary by remember { mutableStateOf<DashboardSummary?>(null) }
    var studentFeeStatus by remember { mutableStateOf<List<StudentFeeStatus>>(emptyList()) }
    var classStock by remember { mutableStateOf<List<ClassStockItem>>(emptyList()) }
    var stockFilter by remember { mutableStateOf("") }
    var latestStockRequestId by remember { mutableStateOf(0) }
    var isExporting by remember { mutableStateOf(false) }
    var isImporting by remember { mutableStateOf(false) }
    var pendingExportJson by remember { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    fun goToLogin() {
        SessionManager.signOut()
        navController.navigate(Destinations.LOGIN) {
            popUpTo(0) { inclusive = true }
        }
    }

    fun reloadAll() {
        val currentToken = token
        if (currentToken == null) {
            goToLogin()
            return
        }
        errorMessage = null
        isLoading = true
        scope.launch {
            val summaryResult = ApiClient.getDashboardSummary(currentToken)
            val feeStatusResult = ApiClient.getStudentFeeStatus(currentToken)
            val stockResult = ApiClient.getClassStockAvailability(currentToken, stockFilter)

            when (summaryResult) {
                is ApiResult.Success -> summary = summaryResult.data
                is ApiResult.Failure -> errorMessage = summaryResult.message
            }
            if (feeStatusResult is ApiResult.Success) studentFeeStatus = feeStatusResult.data
            if (stockResult is ApiResult.Success) classStock = stockResult.data
            isLoading = false
        }
    }

    fun reloadStock(filter: String) {
        val currentToken = token ?: return
        // Typing fires one request per keystroke; a slower earlier response
        // could otherwise land after a faster later one and show stale
        // results. Only the most recently issued request is allowed to win.
        val requestId = ++latestStockRequestId
        scope.launch {
            when (val result = ApiClient.getClassStockAvailability(currentToken, filter)) {
                is ApiResult.Success -> if (requestId == latestStockRequestId) classStock = result.data
                is ApiResult.Failure -> Unit // Keep the last known list rather than clearing it on a filter hiccup.
            }
        }
    }

    // Lets the user pick where to save the backup file (Storage Access
    // Framework — no storage permission needed). Triggered only after the
    // export data is already fetched, so a failed API call never bothers
    // the user with a save dialog.
    val createDocumentLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        val json = pendingExportJson
        pendingExportJson = null
        if (uri != null && json != null) {
            try {
                context.contentResolver.openOutputStream(uri)?.use { it.write(json.toByteArray(Charsets.UTF_8)) }
                scope.launch { snackbarHostState.showSnackbar("Backup saved successfully.") }
            } catch (e: Exception) {
                scope.launch { snackbarHostState.showSnackbar("Could not save the backup file: ${e.message}") }
            }
        }
        isExporting = false
    }

    val openDocumentLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri == null) {
            isImporting = false
            return@rememberLauncherForActivityResult
        }
        val currentToken = token
        if (currentToken == null) {
            isImporting = false
            goToLogin()
            return@rememberLauncherForActivityResult
        }
        scope.launch {
            val json = try {
                context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
            } catch (e: Exception) {
                null
            }
            if (json == null) {
                isImporting = false
                scope.launch { snackbarHostState.showSnackbar("Could not read that file.") }
                return@launch
            }
            // showSnackbar() suspends until the snackbar is dismissed, so it's
            // fired in its own coroutine — the button and data refresh below
            // must not wait for that.
            when (val result = ApiClient.importBackup(currentToken, json)) {
                is ApiResult.Success -> {
                    val s = result.data
                    scope.launch {
                        snackbarHostState.showSnackbar(
                            "Restored: +${s.addedStudents} students, +${s.addedFees} fees, +${s.addedStock} stock items"
                        )
                    }
                    isImporting = false
                    reloadAll()
                }
                is ApiResult.Failure -> {
                    isImporting = false
                    scope.launch { snackbarHostState.showSnackbar(result.message) }
                }
            }
        }
    }

    LaunchedEffect(Unit) { reloadAll() }

    DashboardScreen(
        userName = userName,
        isLoading = isLoading,
        errorMessage = errorMessage,
        summary = summary,
        studentFeeStatus = studentFeeStatus,
        classStock = classStock,
        stockFilter = stockFilter,
        onStockFilterChange = { newFilter ->
            stockFilter = newFilter
            reloadStock(newFilter)
        },
        isExporting = isExporting,
        isImporting = isImporting,
        onExportClick = {
            val currentToken = token ?: return@DashboardScreen
            isExporting = true
            scope.launch {
                when (val result = ApiClient.exportBackup(currentToken)) {
                    is ApiResult.Success -> {
                        pendingExportJson = result.data
                        val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
                        createDocumentLauncher.launch("TenderBuds_Backup_$dateStr.json")
                    }
                    is ApiResult.Failure -> {
                        isExporting = false
                        scope.launch { snackbarHostState.showSnackbar(result.message) }
                    }
                }
            }
        },
        onImportClick = {
            isImporting = true
            openDocumentLauncher.launch(arrayOf("application/json"))
        },
        onRefresh = { reloadAll() },
        onRetry = { reloadAll() },
        onLogout = { goToLogin() },
        snackbarHostState = snackbarHostState
    )
}
