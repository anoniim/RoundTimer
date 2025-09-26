package net.solvetheriddle.roundtimer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import net.solvetheriddle.roundtimer.platform.StatusBarManager
import net.solvetheriddle.roundtimer.storage.initializeStorageFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        
        // Initialize StatusBarManager with this activity
        StatusBarManager.initialize(this)
        
        // Initialize StorageFactory with application context
        initializeStorageFactory(this)

        setContent {
            App()
        }
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}