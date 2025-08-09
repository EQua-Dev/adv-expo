import org.gradle.api.publish.maven.MavenPublication

plugins {
    id("com.android.library") version "8.11.1"
    id("org.jetbrains.kotlin.android") version "2.0.21"
    id("kotlin-parcelize")
    id("maven-publish")
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "tech.sourceid.sid_address_verification"
    compileSdk = 36

    defaultConfig {
        minSdk = 24

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildFeatures {
        compose = true
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

    publishing {
        singleVariant("debug") {
            withSourcesJar()
            withJavadocJar()
        }
        singleVariant("release") {
            withSourcesJar()
            withJavadocJar()
        }
    }
    // Add publishing configuration for Android libraries
//    publishing {
//        singleVariant("release") {
//            withSourcesJar()
//            withJavadocJar()
//        }
//    }
}

// Publishing configuration - afterEvaluate is necessary for Android libraries
afterEvaluate {
    publishing {
        publications {
            create<MavenPublication>("release") {
                from(components["release"])
//                groupId = "com.github.sourceidtechorg"
                groupId = "com.github.EQua-Dev"
                artifactId = "adv-expo"
                version = "1.0.1"

                pom {
                    name.set("SIDAddressVerification")
                    description.set("A SourceID native Android library for verifying addresses using Google Places API and Location Services.")
//                    url.set("https://github.com/sourceidtechorg/sid-address-verification-android")
                    url.set("https://github.com/EQua-Dev/adv-expo")

                    licenses {
                        license {
                            name.set("MIT License")
                            url.set("https://opensource.org/licenses/MIT")
                        }
                    }

                    developers {
                        developer {
                            id.set("richard-sid")
                            name.set("Richard Uzor")
                            email.set("richard@sourceid.tech")
                        }
                    }

                    scm {
                        connection.set("scm:git:git://github.com/sourceidtechorg/sid-address-verification-android.git")
                        developerConnection.set("scm:git:ssh://github.com/sourceidtechorg/sid-address-verification-android.git")
                        url.set("https://github.com/EQua-Dev/adv-expo")
//                        url.set("https://github.com/sourceidtechorg/sid-address-verification-android")
                    }
                }
            }
        }
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")

    // Compose BOM for version alignment
    implementation(platform("androidx.compose:compose-bom:2024.02.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.activity:activity-compose:1.9.0")

    // Debug dependencies
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    // Google Places
    api("com.google.android.libraries.places:places:3.3.0")

    // Location Services
    api("com.google.android.gms:play-services-location:21.0.1")

    // Lifecycle
    api("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    api("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")

    // Coroutines
    api("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    api("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    api("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.7.3")

    // Retrofit + OkHttp (exposed to consuming app)
    api("com.squareup.retrofit2:retrofit:2.9.0")
    api("com.squareup.retrofit2:converter-gson:2.9.0")
    api("com.squareup.okhttp3:okhttp:4.12.0")
    api("com.squareup.okhttp3:logging-interceptor:4.12.0")
}
