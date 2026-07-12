plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "com.beatsandbeyond.video_editor"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.beatsandbeyond.video_editor"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    buildFeatures {
        viewBinding = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }


}

dependencies {
    // ── Core AndroidX ────────────────────────────────────────────────────────
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.fragment.ktx)
    implementation(libs.androidx.constraintlayout)

    // ── Lifecycle & ViewModel ─────────────────────────────────────────────────
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.livedata.ktx)

    // ── Navigation Component ──────────────────────────────────────────────────
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)

    // ── Dependency Injection (Hilt) ───────────────────────────────────────────
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.fragment)

    // ── Room Database ─────────────────────────────────────────────────────────
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // ── Coroutines ────────────────────────────────────────────────────────────
    implementation(libs.kotlinx.coroutines.android)

    // ── Image Loading ─────────────────────────────────────────────────────────
    implementation(libs.coil)
    implementation(libs.coil.video)

    // ── JSON Serialization ────────────────────────────────────────────────────
    implementation(libs.gson)

    // ── Testing ───────────────────────────────────────────────────────────────
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}

kotlin {
    jvmToolchain(11)
}