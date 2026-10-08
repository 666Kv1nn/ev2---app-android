plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.webservicessencillo"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.example.webservicessencillo"
        minSdk = 26
        targetSdk = 37
        versionCode = 2
        versionName = "2.0"

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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.recyclerview)
    implementation(libs.androidx.swiperefreshlayout)
    implementation(libs.volley)
    implementation(libs.androidx.security.crypto)
    implementation(libs.mpandroidchart)
    testImplementation(libs.junit)
}
