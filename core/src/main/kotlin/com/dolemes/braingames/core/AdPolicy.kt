package com.dolemes.braingames.core

/** Regras de frequência dos intersticiais. */
data class AdPolicySettings(
    /** Novatos não veem anúncio: nas primeiras sessões o jogador decide se fica. */
    val minCompletedSessionsBeforeFirstAd: Int = 3,
    /** Rodadas concluídas entre um intersticial e outro. */
    val roundsBetweenInterstitials: Int = 2,
    /** Intervalo mínimo entre intersticiais, em segundos de relógio monotônico. */
    val minSecondsBetweenInterstitials: Double = 180.0,
)

/**
 * Decide SE um intersticial pode aparecer. ONDE ele aparece é decisão da tela de jogo: só no
 * resultado, depois do toque em "Continuar" — nunca na abertura do app, no início ou no meio
 * de uma rodada. O estado fica em memória e recomeça a cada abertura do app.
 */
class AdPolicy(private val settings: AdPolicySettings = AdPolicySettings()) {
    private var roundsSinceLastInterstitial = 0
    private var lastInterstitialSeconds: Double? = null

    fun shouldShowInterstitial(completedSessions: Int, nowSeconds: Double): Boolean {
        if (completedSessions < settings.minCompletedSessionsBeforeFirstAd) return false
        if (roundsSinceLastInterstitial < settings.roundsBetweenInterstitials) return false
        val last = lastInterstitialSeconds
        return last == null || nowSeconds - last >= settings.minSecondsBetweenInterstitials
    }

    fun onRoundFinished() {
        roundsSinceLastInterstitial++
    }

    fun onInterstitialShown(nowSeconds: Double) {
        roundsSinceLastInterstitial = 0
        lastInterstitialSeconds = nowSeconds
    }
}
