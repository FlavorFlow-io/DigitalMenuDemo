package dev.lssoftware.digitalmenu

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import dev.lssoftware.digitalmenu.presentation.ui.AppNavHost
import dev.lssoftware.digitalmenu.ui.theme.DigitalMenuTheme

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
