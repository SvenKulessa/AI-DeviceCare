package online.capitalai.devicecare

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import online.capitalai.devicecare.core.ui.DeviceCareHome

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { DeviceCareHome(appName = "AI-DeviceCare", enterprise = false) }
    }
}
