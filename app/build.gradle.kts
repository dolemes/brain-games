plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.roborazzi)
}

// IDs de TESTE oficiais do AdMob: usados sempre no debug.
val testAdmobAppId = "ca-app-pub-3940256099942544~3347511713"
val testInterstitialId = "ca-app-pub-3940256099942544/1033173712"
val testRewardedId = "ca-app-pub-3940256099942544/5224354917"

fun prop(name: String): String = providers.gradleProperty(name).get()
fun env(name: String): String? = providers.environmentVariable(name).orNull

// Assinatura do release vem de variáveis de ambiente (segredos do GitHub no CI).
val keystorePath = env("KEYSTORE_FILE")

android {
    // O applicationId é DEFINITIVO depois do primeiro envio ao Google Play.
    namespace = "com.dolemes.braingames"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.dolemes.braingames"
        minSdk = 26
        targetSdk = 36
        versionCode = env("VERSION_CODE")?.toInt() ?: 1
        versionName = env("VERSION_NAME") ?: "0.1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("String", "PRIVACY_POLICY_URL", "\"${prop("privacyPolicyUrl")}\"")
    }

    signingConfigs {
        create("release") {
            if (keystorePath != null) {
                storeFile = file(keystorePath)
                storePassword = env("KEYSTORE_PASSWORD")
                keyAlias = env("KEY_ALIAS")
                keyPassword = env("KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        debug {
            manifestPlaceholders["admobAppId"] = testAdmobAppId
            buildConfigField("String", "ADMOB_APP_ID", "\"$testAdmobAppId\"")
            buildConfigField("String", "ADMOB_INTERSTITIAL_ID", "\"$testInterstitialId\"")
            buildConfigField("String", "ADMOB_REWARDED_ID", "\"$testRewardedId\"")
        }
        release {
            isMinifyEnabled = false
            manifestPlaceholders["admobAppId"] = prop("admobAppId")
            buildConfigField("String", "ADMOB_APP_ID", "\"${prop("admobAppId")}\"")
            buildConfigField("String", "ADMOB_INTERSTITIAL_ID", "\"${prop("admobInterstitialId")}\"")
            buildConfigField("String", "ADMOB_REWARDED_ID", "\"${prop("admobRewardedId")}\"")
            if (keystorePath != null) signingConfig = signingConfigs.getByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    // Gera a lista de idiomas para a escolha de idioma por app (Android 13+).
    androidResources {
        generateLocaleConfig = true
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }
}

dependencies {
    implementation(project(":core"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.kotlinx.coroutines.android)

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)
    implementation(libs.compose.ui.tooling.preview)
    debugImplementation(libs.compose.ui.tooling)
    debugImplementation(libs.compose.ui.test.manifest)

    // SDK de anúncios do Google (Next-Gen), recomendado desde julho de 2026.
    // Inclui o UMP (formulário de consentimento).
    implementation(libs.gma.next.gen)

    testImplementation(libs.junit)
    testImplementation(libs.androidx.test.ext.junit)
    testImplementation(libs.robolectric)
    testImplementation(platform(libs.compose.bom))
    testImplementation(libs.compose.ui.test.junit4)
    testImplementation(libs.roborazzi)
    testImplementation(libs.roborazzi.compose)
}

// O SDK Next-Gen não pode conviver com o SDK antigo de anúncios (classes duplicadas).
configurations.configureEach {
    exclude(group = "com.google.android.gms", module = "play-services-ads")
    exclude(group = "com.google.android.gms", module = "play-services-ads-lite")
}
