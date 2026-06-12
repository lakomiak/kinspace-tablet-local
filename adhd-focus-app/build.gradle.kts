import java.util.Properties

plugins {
    id("com.android.application")
    kotlin("android")
    kotlin("kapt")
    kotlin("plugin.serialization")
    id("com.google.dagger.hilt.android")
}

android {
    namespace = "com.adhdfocus.app"
    compileSdk = 36
    flavorDimensions += listOf("deviceTier", "deploymentMode")

    val releaseSigningPropsFile = rootProject.file("release-signing.properties")
    val releaseSigningProps = Properties().apply {
        if (releaseSigningPropsFile.exists()) {
            releaseSigningPropsFile.inputStream().use { load(it) }
        }
    }
    val hasReleaseSigning = releaseSigningPropsFile.exists()

    signingConfigs {
        if (hasReleaseSigning) {
            create("release") {
                storeFile = rootProject.file(
                    releaseSigningProps.getProperty("storeFile") ?: "release-keystore.jks"
                )
                storePassword = releaseSigningProps.getProperty("storePassword")
                keyAlias = releaseSigningProps.getProperty("keyAlias")
                keyPassword = releaseSigningProps.getProperty("keyPassword")
            }
        }
    }

    defaultConfig {
        applicationId = "com.adhdfocus.app"
        minSdk = 28
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"
        multiDexEnabled = true

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
        resourceConfigurations += listOf("en")
    }

    productFlavors {
        create("modern") {
            dimension = "deviceTier"
            minSdk = 28
        }
        create("legacy") {
            dimension = "deviceTier"
            minSdk = 22
            versionNameSuffix = "-legacy"
        }
        create("production") {
            dimension = "deploymentMode"
            buildConfigField("boolean", "ENABLE_KIOSK_MODE", "true")
        }
        create("audit") {
            dimension = "deploymentMode"
            applicationIdSuffix = ".audit"
            versionNameSuffix = "-audit"
            buildConfigField("boolean", "ENABLE_KIOSK_MODE", "false")
        }
    }

    buildTypes {
        debug {
            isDebuggable = true
            isMinifyEnabled = false
        }
        release {
            isMinifyEnabled = false
            isShrinkResources = false
            if (hasReleaseSigning) {
                signingConfig = signingConfigs.getByName("release")
            }
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled = true
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.10"
    }

    packagingOptions {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    // Version management
    val composeBomVersion = "2023.10.00"
    val roomVersion = "2.6.0"
    val hiltVersion = "2.48"
    val coroutinesVersion = "1.7.3"
    val coreKtxVersion = "1.12.0"
    val lifecycleVersion = "2.6.2"
    val activityComposeVersion = "1.8.0"
    val navigationComposeVersion = "2.7.4"
    val dataStoreVersion = "1.0.0"
    val junitVersion = "4.13.2"
    val junitExtVersion = "1.1.5"
    val espressoVersion = "3.5.1"
    val composeTestVersion = "1.5.4"
    val kotlinxSerializationVersion = "1.6.0"
    val kotestVersion = "5.7.2"

    // Jetpack Compose BOM for version management
    implementation(platform("androidx.compose:compose-bom:$composeBomVersion"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")

    // Jetpack Core
    implementation("androidx.core:core-ktx:$coreKtxVersion")
    implementation("androidx.core:core-splashscreen:1.0.1")
    implementation("androidx.multidex:multidex:2.0.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:$lifecycleVersion")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:$lifecycleVersion")
    implementation("androidx.activity:activity-compose:$activityComposeVersion")

    // Room Database
    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")
    kapt("androidx.room:room-compiler:$roomVersion")

    // Coroutines for async operations
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:$coroutinesVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutinesVersion")

    // Serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:$kotlinxSerializationVersion")

    // Dependency Injection - Hilt
    implementation("com.google.dagger:hilt-android:$hiltVersion")
    kapt("com.google.dagger:hilt-compiler:$hiltVersion")
    implementation("androidx.hilt:hilt-navigation-compose:1.1.0")
    implementation("javax.inject:javax.inject:1")

    // Navigation
    implementation("androidx.navigation:navigation-compose:$navigationComposeVersion")

    // Image loading
    implementation("io.coil-kt:coil-compose:2.5.0")

    // DataStore for preferences
    implementation("androidx.datastore:datastore-preferences:$dataStoreVersion")

    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.4")

    // Testing - Unit Tests
    testImplementation("junit:junit:$junitVersion")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:$coroutinesVersion")
    testImplementation("androidx.room:room-testing:$roomVersion")
    testImplementation("io.mockk:mockk:1.13.7")
    testImplementation("io.kotest:kotest-runner-junit5:$kotestVersion")
    testImplementation("io.kotest:kotest-property:$kotestVersion")

    // Testing - Instrumented Tests (Espresso)
    androidTestImplementation(platform("androidx.compose:compose-bom:$composeBomVersion"))
    androidTestImplementation("androidx.test.ext:junit:$junitExtVersion")
    androidTestImplementation("androidx.test.espresso:espresso-core:$espressoVersion")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4:$composeTestVersion")
    androidTestImplementation("io.mockk:mockk-android:1.13.7")

    // Debug dependencies
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
