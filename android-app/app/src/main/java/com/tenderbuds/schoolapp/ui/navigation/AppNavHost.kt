package com.tenderbuds.schoolapp.ui.navigation

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
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

    fun forgotPassword(username: String): String =
        "$FORGOT_PASSWORD?$FORGOT_PASSWORD_USERNAME_ARG=${Uri.encode(username)}"

    fun addStudent(): String = ADD_EDIT_STUDENT

    fun editStudent(regNo: String): String =
        "$ADD_EDIT_STUDENT?$STUDENT_REG_NO_ARG=${Uri.encode(regNo)}"
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
    }
}
