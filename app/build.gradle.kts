plugins {
    alias(libs.plugins.android.application)
    id("com.google.gms.google-services")
}

android {
    namespace = "br.com.meu_controle"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "br.com.meu_controle"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            storeFile = file("keystore/meu-controle-release.jks")
            storePassword = "meucontrole2026"
            keyAlias = "meu-controle"
            keyPassword = "meucontrole2026"
        }
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
            optimization {
                enable = false
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    // Necessário para os testes unitários que criam exceções do Firebase
    // (o construtor delas usa TextUtils, que não existe na JVM)
    testOptions {
        unitTests {
            isReturnDefaultValues = true
        }
    }
}

dependencies {
    // Interface
    implementation(libs.activity.ktx)
    implementation(libs.appcompat)
    implementation(libs.constraintlayout)
    implementation(libs.material)
    implementation(libs.recyclerview)

    // Arquitetura (ViewModel e LiveData)
    implementation(libs.lifecycle.viewmodel)
    implementation(libs.lifecycle.livedata)

    // Banco local (Room)
    implementation(libs.room.runtime)
    annotationProcessor(libs.room.compiler)

    // Firebase (a versão de cada biblioteca vem do BOM)
    implementation(platform("com.google.firebase:firebase-bom:35.1.0"))
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-firestore")

    // Testes unitários (pasta src/test, rodam na JVM)
    testImplementation(libs.junit)
    testImplementation("junit:junit:4.13.2")
    // Só se for testar o parser de feriados (org.json é só um stub na JVM):
    // testImplementation("org.json:json:20231013")

    // Testes instrumentados (pasta src/androidTest, rodam no emulador)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}