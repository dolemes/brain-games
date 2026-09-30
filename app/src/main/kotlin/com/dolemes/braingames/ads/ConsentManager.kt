package com.dolemes.braingames.ads

import android.app.Activity
import android.content.Context
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform

/**
 * Consentimento de anúncios pelo UMP (a plataforma certificada do Google). O formulário só
 * aparece onde a lei exige (EEE, Reino Unido, Suíça); no Brasil o fluxo segue direto.
 * Para testar como se estivesse na Europa, use ConsentDebugSettings com DEBUG_GEOGRAPHY_EEA.
 */
class ConsentManager(context: Context) {
    private val info: ConsentInformation = UserMessagingPlatform.getConsentInformation(context)

    val canRequestAds: Boolean get() = info.canRequestAds()

    /** Quando true, as Configurações precisam oferecer "Privacidade dos anúncios". */
    val isPrivacyOptionsRequired: Boolean
        get() = info.privacyOptionsRequirementStatus ==
            ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED

    /** Atualiza o status e mostra o formulário se preciso. Deve rodar a cada abertura do app. */
    fun gather(activity: Activity, onDone: () -> Unit) {
        val params = ConsentRequestParameters.Builder().build()
        info.requestConsentInfoUpdate(
            activity,
            params,
            { UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { onDone() } },
            { onDone() },
        )
    }

    fun showPrivacyOptions(activity: Activity) {
        UserMessagingPlatform.showPrivacyOptionsForm(activity) { }
    }
}
