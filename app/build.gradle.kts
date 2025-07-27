plugins {
    alias(libs.plugins.android.application)
    id("com.google.gms.google-services")
}

android {
    namespace = "com.app.selectifyai"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.app.selectifyai"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        ndk {
            abiFilters += listOf("armeabi-v7a", "arm64-v8a")
        }

        externalNativeBuild {
            cmake {} // Burayı boş bırak çünkü `cppFlags` burada tanımlanamaz.
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

    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = "3.22.1"
        }
    }

    sourceSets {
        getByName("main") {
            jniLibs.srcDirs("src/main/jniLibs")
        }
    }
    
    lint {
        baseline = file("lint-baseline.xml")
    }
}
dependencies {
    implementation("com.startapp:inapp-sdk:5.2.3")// son sürüm
    implementation("com.google.android.gms:play-services-ads:23.1.0")
// En güncel sürüm kontrol edilmeli
    implementation("com.google.firebase:firebase-storage:20.3.0")
    implementation("com.android.volley:volley:1.2.1")
    implementation("com.github.bumptech.glide:glide:4.16.0")
    implementation(libs.play.services.location)
    implementation(libs.recyclerview)
    annotationProcessor("com.github.bumptech.glide:compiler:4.16.0")
    implementation("com.airbnb.android:lottie:6.3.0")
    implementation("com.google.firebase:firebase-auth:22.0.0")
    implementation("com.google.android.gms:play-services-auth:20.6.0")
    implementation("com.google.firebase:firebase-analytics")
    implementation(platform("com.google.firebase:firebase-bom:33.16.0"))
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.firebase.firestore)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    implementation("androidx.cardview:cardview:1.0.0")
}