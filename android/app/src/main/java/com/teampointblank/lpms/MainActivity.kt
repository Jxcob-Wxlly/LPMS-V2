package com.teampointblank.lpms

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.teampointblank.lpms.ui.navigation.LpmsNavHost
import com.teampointblank.lpms.ui.theme.LpmsTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as LpmsApplication).container

        setContent {
            val settings by container.settings.state.collectAsStateWithLifecycle()
            LpmsTheme(themeMode = settings.themeMode, largeText = settings.largeText) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    LpmsNavHost(container)
                }
            }
        }
    }
}
