# GPS Coordinates — Kotlin + JavaScript Android app

A complete Android Studio source project for a live GPS latitude/longitude display. The app uses **Kotlin** to get satellite GPS fixes and an offline **HTML/CSS/JavaScript** screen in an Android WebView to display the coordinates.

## Requirements

- Android Studio (recent version) with **Android SDK Platform 35** installed.
- **JDK 17** selected for the Gradle JVM (Android Studio's bundled JBR/JDK 17 works). **Do not select JDK 25** with the pinned Gradle 8.9 version.
- An Android phone (Android 8.0 or newer) with Location enabled **or** an emulator running Google APIs with a simulated location.
- Internet connection during the first Gradle sync to obtain build dependencies; the running app uses no Internet.

## Open and run using Android Studio (Windows)

1. **Extract** `GPSCoordinatesApp.zip` to a normal folder. Do not open the ZIP directly.
2. Open **Android Studio** and choose **Open** (or File > Open).
3. Select the **GPSCoordinatesApp** folder containing `settings.gradle.kts`. Click **OK** / **Trust Project**.
4. If prompted to install Android SDK Platform 35 or download Gradle/dependencies, accept. The Gradle configuration specifies **Gradle 8.9**, **Android Gradle Plugin 8.7.3**, and **Kotlin 2.0.21**.
5. Set **Settings > Build, Execution, Deployment > Build Tools > Gradle > Gradle JDK** to **JDK 17**. Then use **File > Sync Project with Gradle Files**, if necessary.
6. Connect a phone with USB debugging enabled or start an emulator (for example, **Pixel 8**) in **Device Manager**.
7. Choose the `app` run configuration and click the **green Run (▶) button**.
8. Tap **Start GPS** in the app.
9. When the Android permission dialog appears, select **Precise**, then **While using the app**. If GPS is disabled, tap **GPS Settings** and turn device Location on.
10. Once satellite reception/simulated GPS is available, the **current latitude**, **current longitude**, **estimated accuracy**, and **last fix time** appear. Tap **Stop GPS** to stop updates.

**Important:** This source ZIP includes the `gradle-wrapper.properties` version pin, but not an executable `gradlew`/`gradlew.bat` wrapper or binary wrapper JAR. It is designed for Android Studio's Gradle import/sync. If you need CLI Gradle commands, use a locally installed Gradle 8.9 to run `gradle wrapper --gradle-version 8.9` in the project directory, then `./gradlew assembleDebug` (macOS/Linux) or `gradlew.bat assembleDebug` (Windows). Do not download unknown wrapper JARs.

## Test GPS with the Pixel 8 emulator

1. Start **Device Manager > Pixel 8** (or create an AVD with **Google APIs**).
2. Run the app and tap **Start GPS**. Approve **Precise** location access.
3. In the emulator, click the **three-dot menu (Extended Controls)**.
4. Open **Location**. Select a position on the map or enter latitude/longitude and click **Set location**.
5. Return to the application and verify that both GPS coordinates display. If there is no fix, check that device Location is enabled and try setting the simulated position again.

On a physical phone, GPS reception is usually better outside. The first satellite fix can take time. The application **does not fall back to network-derived location**, so a phone indoors may stay at “Searching for GPS satellites.”

## Where the important code lives

| File | Purpose |
| --- | --- |
| `app/src/main/AndroidManifest.xml` | Declares both required location permissions. |
| `app/src/main/java/com/example/gpscoordinates/MainActivity.kt` | Requests permissions, checks whether GPS is enabled, subscribes to GPS_PROVIDER, and sends updates to JavaScript. |
| `app/src/main/assets/index.html` | Offline JavaScript interface displaying latitude, longitude, accuracy and last update. |
| `app/src/main/res/layout/activity_main.xml` | WebView plus Start/Stop/Settings buttons. |
| `app/build.gradle.kts` | SDK and Kotlin/Java build settings. |

## Permissions and privacy

- Declares `ACCESS_FINE_LOCATION` and `ACCESS_COARSE_LOCATION` in AndroidManifest.xml.
- Requests both permissions **together at runtime**, after the user presses Start GPS, in accordance with Android 12+ permission behavior.
- Requires **Precise location** because the app explicitly uses the GPS satellite provider. If approximate-only permission is granted, it explains how to upgrade.
- Handles denied permissions, disabled GPS, no GPS provider, and runtime revocations.
- Registers updates with `LocationManager.GPS_PROVIDER`, a minimum interval of 2,000 ms, and no movement threshold. Update timing depends on device/satellite availability.
- Stops updates when **Stop GPS** is pressed or the app goes into the background.
- Does **not** request background location permission, use network location, make network calls, save coordinates, or upload them anywhere.
- Uses an offline WebView display. There is no JavaScript-to-Android interface; Kotlin safely sends numeric fixes to the display.

## Suggested class submission screenshots

1. Android Studio project tree, showing `MainActivity.kt`, `index.html`, and `AndroidManifest.xml`.
2. Manifest containing `ACCESS_FINE_LOCATION` and `ACCESS_COARSE_LOCATION`.
3. The Android runtime permission dialog, showing **Precise** and **While using the app**.
4. The running app displaying valid latitude and longitude values (after obtaining a fix).

Submit the source ZIP and screenshots if your instructor requests them. **No built APK is included** because the Android SDK/Gradle build chain was not available in the creation environment; compile it in Android Studio.
