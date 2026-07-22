package com.gravo.grabadora

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import com.gravo.grabadora.ui.theme.GrabadoraTheme

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            GrabadoraTheme(darkTheme = true) {
                Text("Grabadora", modifier = Modifier.fillMaxSize().background(GrabadoraTheme.colors.bg))
            }
        }
    }
}
