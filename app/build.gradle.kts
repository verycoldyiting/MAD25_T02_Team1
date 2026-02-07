import java.util.Properties

val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")

if (localPropertiesFile.exists()) {
    localProperties.load(localPropertiesFile.inputStream())
    println("✅ Gradle Found local.properties!")
} else {
    println("❌ Gradle could NOT find local.properties at: ${localPropertiesFile.absolutePath}")
}

val apiKey = localProperties.getProperty("MY_API_KEY") ?: ""
val googleMapsKey = localProperties.getProperty("GOOGLE_MAPS_KEY") ?: ""
println("🔑 Key seen by Gradle: '$googleMapsKey'")

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.jetbrainsKotlinAndroid)
    alias(libs.plugins.kotlinCompose)
    alias(libs.plugins.googleGmsGoogleServices)
    alias(libs.plugins.kotlinSerialization)
}



android {
    namespace = "sg.edu.np.mad.mad25_t02_team1"
    compileSdk = 36

    buildFeatures {
        compose = true
        buildConfig = true
    }


    defaultConfig {
        applicationId = "sg.edu.np.mad.mad25_t02_team1"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
        buildConfigField(
            "String",
            "MY_API_KEY",
            "\"${localProperties.getProperty("MY_API_KEY")}\""
        )

        buildConfigField(
            "String",
            "STRIPE_PUBLISHABLE_KEY",
            "\"${localProperties.getProperty("STRIPE_PUBLISHABLE_KEY")}\""
        )


        manifestPlaceholders["MAPS_API_KEY"] = googleMapsKey
        buildConfigField("String", "MAPS_API_KEY", "\"$googleMapsKey\"")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.1"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}


dependencies {


    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.10.1")


    // Compose / AndroidX
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)

    // Firebase (use ONLY Firebase BOM style)
    implementation(platform("com.google.firebase:firebase-bom:32.7.0"))
    implementation("com.google.firebase:firebase-analytics-ktx")
    implementation("com.google.firebase:firebase-firestore-ktx")
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.firebase:firebase-storage-ktx")
    implementation("com.google.firebase:firebase-functions-ktx")

    // Others
    implementation("com.airbnb.android:lottie-compose:6.0.0")
    implementation("io.coil-kt:coil-compose:2.6.0")
    implementation("androidx.compose.material:material-icons-extended:1.6.0")
    implementation("androidx.navigation:navigation-compose:2.7.7")
    implementation("org.apache.commons:commons-text:1.10.0")
    implementation("com.google.zxing:core:3.5.2")
    implementation("androidx.compose.material:material-icons-extended")
    implementation(libs.smart.reply.common)

    // Stripe
    implementation("com.stripe:stripe-android:22.6.1")

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
    implementation("androidx.biometric:biometric:1.1.0")
    implementation("androidx.biometric:biometric:1.1.0")

    // Google Maps
    implementation("com.google.maps.android:maps-compose:4.4.1")
    implementation("com.google.android.gms:play-services-maps:18.2.0")
    implementation("com.google.android.gms:play-services-location:21.0.1")
    implementation("com.google.android.libraries.places:places:3.5.0")
}







//dependencies {
//    // Force Kotlin + Coroutines versions to match Kotlin 1.9.x
//    implementation(platform("org.jetbrains.kotlin:kotlin-bom:1.9.0"))
//    implementation(platform("org.jetbrains.kotlinx:kotlinx-coroutines-bom:1.7.3"))
//
//    // Coroutines Task await()
//    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services")
//
//    implementation(libs.androidx.core.ktx)
//    implementation(libs.androidx.lifecycle.runtime.ktx)
//    implementation(libs.androidx.activity.compose)
//    implementation(platform(libs.androidx.compose.bom))
//    implementation(libs.androidx.ui)
//    implementation(libs.androidx.ui.graphics)
//    implementation(libs.androidx.ui.tooling.preview)
//    implementation(libs.androidx.material3)
//    //implementation(libs.firebase.firestore)
//    //implementation(libs.firebase.auth)
//    //implementation(libs.firebase.storage)
//    //implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.7.3")
//    implementation("com.airbnb.android:lottie-compose:6.0.0")
//    implementation(platform("com.google.firebase:firebase-bom:32.7.0"))
//    implementation("com.google.firebase:firebase-analytics")
//    implementation("com.google.firebase:firebase-firestore")
//    implementation("com.google.firebase:firebase-auth")
//    implementation("com.google.firebase:firebase-storage")
//
//
//
//    //implementation("io.coil-kt:coil-compose:2.6.0")
//    implementation("androidx.compose.material:material-icons-extended:1.6.0")
//    implementation("androidx.navigation:navigation-compose:2.7.7")
//
//    implementation("org.apache.commons:commons-text:1.10.0")
//
//
//
//    //implementation("androidx.compose.material:material-icons-extended")
//    implementation("com.google.zxing:core:3.5.2")
//    implementation(libs.androidx.navigation.compose)
//    implementation(libs.smart.reply.common)
//
//    implementation("androidx.compose.material3:material3-adaptive-navigation-suite-android:1.3.0")
//
//
//    implementation("io.coil-kt:coil-compose:2.6.0")
//
//    //Stripe
//    implementation("com.stripe:stripe-android:22.6.1")
//    implementation("com.google.firebase:firebase-functions-ktx")
//    implementation("com.google.firebase:firebase-analytics-ktx")
//    implementation("com.google.firebase:firebase-firestore-ktx")
//    implementation("com.google.firebase:firebase-auth-ktx")
//    implementation("com.google.firebase:firebase-storage-ktx")
//
//
//
//
//
//
//
//
//
//    testImplementation(libs.junit)
//    androidTestImplementation(libs.androidx.junit)
//    androidTestImplementation(libs.androidx.espresso.core)
//    androidTestImplementation(platform(libs.androidx.compose.bom))
//    androidTestImplementation(libs.androidx.ui.test.junit4)
//    debugImplementation(libs.androidx.ui.tooling)
//    debugImplementation(libs.androidx.ui.test.manifest)
//}