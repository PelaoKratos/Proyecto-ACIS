import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

val archivoFirma = rootProject.file("keystore.properties")
val datosFirma = Properties().apply {
    if (archivoFirma.isFile) archivoFirma.inputStream().use { load(it) }
}

android {
    namespace = "com.example.proyecto_acis"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.example.proyecto_acis"
        minSdk = 24
        targetSdk = 37
        versionCode = 2
        versionName = "1.1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        if (archivoFirma.isFile) {
            create("entrega") {
                fun dato(nombre: String): String = requireNotNull(datosFirma.getProperty(nombre)?.takeIf { it.isNotBlank() }) {
                    "Falta $nombre en keystore.properties"
                }
                storeFile = rootProject.file(dato("storeFile"))
                storePassword = dato("storePassword")
                keyAlias = dato("keyAlias")
                keyPassword = dato("keyPassword")
            }
        }
    }
    buildTypes {
        release {
            isDebuggable = false
            if (archivoFirma.isFile) signingConfig = signingConfigs.getByName("entrega")
            optimization {
                enable = true
                packageScope = setOf("androidx.**", "kotlin.**", "kotlinx.**")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }
    testOptions {
        unitTests.isIncludeAndroidResources = true
    }
    sourceSets {
        getByName("test").kotlin.directories += "src/sharedTest/java"
        getByName("androidTest").kotlin.directories += "src/sharedTest/java"
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.retrofit)
    implementation(libs.retrofit.gson)
    implementation(libs.okhttp)
    implementation(libs.room.runtime)
    implementation(libs.datastore.preferences)
    ksp(libs.room.compiler)
    testImplementation(libs.junit)
    testImplementation(libs.okhttp.mockwebserver)
    testImplementation(libs.robolectric)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
