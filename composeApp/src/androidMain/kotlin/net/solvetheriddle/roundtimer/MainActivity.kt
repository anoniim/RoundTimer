package net.solvetheriddle.roundtimer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.tooling.preview.Preview
import com.google.firebase.FirebaseApp
import net.solvetheriddle.roundtimer.platform.StatusBarManager
import net.solvetheriddle.roundtimer.platform.getSoundPlayer
import net.solvetheriddle.roundtimer.storage.initializeStorageFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // Set current activity
        CurrentActivity.INSTANCE = this

        // Initialize AppContext
        AppContext.initialize(this)

        // Initialize Firebase
        FirebaseApp.initializeApp(this)
        
        // Initialize StatusBarManager with this activity
        StatusBarManager.initialize(this)
        
        // Initialize StorageFactory with application context
        initializeStorageFactory(this)

        setContent {
            val soundPlayer = getSoundPlayer()
            DisposableEffect(Unit) {
                onDispose {
                    soundPlayer.cleanup()
                }
            }
            App()
        }
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}