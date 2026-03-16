plugins {
    alias(libs.plugins.android.application)
    id("com.google.gms.google-services")
}

buildscript {
    dependencies {
        // Стабильная версия Android Gradle Plugin
        classpath("com.android.tools.build:gradle:7.4.2")
    }
}

android {
    namespace = "com.example.shop_exam"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.shop_exam"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}

dependencies {
    // Google Sign-In + Firebase Auth
    implementation("com.google.android.gms:play-services-auth:21.2.0")
    implementation("com.google.firebase:firebase-auth:22.3.1")
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.constraintlayout)
    implementation(libs.recyclerview)
    implementation("androidx.viewpager2:viewpager2:1.1.0")
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    // Retrofit для работы с сетью
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    // Gson конвертер для преобразования JSON в объекты
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")

    // Picasso для загрузки изображений
    implementation("com.squareup.picasso:picasso:2.71828")

    // Геолокация (Fused Location Provider)
    implementation("com.google.android.gms:play-services-location:21.3.0")
}