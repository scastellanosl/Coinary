package com.example.coinary

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.coinary.navigation.AppNavigation
import com.example.coinary.ui.theme.CoinaryTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CoinaryTheme {
                AppNavigation()
            }
        }
    }
}


