package com.dolemes.braingames.ads

import android.app.Activity
import android.content.Context

/**
 * Contrato de anúncios. O jogo só conhece esta interface; trocar de SDK (ou lançar uma versão
 * sem anúncios) não mexe em nenhum minijogo. Os callbacks chegam uma única vez, na thread
 * principal, inclusive quando o anúncio falha.
 */
interface AdService {
    fun initialize(context: Context)

    val isInterstitialReady: Boolean

    /** Mostra um intersticial; [onClosed] roda ao fechar, ou na hora se não houver anúncio. */
    fun showInterstitial(activity: Activity, onClosed: () -> Unit)

    val isRewardedReady: Boolean

    /** Mostra um premiado; `onFinished(true)` só se o jogador ganhou a recompensa. */
    fun showRewarded(activity: Activity, onFinished: (rewarded: Boolean) -> Unit)
}

/** Sem anúncios: para previews, testes e uma eventual versão paga. */
object NoAdService : AdService {
    override fun initialize(context: Context) = Unit

    override val isInterstitialReady: Boolean = false

    override fun showInterstitial(activity: Activity, onClosed: () -> Unit) = onClosed()

    override val isRewardedReady: Boolean = false

    override fun showRewarded(activity: Activity, onFinished: (rewarded: Boolean) -> Unit) = onFinished(false)
}
