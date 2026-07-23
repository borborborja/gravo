package com.gravo.grabadora

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalView
import androidx.core.content.ContextCompat
import androidx.core.os.LocaleListCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.rememberNavController
import com.gravo.grabadora.audio.RecStatus
import com.gravo.grabadora.data.settings.AppSettings
import com.gravo.grabadora.ui.navigation.AppNavHost
import com.gravo.grabadora.ui.theme.GrabadoraTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as GrabadoraApp).container

        // idioma persistido → locales por app
        lifecycleScope.launch {
            val settings = container.settingsRepository.settings.first()
            val current = AppCompatDelegate.getApplicationLocales().toLanguageTags()
            if (current.isEmpty() || !current.startsWith(settings.language)) {
                AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(settings.language))
            }
            maybeAutoStart(settings)
        }

        setContent {
            val settings by container.settingsRepository.settings
                .collectAsState(initial = AppSettings())
            GrabadoraTheme(darkTheme = settings.darkTheme) {
                val view = LocalView.current
                SideEffect {
                    val window = (view.context as Activity).window
                    val insets = WindowInsetsControllerCompat(window, view)
                    insets.isAppearanceLightStatusBars = !settings.darkTheme
                    insets.isAppearanceLightNavigationBars = !settings.darkTheme
                }
                val navController = rememberNavController()
                AppNavHost(navController, container)
            }
        }
    }

    private fun maybeAutoStart(settings: AppSettings) {
        if (!settings.autoStartRecording) return
        val granted = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED
        if (!granted) return
        val controller = (application as GrabadoraApp).container.recordingController
        if (controller.status.value == RecStatus.IDLE) {
            lifecycleScope.launch { controller.startRecording(settings.toSpec(), settings.micId) }
        }
    }
}
