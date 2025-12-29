# SID Address Verification Android SDK

Native Android library for real-time address verification with background location tracking.

---

## Before You Begin

Complete the following steps on the **Source ID platform** before integrating the SDK:

### 1. Create a Customer

Log into the Source ID dashboard and create a **customer profile**. You will receive your **API key**, **customer ID**, and other required credentials.

### 2. Collect User Address Details

Collect the user’s address in your Android app and send it to Source ID via your backend using the provided REST API.

### 3. Invoke the Android SDK

After submitting the user’s address to Source ID, you can begin tracking and verification inside your Android app.

---

## Installation (via JitPack)

Add JitPack to your root `settings.gradle` or `build.gradle` repositories:

```gradle
repositories {
    mavenCentral()
    maven { url "https://jitpack.io" }
}
```

Add the dependency to your **app-level** `build.gradle`:

```gradle
dependencies {
    implementation "com.github.sourceidtechorg:sid-android-address-verification:1.1.30"

    // Retrofit
    implementation "com.squareup.retrofit2:retrofit:2.9.0"
    implementation "com.squareup.retrofit2:converter-gson:2.9.0"
    implementation "com.squareup.okhttp3:okhttp:4.12.0"

    // Location Services
    implementation "com.google.android.gms:play-services-location:21.0.1"

    // Lifecycle
    implementation "androidx.lifecycle:lifecycle-runtime-ktx:2.7.0"

    // Coroutines
    implementation "org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3"
    implementation "org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.7.3"
}
```

Minimum requirements:

* **minSdkVersion: 24**
* **Kotlin 1.8+**

---

## Android Permissions

Add the required permissions inside `AndroidManifest.xml`:

```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.ACCESS_BACKGROUND_LOCATION" />
```

You must also request runtime permissions with your preferred library (e.g., `ActivityCompat` or `react-native-permissions` if used in RN embedding).

---

## Basic Usage

```kotlin
import tech.sourceid.sid_address_verification.AddressVerification

class MainActivity : AppCompatActivity() {

    private lateinit var verifier: AddressVerification

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        verifier = AddressVerification(this)
    }

    private fun startTracking() {
        verifier.startLocationTracking(
            apiKey = "YOUR_API_KEY",
            verificationGroupID = "VERIFICATION_TOKEN",
            customerID = "CUSTOMER_ID"
        ) { lat, lng ->
            println("Location: $lat, $lng")
        }
    }

    private fun stopTracking() {
        verifier.stopLocationTracking()
    }

    private fun fetchConfig() {
        val config = verifier.fetchConfig()
        println("Config: Polling = ${config.geotaggingPollingInterval}, Timeout = ${config.geotaggingSessionTimeout}")
    }
}
```

---

## API Reference

### `startLocationTracking(apiKey, verificationGroupID, customerId, callback)`

Starts the background tracking service.

### `stopLocationTracking()`

Stops tracking.

### `fetchConfig()`

Fetches geotagging configuration from Source ID.

### Callback

Returned location callback provides:

```kotlin
(lat: Double, lng: Double)
```

---
## Additional SDKs

If you prefer other integrations:

### Native iOS SDK

[https://github.com/sourceidtechorg/sid-ios-address-verification](https://github.com/sourceidtechorg/sid-address-verification-ios.git)

### Flutter Plugin

[https://github.com/sourceidtechorg/sid-flutter-address-verification](https://github.com/sourceidtechorg/sid-flutter-address-verification.git)

### React Native

**Repository:** [https://github.com/sourceidtechorg/sid-react-native-address-verification](https://github.com/sourceidtechorg/sid-rn-address-verification.git)


---

## Troubleshooting

| Issue                 | Solution                                             |
| --------------------- | ---------------------------------------------------- |
| Tracking not starting | Ensure background permissions are granted            |
| No location updates   | Verify device GPS settings and permissions           |
| Config fetch fails    | Check API key, customer ID, and network connectivity |

---

## License

MIT

