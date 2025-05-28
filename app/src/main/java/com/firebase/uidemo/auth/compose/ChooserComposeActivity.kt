package com.firebase.uidemo.auth.compose

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.firebase.uidemo.ui.theme.FirebaseUIDemoTheme

class ChooserComposeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FirebaseUIDemoTheme {
                ChooserScreen()
            }
        }
    }
} 