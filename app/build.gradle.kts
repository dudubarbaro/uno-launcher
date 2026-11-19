plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "io.github.dudubarbaro.unolauncher"
    compileSdk = 35
    buildToolsVersion = "35.0.0"

    defaultConfig {
        applicationId = "io.github.dudubarbaro.unolauncher"
        minSdk = 26
        targetSdk = 35
        versionCode = 5
        versionName = "0.2.3"
        // Contato exigido pelo met.no; vem de ~/.gradle/gradle.properties, nunca do repositório.
        buildConfigField("String", "MET_CONTACT", "\"${findProperty("unoContact") ?: ""}\"")
    }

    // Chave do dono, fora do repositório (~/.gradle/gradle.properties). Sem ela o release sai sem assinatura.
    if (project.hasProperty("unoKeystore")) {
        signingConfigs.create("uno") {
            storeFile = file(property("unoKeystore") as String)
            storePassword = property("unoKeystorePassword") as String
            keyAlias = property("unoKeyAlias") as String
            keyPassword = property("unoKeyPassword") as String
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false // ponytail: sem R8; ligar se o tamanho do APK incomodar
            signingConfigs.findByName("uno")?.let { signingConfig = it }
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    testOptions {
        unitTests.isIncludeAndroidResources = true
        unitTests.all { it.systemProperty("roborazzi.test.record", "true") }
    }
}

kotlin { jvmToolchain(17) }

dependencies {
    val bom = platform("androidx.compose:compose-bom:2024.12.01")
    implementation(bom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.core:core-ktx:1.15.0")

    testImplementation(bom)
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")
    testImplementation("org.json:json:20240303")
    testImplementation("org.robolectric:robolectric:4.14.1")
    testImplementation("io.github.takahirom.roborazzi:roborazzi:1.36.0")
    testImplementation("io.github.takahirom.roborazzi:roborazzi-compose:1.36.0")
    testImplementation("androidx.compose.ui:ui-test-junit4")
    testImplementation("androidx.test:core:1.6.1")
    debugImplementation(bom)
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
