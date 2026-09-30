package com.dolemes.braingames

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.dolemes.braingames.ui.BrainGamesRoot

/**
 * Única Activity. É AppCompatActivity (e não só ComponentActivity) por causa da escolha de
 * idioma dentro do app (AppCompatDelegate.setApplicationLocales).
 */
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val container = (application as BrainGamesApp).container

        // Consentimento primeiro (exigido para usuários do EEE, Reino Unido e Suíça); depois anúncios.
        if (savedInstanceState == null) {
            container.consent.gather(this) {
                if (container.consent.canRequestAds) container.ads.initialize(applicationContext)
            }
        }
        // Consentimento dado em sessões anteriores já permite iniciar.
        if (container.consent.canRequestAds) container.ads.initialize(applicationContext)

        setContent { BrainGamesRoot(container) }
    }
}
