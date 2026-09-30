package com.dolemes.braingames.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dolemes.braingames.AppContainer
import com.dolemes.braingames.game.GameCatalog
import com.dolemes.braingames.ui.theme.BrainGamesTheme

private const val ROUTE_HOME = "home"
private const val ROUTE_SETTINGS = "settings"
private const val GAME_PREFIX = "game:"

/**
 * Raiz do app. A navegação é um texto salvo ("home", "settings", "game:<id>"): simples,
 * sobrevive à recriação da Activity e dispensa biblioteca de navegação.
 */
@Composable
fun BrainGamesRoot(container: AppContainer) {
    val progress by container.progress.progress.collectAsStateWithLifecycle()
    var route by rememberSaveable { mutableStateOf(ROUTE_HOME) }

    BrainGamesTheme(textScale = progress.preferences.textScale) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            when {
                route == ROUTE_SETTINGS -> SettingsRoute(
                    container = container,
                    progress = progress,
                    onBack = { route = ROUTE_HOME },
                )

                route.startsWith(GAME_PREFIX) -> {
                    val game = GameCatalog.find(route.removePrefix(GAME_PREFIX))
                    if (game == null) {
                        LaunchedEffect(route) { route = ROUTE_HOME }
                    } else {
                        key(route) {
                            GameHostScreen(
                                game = game,
                                container = container,
                                progress = progress,
                                onExit = { route = ROUTE_HOME },
                            )
                        }
                    }
                }

                else -> HomeRoute(
                    container = container,
                    progress = progress,
                    onPlay = { id -> route = GAME_PREFIX + id },
                    onSettings = { route = ROUTE_SETTINGS },
                )
            }
        }
    }
}
