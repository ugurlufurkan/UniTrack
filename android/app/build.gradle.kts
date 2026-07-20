import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

// Release imzalama bilgileri commit edilmeyen `keystore.properties`'ten okunur
// (bkz. keystore.properties.example + android/.gitignore). Dosya yoksa
// (ör. CI'da henüz kurulmadıysa) release signingConfig sessizce atlanır —
// derleme kırılmaz ama üretilen AAB imzasız kalır, bu da net bir uyarıyla
// aşağıda belirtiliyor.
val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties().apply {
    if (keystorePropertiesFile.exists()) {
        keystorePropertiesFile.inputStream().use { load(it) }
    }
}
val hasReleaseSigning = keystorePropertiesFile.exists()

if (!hasReleaseSigning) {
    logger.warn(
        "UYARI: keystore.properties bulunamadı — release build imzasız kalacak. " +
            "Play Console'a yüklemeden önce keystore.properties.example'ı kopyalayıp doldur."
    )
}

android {
    namespace = "com.unitrack.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.unitrack.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    signingConfigs {
        if (hasReleaseSigning) {
            create("release") {
                storeFile = rootProject.file(keystoreProperties.getProperty("storeFile"))
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        debug {
            // Emulator'ın host makineye baktığı özel adres — sadece debug'da.
            buildConfigField("String", "BASE_URL", "\"http://10.0.2.2:5000/api/v1/\"")
            // TODO: admin-panel'i deploy edince gerçek domain ile değiştir.
            buildConfigField("String", "WEB_BASE_URL", "\"http://10.0.2.2:3000\"")
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            if (hasReleaseSigning) {
                signingConfig = signingConfigs.getByName("release")
            }
            // TODO: backend prod'a deploy edilince gerçek domain ile değiştir.
            // Bu bir sır değil (public API adresi), o yüzden burada sabit kalabilir.
            buildConfigField("String", "BASE_URL", "\"https://api.unitrack.app/api/v1/\"")
            // admin-panel'in yayınlandığı domain — Ayarlar ekranındaki "Gizlilik
            // Politikası" / "Hesabımı Sil (web)" linkleri buraya /legal/... ekleyerek
            // açılıyor (bkz. util/AppLinks.kt). admin-panel deploy edilince güncelle.
            buildConfigField("String", "WEB_BASE_URL", "\"https://unitrack.app\"")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    kotlin {
        jvmToolchain(21)
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.splashscreen)

    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.ui.text.google.fonts)
    implementation(libs.lottie.compose)

    implementation(libs.navigation.compose)
    implementation(libs.lifecycle.viewmodel.compose)

    implementation(libs.retrofit)
    implementation(libs.retrofit.kotlinx)

    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)

    implementation(libs.kotlinx.serialization)

    implementation(libs.hilt.android)
    implementation(libs.hilt.navigation.compose)

    implementation(libs.datastore)

    implementation(libs.coil.compose)

    implementation(libs.security.crypto)

    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.hilt.work)
    ksp(libs.androidx.hilt.compiler)

    // Home screen widget (İstatistik/GPA özeti) — Compose tabanlı Glance API'si,
    // projenin geri kalanıyla aynı UI paradigmasını kullanıyor.
    implementation(libs.androidx.glance.appwidget)
    implementation(libs.androidx.glance.material3)

    ksp(libs.hilt.compiler)

    testImplementation(libs.junit)

    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)

    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
// Google Credential Manager (Modern Google Girişi)
    implementation(libs.credentials)
    implementation(libs.credentials.play.auth)
    implementation(libs.googleid)
}