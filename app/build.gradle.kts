@file:Suppress("UnstableApiUsage")

import org.jetbrains.kotlin.konan.properties.Properties

plugins {
    alias(libs.plugins.android.application)
}

val localProperties = Properties()
if (rootProject.file("local.properties").canRead())
    localProperties.load(rootProject.file("local.properties").inputStream())

android {
    namespace = gropify.project.app.packageName
    compileSdk = gropify.project.android.compileSdk

    val universal = localProperties.getProperty("androidStoreFile")?.let {
        signingConfigs.create("universal") {
            storeFile = file(it)
            storePassword = localProperties.getProperty("androidStorePassword")
            keyAlias = localProperties.getProperty("androidKeyAlias")
            keyPassword = localProperties.getProperty("androidKeyPassword")
            enableV1Signing = true
            enableV2Signing = true
        }
    }
    defaultConfig {
        applicationId = gropify.project.app.packageName
        minSdk = gropify.project.android.minSdk
        targetSdk = gropify.project.android.targetSdk
        versionName = gropify.project.app.versionName
        versionCode = gropify.project.app.versionCode
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    buildTypes {
        all { signingConfig = universal ?: signingConfigs["debug"] }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        buildConfig = true
        viewBinding = true
    }
    lint { checkReleaseBuilds = false }
    androidResources.additionalParameters += listOf("--allow-reserved-package-id", "--package-id", "0x37")
}

androidComponents {
    onVariants(selector().all()) {
        it.outputs.forEach { output ->
            val currentType = it.buildType
            val currentSuffix = gropify.github.ci.commit.id.let { suffix ->
                if (!suffix.isBlank()) "-$suffix" else ""
            }
            val currentVersion = "${output.versionName.get()}$currentSuffix(${output.versionCode.get()})"
            if (output is com.android.build.api.variant.impl.VariantOutputImpl)
                output.outputFileName.set("${gropify.project.name}-v$currentVersion-$currentType.apk")
        }
    }
}

dependencies {
    compileOnly(libs.libxposed.api)
    implementation(libs.libxposed.service)
    implementation(platform(libs.kavaref.bom))
    implementation(libs.kavaref.core)
    implementation(libs.kavaref.android)
    implementation(libs.kavaref.extension)
    implementation(libs.betterandroid.ui.extension)
    implementation(libs.anip.sdk)
    implementation(libs.libsu)
    implementation(libs.drawabletoolbox)
    implementation(libs.okhttp)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.espresso.core)
}