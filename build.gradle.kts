// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    id("com.android.library") version libs.versions.agp.get() apply false
    id("org.jetbrains.kotlin.android") version libs.versions.kotlin.get() apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.google.devtools.ksp) apply false
    alias(libs.plugins.jetbrains.kotlin.plugin.serialization) apply false
    alias(libs.plugins.googleServicesPlugin) apply false
    alias(libs.plugins.hilt) apply false
}