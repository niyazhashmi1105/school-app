package com.tenderbuds.schoolapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.tenderbuds.schoolapp.ui.navigation.AppNavHost
import com.tenderbuds.schoolapp.ui.theme.TenderBudsTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TenderBudsTheme {
                // Screens 1-3 of 14: Login, Sign Up, Forgot Password.
                AppNavHost()
            }
        }
    }
}
