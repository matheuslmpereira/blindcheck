plugins {
    kotlin("jvm")
    alias(libs.plugins.jetbrainsCompose)
    alias(libs.plugins.kotlin.compose)
}

dependencies {
    implementation(compose.desktop.currentOs)
    implementation(compose.material3)
}

compose.desktop {
    application {
        mainClass = "com.theustech.blindcheck_desktop.MainKt"
    }
}
