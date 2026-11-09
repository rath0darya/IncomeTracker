plugins {
 id("com.android.application")
 id("org.jetbrains.kotlin.android")
 id("org.jetbrains.kotlin.plugin.compose")
 id("com.google.devtools.ksp")
}
android {
 namespace = "com.anontracker.personal"
 compileSdk = 35
 defaultConfig { applicationId = "com.anontracker.personal"; minSdk = 26; targetSdk = 35; versionCode = 1; versionName = "1.0.0" }
 signingConfigs { create("release") { storeFile = file(System.getenv("KEYSTORE_PATH") ?: "release-keystore.jks"); storePassword = System.getenv("KEYSTORE_PASSWORD"); keyAlias = System.getenv("KEY_ALIAS"); keyPassword = System.getenv("KEY_PASSWORD") } }
 buildTypes { debug { applicationIdSuffix = ".debug" }; release { isMinifyEnabled = true; isShrinkResources = true; signingConfig = signingConfigs.getByName("release"); proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro") } }
 buildFeatures { compose = true }
 compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
 kotlinOptions { jvmTarget = "17" }
}
dependencies {
 implementation(platform("androidx.compose:compose-bom:2024.12.01"))
 implementation("androidx.activity:activity-compose:1.10.0")
 implementation("androidx.compose.ui:ui")
 implementation("androidx.compose.ui:ui-tooling-preview")
 implementation("androidx.compose.material3:material3")
 implementation("androidx.compose.material:material-icons-extended")
 implementation("androidx.room:room-runtime:2.6.1")
 implementation("androidx.room:room-ktx:2.6.1")
 ksp("androidx.room:room-compiler:2.6.1")
 implementation("com.google.zxing:core:3.5.3")
 debugImplementation("androidx.compose.ui:ui-tooling")
}
