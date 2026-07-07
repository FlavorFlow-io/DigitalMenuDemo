package io.flavorflow.demo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import io.flavorflow.demo.presentation.ui.AppNavHost
import io.flavorflow.demo.ui.theme.DigitalMenuTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DigitalMenuTheme {
                AppNavHost()
            }
        }
    }
}
