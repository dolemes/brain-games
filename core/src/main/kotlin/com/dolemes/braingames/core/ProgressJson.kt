package com.dolemes.braingames.core

import kotlinx.serialization.json.Json

/**
 * Converte o progresso para JSON e de volta. `ignoreUnknownKeys` deixa um save mais novo
 * abrir numa versão antiga do app; os valores padrão dos campos cobrem saves mais velhos.
 * Mudou a estrutura de forma incompatível? Suba [PlayerProgress.CURRENT_SCHEMA_VERSION] e
 * trate em [migrate].
 */
object ProgressJson {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun encode(progress: PlayerProgress): String =
        json.encodeToString(PlayerProgress.serializer(), progress)

    /** Devolve null se o texto estiver corrompido (o repositório tenta o backup). */
    fun decode(text: String): PlayerProgress? =
        try {
            migrate(json.decodeFromString(PlayerProgress.serializer(), text))
        } catch (e: IllegalArgumentException) { // inclui SerializationException
            null
        }

    private fun migrate(progress: PlayerProgress): PlayerProgress {
        var p = progress
        // Exemplo para o futuro:
        // if (p.schemaVersion < 2) p = p.copy(...)
        if (p.schemaVersion != PlayerProgress.CURRENT_SCHEMA_VERSION) {
            p = p.copy(schemaVersion = PlayerProgress.CURRENT_SCHEMA_VERSION)
        }
        return p
    }
}
