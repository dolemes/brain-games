// O AGP 9 compila Kotlin sozinho ("built-in Kotlin"). Declarar o plugin Kotlin aqui, sem
// aplicar, fixa a versão do Kotlin usada por todos os módulos (a do libs.versions.toml).
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.roborazzi) apply false
}
