package com.example.travelapp.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import com.example.travelapp.ui.navigation.AppNavGraph

// Unica Activity dell'app: ospita il grafo di navigazione Compose.
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                AppNavGraph(onExitApp = { finish() })
            }
        }
    }
}
