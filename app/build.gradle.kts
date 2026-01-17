plugins {
    id("com.android.application")
}

android {
    namespace = "com.example.brokerfi"
    compileSdk = 33

    defaultConfig {
        applicationId = "com.example.brokerfi"
        minSdk = 21
        targetSdk = 33
        versionCode = 1
        versionName = "2.0.0"

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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}

dependencies {

    //noinspection GradleCompatible
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.8.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    implementation("com.google.zxing:core:3.5.1")
    implementation("com.journeyapps:zxing-android-embedded:4.3.0") {
        exclude(group = "com.android.support", module = "support-v4")
    }
    implementation ("com.squareup.okhttp3:okhttp:4.9.3")
    implementation("org.bouncycastle:bcpkix-jdk15on:1.70") // 根据需要选择版本号
    implementation("org.bouncycastle:bcprov-jdk15on:1.70") // 根据需要选择版本号
    implementation ("com.google.code.gson:gson:2.10.1")


//    implementation("androidx.activity:activity-ktx:1.7.2")
//    implementation("androidx.fragment:fragment-ktx:1.5.7")
    implementation("com.github.bumptech.glide:glide:4.16.0")
//    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("org.web3j:core:4.9.8")
    implementation("androidx.exifinterface:exifinterface:1.3.6")
    
    // OpenCV for document scanning - using a manual approach
    // Note: OpenCV Android SDK needs to be downloaded manually and added as a module
    // For now, we'll implement a simplified version without OpenCV
}