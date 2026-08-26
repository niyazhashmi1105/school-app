package com.tenderbuds.schoolapp.ui.navigation

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.tenderbuds.schoolapp.ui.routes.AddEditFeeRoute
import com.tenderbuds.schoolapp.ui.routes.AddEditStockRoute
import com.tenderbuds.schoolapp.ui.routes.AddEditStudentRoute
import com.tenderbuds.schoolapp.ui.routes.ForgotPasswordRoute
import com.tenderbuds.schoolapp.ui.routes.LoginRoute
import com.tenderbuds.schoolapp.ui.routes.SignUpRoute

object Destinations {
    const val LOGIN = "login"
    const val SIGN_UP = "signup"
    const val MAIN = "main"
    const val FORGOT_PASSWORD = "forgotPassword"
    private const val FORGOT_PASSWORD_USERNAME_ARG = "username"
    const val FORGOT_PASSWORD_ROUTE = "$FORGOT_PASSWORD?$FORGOT_PASSWORD_USERNAME_ARG={$FORGOT_PASSWORD_USERNAME_ARG}"

    const val ADD_EDIT_STUDENT = "addEditStudent"
    private const val STUDENT_REG_NO_ARG = "regNo"
    const val ADD_EDIT_STUDENT_ROUTE = "$ADD_EDIT_STUDENT?$STUDENT_REG_NO_ARG={$STUDENT_REG_NO_ARG}"

    const val ADD_EDIT_FEE = "addEditFee"
    private const val FEE_ID_ARG = "feeId"
    const val ADD_EDIT_FEE_ROUTE = "$ADD_EDIT_FEE?$FEE_ID_ARG={$FEE_ID_ARG}"

    const val ADD_EDIT_STOCK = "addEditStock"
    private const val STOCK_ID_ARG = "stockId"
    const val ADD_EDIT_STOCK_ROUTE = "$ADD_EDIT_STOCK?$STOCK_ID_ARG={$STOCK_ID_ARG}"

    fun forgotPassword(username: String): String =
        "$FORGOT_PASSWORD?$FORGOT_PASSWORD_USERNAME_ARG=${Uri.encode(username)}"

    fun addStudent(): String = ADD_EDIT_STUDENT

    fun editStudent(regNo: String): String =
        "$ADD_EDIT_STUDENT?$STUDENT_REG_NO_ARG=${Uri.encode(regNo)}"

    fun addFee(): String = ADD_EDIT_FEE

    fun editFee(feeId: String): String =
        "$ADD_EDIT_FEE?$FEE_ID_ARG=${Uri.encode(feeId)}"

    fun addStock(): String = ADD_EDIT_STOCK

    fun editStock(stockId: String): String =
        "$ADD_EDIT_STOCK?$STOCK_ID_ARG=${Uri.encode(stockId)}"
}

@Composable
fun AppNavHost(navController: NavHostController = rememberNavController()) {
    NavHost(navController = navController, startDestination = Destinations.LOGIN) {
        composable(Destinations.LOGIN) { LoginRoute(navController) }
        composable(Destinations.SIGN_UP) { SignUpRoute(navController) }
        composable(Destinations.MAIN) { MainScreen(navController) }
        composable(
            route = Destinations.FORGOT_PASSWORD_ROUTE,
            arguments = listOf(navArgument("username") { type = NavType.StringType; defaultValue = "" })
        ) { backStackEntry ->
            val username = backStackEntry.arguments?.getString("username").orEmpty()
            ForgotPasswordRoute(navController, prefilledUsername = username)
        }
        composable(
            route = Destinations.ADD_EDIT_STUDENT_ROUTE,
            arguments = listOf(navArgument("regNo") { type = NavType.StringType; nullable = true; defaultValue = null })
        ) { backStackEntry ->
            val regNo = backStackEntry.arguments?.getString("regNo")
            AddEditStudentRoute(navController, regNo)
        }
        composable(
            route = Destinations.ADD_EDIT_FEE_ROUTE,
            arguments = listOf(navArgument("feeId") { type = NavType.StringType; nullable = true; defaultValue = null })
        ) { backStackEntry ->
            val feeId = backStackEntry.arguments?.getString("feeId")
            AddEditFeeRoute(navController, feeId)
        }
        composable(
            route = Destinations.ADD_EDIT_STOCK_ROUTE,
            arguments = listOf(navArgument("stockId") { type = NavType.StringType; nullable = true; defaultValue = null })
        ) { backStackEntry ->
            val stockId = backStackEntry.arguments?.getString("stockId")
            AddEditStockRoute(navController, stockId)
        }
    }
}
