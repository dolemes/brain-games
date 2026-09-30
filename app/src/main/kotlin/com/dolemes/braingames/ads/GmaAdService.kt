package com.dolemes.braingames.ads

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import com.google.android.libraries.ads.mobile.sdk.MobileAds
import com.google.android.libraries.ads.mobile.sdk.common.AdRequest
import com.google.android.libraries.ads.mobile.sdk.common.FullScreenContentError
import com.google.android.libraries.ads.mobile.sdk.common.PreloadConfiguration
import com.google.android.libraries.ads.mobile.sdk.initialization.InitializationConfig
import com.google.android.libraries.ads.mobile.sdk.interstitial.InterstitialAdEventCallback
import com.google.android.libraries.ads.mobile.sdk.interstitial.InterstitialAdPreloader
import com.google.android.libraries.ads.mobile.sdk.rewarded.RewardedAdEventCallback
import com.google.android.libraries.ads.mobile.sdk.rewarded.RewardedAdPreloader
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Anúncios pelo Google Mobile Ads Next-Gen SDK, com pré-carregamento: o SDK mantém um anúncio
 * pronto e busca o próximo sozinho depois de cada exibição. Os callbacks do SDK podem vir de
 * outra thread; aqui eles são repassados para a thread principal.
 */
class GmaAdService(
    private val appId: String,
    private val interstitialUnitId: String,
    private val rewardedUnitId: String,
) : AdService {

    private val initStarted = AtomicBoolean(false)
    @Volatile private var preloading = false
    private val mainHandler = Handler(Looper.getMainLooper())
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun initialize(context: Context) {
        if (initStarted.getAndSet(true)) return
        val appContext = context.applicationContext
        // Precisa rodar fora da thread principal; na principal pode travar o app (ANR).
        scope.launch {
            MobileAds.initialize(appContext, InitializationConfig.Builder(appId).build()) {
                InterstitialAdPreloader.start(
                    interstitialUnitId,
                    PreloadConfiguration(AdRequest.Builder(interstitialUnitId).build()),
                )
                RewardedAdPreloader.start(
                    rewardedUnitId,
                    PreloadConfiguration(AdRequest.Builder(rewardedUnitId).build()),
                )
                preloading = true
            }
        }
    }

    override val isInterstitialReady: Boolean
        get() = preloading && InterstitialAdPreloader.isAdAvailable(interstitialUnitId)

    override val isRewardedReady: Boolean
        get() = preloading && RewardedAdPreloader.isAdAvailable(rewardedUnitId)

    override fun showInterstitial(activity: Activity, onClosed: () -> Unit) {
        val ad = if (preloading) InterstitialAdPreloader.pollAd(interstitialUnitId) else null
        if (ad == null) {
            onClosed()
            return
        }
        val done = AtomicBoolean(false)
        fun finish() {
            if (!done.getAndSet(true)) mainHandler.post { onClosed() }
        }
        ad.adEventCallback = object : InterstitialAdEventCallback {
            override fun onAdDismissedFullScreenContent() = finish()

            override fun onAdFailedToShowFullScreenContent(fullScreenContentError: FullScreenContentError) = finish()
        }
        ad.show(activity)
    }

    override fun showRewarded(activity: Activity, onFinished: (rewarded: Boolean) -> Unit) {
        val ad = if (preloading) RewardedAdPreloader.pollAd(rewardedUnitId) else null
        if (ad == null) {
            onFinished(false)
            return
        }
        val earned = AtomicBoolean(false)
        val done = AtomicBoolean(false)
        // Algumas redes avisam a recompensa depois do fechamento: espera um instante.
        fun finish() {
            if (!done.getAndSet(true)) mainHandler.postDelayed({ onFinished(earned.get()) }, REWARD_GRACE_MS)
        }
        ad.adEventCallback = object : RewardedAdEventCallback {
            override fun onAdDismissedFullScreenContent() = finish()

            override fun onAdFailedToShowFullScreenContent(fullScreenContentError: FullScreenContentError) = finish()
        }
        ad.show(activity) { earned.set(true) }
    }

    private companion object {
        const val REWARD_GRACE_MS = 400L
    }
}
