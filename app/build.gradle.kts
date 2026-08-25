plugins {
    alias(libs.plugins.android.application)
    // Renders the store screenshots and feature graphic from the app's own
    // Compose UI, so the listing art carries whatever branding the build has.
    id("io.github.lucianosantosdev.storescreenshots")
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hotswan.compiler)
}

// Signing comes from the environment so the keystore never lives in the repo.
// All of it absent is the normal case for a local debug build, and release then
// stays unsigned rather than failing the build.
val keystorePath = providers.environmentVariable("ANDROID_KEYSTORE_PATH").orNull
val keystorePassword = providers.environmentVariable("ANDROID_KEYSTORE_PASSWORD").orNull
val signingKeyAlias = providers.environmentVariable("ANDROID_KEY_ALIAS").orNull
val signingKeyPassword = providers.environmentVariable("ANDROID_KEY_PASSWORD").orNull
val hasSigning = !keystorePath.isNullOrBlank() && file(keystorePath).exists()

// Play rejects a bundle whose versionCode it has seen before, so CI overrides
// these per release. Each white-label client is its own Play app with its own
// sequence, which is why this is passed in rather than derived from the repo.
val buildVersionCode = (providers.gradleProperty("versionCode").orNull)?.toInt() ?: 1
val buildVersionName = providers.gradleProperty("versionName").orNull ?: "1.0"

android {
    namespace = "io.flavorflow.demo"
    compileSdk = 37

    defaultConfig {
        applicationId = "io.flavorflow.demo"
        minSdk = 24
        versionCode = buildVersionCode
        versionName = buildVersionName

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Firebase project that serves every white-label build's menu. All clients
        // share one project; each reads its own tenant keyed by applicationId.
        buildConfigField("String", "FIREBASE_PROJECT_ID", "\"flavorflow-digitalmenu\"")
        // Optional Firebase Web API key; empty relies on public-read security rules.
        buildConfigField("String", "FIRESTORE_API_KEY", "\"\"")
    }

    signingConfigs {
        if (hasSigning) {
            create("release") {
                storeFile = file(keystorePath!!)
                storePassword = keystorePassword
                keyAlias = signingKeyAlias
                keyPassword = signingKeyPassword
            }
        }
    }

    buildTypes {
        release {
            // findByName, not getByName: null simply means "unsigned", which is
            // what a developer building release locally should get.
            signingConfig = signingConfigs.findByName("release")
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.core)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.coil.compose)
    implementation(libs.coil.network)
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.hotswan.preview)
}

storeScreenshots {
    // Fastlane's layout, so the same folder can feed an upload either way.
    destDir = layout.projectDirectory.dir("screenshots")
}
