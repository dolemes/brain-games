package com.dolemes.braingames

import android.app.Application
import com.dolemes.braingames.ads.AdService
import com.dolemes.braingames.ads.ConsentManager
import com.dolemes.braingames.ads.GmaAdService
import com.dolemes.braingames.core.AdPolicy
import com.dolemes.braingames.core.RoundResult
import com.dolemes.braingames.core.StaircaseState
import com.dolemes.braingames.data.ProgressRepository
import java.io.File
import java.time.LocalDate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

class BrainGamesApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}

/**
 * Dependências do app, criadas uma vez por processo. Os minijogos não enxergam nada daqui:
 * save, anúncios e navegação são responsabilidade do GameHostScreen.
 */
class AppContainer(app: Application) {
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    val progress = ProgressRepository(File(app.filesDir, "progress.json"))
    val adPolicy = AdPolicy()
    val consent = ConsentManager(app)
    val ads: AdService = GmaAdService(
        appId = BuildConfig.ADMOB_APP_ID,
        interstitialUnitId = BuildConfig.ADMOB_INTERSTITIAL_ID,
        rewardedUnitId = BuildConfig.ADMOB_REWARDED_ID,
    )

    init {
        appScope.launch { progress.load() }
    }

    /**
     * Salva o resultado de uma rodada. Roda no escopo do app (não da tela), para o save
     * terminar mesmo se o jogador sair da tela no mesmo instante.
     */
    suspend fun recordRound(result: RoundResult, finalStaircase: StaircaseState) {
        appScope.async {
            progress.update { it.withResult(result, finalStaircase, LocalDate.now()) }
            if (result.completed) adPolicy.onRoundFinished()
        }.await()
    }
}
