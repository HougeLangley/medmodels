import java.util.Properties
import java.io.FileInputStream

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

// 加载签名配置（keystore.properties 不入库——.gitignore 排除）
val keystoreProperties = Properties().apply {
    val f = rootProject.file("keystore.properties")
    if (f.exists()) load(FileInputStream(f))
}

android {
    namespace = "com.hermes.medmodels"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.hermes.medmodels"
        minSdk = 26
        targetSdk = 36
        versionCode = 11
        versionName = "0.10.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            val ksFile = rootProject.file(keystoreProperties.getProperty("storeFile") ?: "")
            if (keystoreProperties.getProperty("storeFile") != null && ksFile.exists()) {
                signingConfig = signingConfigs.create("release") {
                    storeFile = ksFile
                    storePassword = keystoreProperties.getProperty("storePassword")
                    keyAlias = keystoreProperties.getProperty("keyAlias")
                    keyPassword = keystoreProperties.getProperty("keyPassword")
                }
            }
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
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2026.01.00")
    implementation(composeBom)
    implementation("androidx.core:core-ktx:1.17.0")
    implementation("androidx.activity:activity-compose:1.11.0")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.9.4")

    testImplementation("junit:junit:4.13.2")

    debugImplementation("androidx.compose.ui:ui-tooling")
}
