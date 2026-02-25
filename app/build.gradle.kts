plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.a4_2_food"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.a4_2_food"
        minSdk = 24
        targetSdk = 36
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
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    implementation("androidx.viewpager2:viewpager2:1.1.0")
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")

    // Picasso для загрузки изображений
    implementation("com.squareup.picasso:picasso:2.71828")
    // Gson конвертер для преобразования JSON в объекты
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    // Retrofit для работы с сетью
    implementation("com.squareup.retrofit2:retrofit:2.9.0")

}