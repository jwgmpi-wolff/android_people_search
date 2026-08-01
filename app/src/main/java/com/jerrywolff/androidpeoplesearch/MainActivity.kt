package com.jerrywolff.androidpeoplesearch

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.jerrywolff.androidpeoplesearch.ui.AndroidPeopleSearchApp
import com.jerrywolff.androidpeoplesearch.ui.theme.AndroidPeopleSearchTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AndroidPeopleSearchTheme { AndroidPeopleSearchApp() }
        }
    }
}
