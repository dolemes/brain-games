package com.dolemes.braingames.data

import android.util.Log
import androidx.core.util.AtomicFile
import com.dolemes.braingames.core.PlayerProgress
import com.dolemes.braingames.core.ProgressJson
import java.io.File
import java.io.IOException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Progresso do jogador num arquivo JSON local. O AtomicFile grava num arquivo novo e só troca
 * no fim: se o Android matar o app no meio da gravação, o save anterior continua inteiro.
 */
class ProgressRepository(
    file: File,
    private val io: CoroutineDispatcher = Dispatchers.IO,
) {
    private val atomicFile = AtomicFile(file)
    private val mutex = Mutex()
    private val loaded = CompletableDeferred<Unit>()
    private val state = MutableStateFlow(PlayerProgress())

    val progress: StateFlow<PlayerProgress> = state.asStateFlow()

    suspend fun load() {
        mutex.withLock {
            if (loaded.isCompleted) return
            state.value = withContext(io) { read() } ?: PlayerProgress()
            loaded.complete(Unit)
        }
    }

    /** Aplica uma mudança e grava. Espera o carregamento, para nunca sobrescrever o save com o padrão. */
    suspend fun update(transform: (PlayerProgress) -> PlayerProgress): PlayerProgress {
        loaded.await()
        return mutex.withLock {
            val updated = transform(state.value)
            if (updated != state.value) {
                state.value = updated
                withContext(io) { write(updated) }
            }
            updated
        }
    }

    private fun read(): PlayerProgress? =
        try {
            ProgressJson.decode(atomicFile.readFully().decodeToString())
        } catch (e: IOException) {
            null // primeiro uso: ainda não existe arquivo
        }

    private fun write(progress: PlayerProgress) {
        val out = try {
            atomicFile.startWrite()
        } catch (e: IOException) {
            Log.w(TAG, "Não foi possível abrir o save para gravação", e)
            return
        }
        try {
            out.write(ProgressJson.encode(progress).encodeToByteArray())
            atomicFile.finishWrite(out)
        } catch (e: IOException) {
            atomicFile.failWrite(out)
            Log.w(TAG, "Falha ao gravar o save", e)
        }
    }

    private companion object {
        const val TAG = "ProgressRepository"
    }
}
