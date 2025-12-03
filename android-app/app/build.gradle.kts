plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.ksp)

    id("org.jetbrains.kotlin.plugin.serialization") version "1.9.0"
    id("com.google.gms.google-services")
}

android {
    namespace = "com.thomasalfa.photobooth"
    compileSdk = 35 // REKOMENDASI: Pakai 35 (Stable) jangan 36 dulu

    defaultConfig {
        applicationId = "com.thomasalfa.photobooth"
        minSdk = 27
        targetSdk = 33
        versionCode = 1
        versionName = "0.1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }

        // PENTING: Huawei MatePad & USB Camera butuh ndk support kadang
        ndk {
            abiFilters.add("armeabi-v7a")
            abiFilters.add("arm64-v8a")
        }
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

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        viewBinding = true // Wajib untuk libausbc
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"

            excludes += "META-INF/INDEX.LIST"
            excludes += "META-INF/io.netty.versions.properties"
        }
    }
}

dependencies {
    // Core
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)

    implementation("com.google.zxing:core:3.5.2")

    implementation("io.ktor:ktor-server-core:2.3.7")
    implementation("io.ktor:ktor-server-netty:2.3.7")

    // Compose BOM (Magic happens here)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)

    // Navigation & ViewModel
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation("androidx.datastore:datastore-preferences:1.0.0")

    // Image Loading (Penting buat Photobooth)
    implementation(libs.coil.compose)
    implementation("io.coil-kt:coil-gif:2.5.0")

    // Camera Library (Exclude Strategy kamu KEREN!)
    implementation(libs.ausbc) {
        exclude(group = "com.gyf.immersionbar", module = "immersionbar")
        exclude(group = "com.zlc.glide", module = "webpdecoder")
        // Tambahan preventif jika ada conflict lifecycle
        exclude(group = "androidx.lifecycle", module = "lifecycle-viewmodel-ktx")
    }
    implementation(libs.androidx.compose.foundation)
    implementation(libs.material)

    implementation("androidx.core:core-splashscreen:1.0.1")

    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.airbnb.android:lottie-compose:6.1.0")
    implementation(libs.androidx.compose.runtime.saveable)
    implementation(libs.androidx.foundation)
    implementation(libs.androidx.compose.animation)

    implementation("org.jcodec:jcodec:0.2.5")
    implementation("org.jcodec:jcodec-android:0.2.5")
    implementation("androidx.compose.material:material-icons-extended:1.7.8")

    val room_version = "2.6.1"
    implementation("androidx.room:room-runtime:$room_version")
    implementation("androidx.room:room-ktx:$room_version")
    ksp("androidx.room:room-compiler:$room_version")

    implementation(platform("io.github.jan-tennert.supabase:bom:3.0.0"))

    // Modul (Perhatikan akhiran -kt)
    implementation("io.github.jan-tennert.supabase:postgrest-kt")
    implementation("io.github.jan-tennert.supabase:storage-kt")

    // Serializer (Wajib untuk Supabase)
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
    implementation("io.ktor:ktor-client-core:3.0.0")
    implementation("io.ktor:ktor-client-cio:3.0.0")
    // Test
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}