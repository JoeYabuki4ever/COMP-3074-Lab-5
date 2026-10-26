package com.example.gpscoordinates

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.os.Looper
import android.provider.Settings
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import org.json.JSONObject
import java.util.Locale

/**
 * Reads live satellite/GPS updates with Android's LocationManager.
 * JavaScript is used only to display GPS coordinates in a local WebView;
 * it cannot request permissions or access Android location APIs directly.
 */
class MainActivity : Activity() {

    companion object {
        private const val LOCATION_PERMISSION_REQUEST = 1001
        private const val GPS_UPDATE_INTERVAL_MS = 2_000L
    }

    private lateinit var locationManager: LocationManager
    private lateinit var locationWebView: WebView
    private lateinit var startButton: Button
    private lateinit var stopButton: Button

    private var isTracking = false
    private var isPageReady = false
    private var statusText = "Tap Start GPS to read your current location."
    private var statusKind = "idle"
    private var latestFix: Location? = null

    private val gpsListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            if (!isTracking) return
            latestFix = location
            renderLocation(location)
            showStatus("Receiving live satellite GPS updates.", "success")
        }

        override fun onProviderDisabled(provider: String) {
            if (provider == LocationManager.GPS_PROVIDER) {
                stopGps("GPS was turned off. Enable it in GPS Settings and try again.", "warning")
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
        locationWebView = findViewById(R.id.locationWebView)
        startButton = findViewById(R.id.startButton)
        stopButton = findViewById(R.id.stopButton)

        setupOfflineJavaScriptDisplay()

        startButton.setOnClickListener { requestStartGps() }
        stopButton.setOnClickListener { stopGps("GPS tracking stopped. Tap Start GPS to resume.", "idle") }
        findViewById<Button>(R.id.gpsSettingsButton).setOnClickListener {
            startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
        }
        findViewById<Button>(R.id.appSettingsButton).setOnClickListener {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:$packageName")
            }
            startActivity(intent)
        }

        updateButtons()
    }

    private fun setupOfflineJavaScriptDisplay() {
        locationWebView.setBackgroundColor(Color.rgb(10, 20, 35))
        locationWebView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = false
            allowFileAccess = false
            allowContentAccess = false
            blockNetworkLoads = true
            javaScriptCanOpenWindowsAutomatically = false
        }
        locationWebView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                // This WebView displays only bundled HTML; navigation is not allowed.
                return true
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                isPageReady = true
                latestFix?.let { renderLocation(it) }
                showStatus(statusText, statusKind)
            }
        }
        // Only local app content is loaded. No INTERNET permission is required.
        val html = assets.open("index.html").bufferedReader().use { it.readText() }
        locationWebView.loadDataWithBaseURL(
            "https://appassets.androidplatform.net/",
            html,
            "text/html",
            "UTF-8",
            null
        )
    }

    private fun requestStartGps() {
        if (isTracking) return

        if (!hasPreciseLocationPermission()) {
            if (shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)) {
                AlertDialog.Builder(this)
                    .setTitle("Precise location needed")
                    .setMessage("This app uses the GPS satellite provider, which requires Precise location permission while the app is in use. No location data is uploaded.")
                    .setNegativeButton("Cancel", null)
                    .setPositiveButton("Continue") { _, _ -> askForLocationPermissions() }
                    .show()
            } else {
                askForLocationPermissions()
            }
            return
        }

        if (!locationManager.allProviders.contains(LocationManager.GPS_PROVIDER)) {
            showStatus("This device does not offer a GPS provider.", "error")
            return
        }

        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            showStatus("GPS is disabled. Tap GPS Settings to enable Location, then tap Start GPS.", "warning")
            return
        }

        startGps()
    }

    private fun askForLocationPermissions() {
        // Android 12+ requires these two permissions in a single runtime request.
        requestPermissions(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ),
            LOCATION_PERMISSION_REQUEST
        )
    }

    private fun hasPreciseLocationPermission(): Boolean =
        checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode != LOCATION_PERMISSION_REQUEST) return

        if (hasPreciseLocationPermission()) {
            requestStartGps()
        } else {
            val hasApproximate =
                checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
            if (hasApproximate) {
                showStatus(
                    "Approximate permission was granted. GPS requires Precise location. Tap Start GPS to upgrade, or use App Permissions.",
                    "warning"
                )
            } else {
                showStatus(
                    "Location permission denied. Tap Start GPS to retry, or open App Permissions.",
                    "error"
                )
            }
        }
    }

    private fun startGps() {
        if (!hasPreciseLocationPermission()) {
            showStatus("Precise location permission is required.", "warning")
            return
        }
        try {
            // Explicit GPS_PROVIDER means we don't switch to network/fused positioning.
            locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                GPS_UPDATE_INTERVAL_MS,
                0f,
                gpsListener,
                Looper.getMainLooper()
            )
            isTracking = true
            latestFix = null
            runJavaScript("window.clearPosition();")
            showStatus("Searching for GPS satellites… This can take a minute outdoors.", "waiting")
            updateButtons()
        } catch (exception: SecurityException) {
            stopGps("Permission was revoked. Grant Precise location in App Permissions.", "error")
        } catch (exception: IllegalArgumentException) {
            stopGps("GPS provider is unavailable on this device.", "error")
        }
    }

    private fun stopGps(message: String, kind: String) {
        if (isTracking) {
            try {
                locationManager.removeUpdates(gpsListener)
            } catch (_: SecurityException) {
                // Permission may have changed while tracking.
            }
        }
        isTracking = false
        showStatus(message, kind)
        updateButtons()
    }

    private fun updateButtons() {
        startButton.isEnabled = !isTracking
        stopButton.isEnabled = isTracking
        startButton.alpha = if (isTracking) 0.5f else 1f
        stopButton.alpha = if (isTracking) 1f else 0.5f
    }

    private fun renderLocation(location: Location) {
        // Numeric values are serialized with a fixed locale, not as executable text.
        val latitude = String.format(Locale.US, "%.8f", location.latitude)
        val longitude = String.format(Locale.US, "%.8f", location.longitude)
        val accuracy = String.format(Locale.US, "%.1f", location.accuracy)
        runJavaScript(
            "window.updatePosition($latitude, $longitude, $accuracy, ${location.time});"
        )
    }

    private fun showStatus(message: String, kind: String) {
        statusText = message
        statusKind = kind
        val safeMessage = JSONObject.quote(message)
        val safeKind = JSONObject.quote(kind)
        runJavaScript("window.setStatus($safeMessage, $safeKind);")
    }

    private fun runJavaScript(script: String) {
        if (isPageReady) locationWebView.evaluateJavascript(script, null)
    }

    override fun onPause() {
        super.onPause()
        // No background location permission is requested: GPS stops off-screen.
        if (isTracking) {
            stopGps("GPS paused while the app is in the background. Tap Start GPS to resume.", "idle")
        }
    }

    override fun onDestroy() {
        isPageReady = false
        locationWebView.destroy()
        super.onDestroy()
    }
}
