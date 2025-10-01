package com.danihg.bitratelab.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.net.InetAddress
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt
import kotlin.system.measureTimeMillis

data class NetworkTestResult(
    val downloadSpeed: Double, // Mbps
    val uploadSpeed: Double, // Mbps
    val latency: Long, // ms
    val jitter: Double, // ms
    val packetLoss: Double, // percentage
    val connectionType: ConnectionType,
    val isStable: Boolean,
    val connectionReport: ConnectionReport
)

data class ConnectionReport(
    val testDurationSeconds: Int,
    val speedMeasurements: List<SpeedMeasurement>,
    val latencyMeasurements: List<LatencyMeasurement>,
    val hasSpikes: Boolean,
    val spikeCount: Int,
    val stabilityScore: Double, // 0.0 to 1.0
    val stabilityDescription: String,
    val averageSpeed: Double,
    val minSpeed: Double,
    val maxSpeed: Double,
    val speedVariation: Double // percentage
)

data class LatencyMeasurement(
    val timestamp: Long,
    val latency: Long // ms
)

enum class ConnectionType {
    WIFI, MOBILE_5G, MOBILE_4G, MOBILE_3G, MOBILE_2G, ETHERNET, UNKNOWN
}

data class SpeedMeasurement(
    val timestamp: Long,
    val speed: Double
)

class NetworkTester(private val context: Context) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    suspend fun runComprehensiveTest(
        progressCallback: (Int, String) -> Unit = { _, _ -> }
    ): NetworkTestResult = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        val testDurationMs = 22000L // 22 seconds total

        progressCallback(5, "Detecting connection type...")
        val connectionType = getConnectionType()

        progressCallback(10, "Starting comprehensive analysis...")
        val speedMeasurements = mutableListOf<SpeedMeasurement>()
        val latencyMeasurements = mutableListOf<LatencyMeasurement>()

        // Perform continuous testing for 40 seconds (20 samples)
        val testIntervalMs = 2000L // Test every 2 seconds
        var currentProgress = 15
        val progressIncrement = 70 / 20 // 70% progress over 20 measurements

        for (i in 0 until 20) {
            val measurementStart = System.currentTimeMillis()

            progressCallback(currentProgress, "Measuring speed and latency... (${i + 1}/20)")

            // Measure speed with timeout (max 3 seconds per measurement)
            val speed = withTimeoutOrNull(3000L) {
                measureDownloadSpeedQuick()
            } ?: 0.0
            speedMeasurements.add(SpeedMeasurement(measurementStart, speed))

            // Measure latency with timeout (max 1.5 seconds per measurement)
            val latency = withTimeoutOrNull(1500L) {
                measureLatencyQuick()
            } ?: 1000L
            latencyMeasurements.add(LatencyMeasurement(measurementStart, latency))

            currentProgress += progressIncrement

            // Always maintain 2 second interval between measurements
            val elapsed = System.currentTimeMillis() - measurementStart
            val waitTime = testIntervalMs - elapsed
            if (waitTime > 0) {
                kotlinx.coroutines.delay(waitTime)
            }
        }

        progressCallback(85, "Analyzing connection stability...")

        // Calculate comprehensive metrics
        // Filter out failed measurements (0.0 speed) for more accurate statistics
        val validSpeedMeasurements = speedMeasurements.filter { it.speed > 0.0 }
        val avgSpeed = if (validSpeedMeasurements.isNotEmpty()) {
            validSpeedMeasurements.map { it.speed }.average()
        } else {
            0.1 // Minimum threshold to avoid division by zero
        }
        val minSpeed = if (validSpeedMeasurements.isNotEmpty()) {
            validSpeedMeasurements.minOf { it.speed }
        } else {
            avgSpeed
        }
        val maxSpeed = if (validSpeedMeasurements.isNotEmpty()) {
            validSpeedMeasurements.maxOf { it.speed }
        } else {
            avgSpeed
        }
        val avgLatency = latencyMeasurements.map { it.latency }.average().toLong()

        // Calculate jitter from latency measurements
        val jitter = calculateJitter(latencyMeasurements)

        // Detect spikes and stability
        val (hasSpikes, spikeCount) = detectSpikes(speedMeasurements)
        val stabilityScore = calculateStabilityScore(speedMeasurements, latencyMeasurements)
        val stabilityDescription = getStabilityDescription(stabilityScore, hasSpikes, spikeCount)

        progressCallback(90, "Measuring upload speed...")
        val uploadSpeed = withTimeoutOrNull(5000L) {
            measureUploadSpeed(avgSpeed)
        } ?: (avgSpeed * 0.2)

        progressCallback(95, "Calculating packet loss...")
        val packetLoss = withTimeoutOrNull(5000L) {
            measurePacketLoss()
        } ?: 0.0

        // Calculate speed variation with safe guards
        val speedVariation = if (avgSpeed > 0) {
            ((maxSpeed - minSpeed) / avgSpeed) * 100
        } else {
            0.0
        }

        // More realistic stability check based on multiple factors
        // Connection is stable if:
        // - Stability score is reasonable (> 0.5)
        // - Not too many spikes (< 3)
        // - Speed variation is acceptable (< 100%)
        // - Low jitter (< 50ms)
        // - Low packet loss (< 2%)
        val isStable = stabilityScore > 0.5 &&
                      spikeCount < 3 &&
                      speedVariation < 100 &&
                      jitter < 50.0 &&
                      packetLoss < 2.0

        val connectionReport = ConnectionReport(
            testDurationSeconds = ((System.currentTimeMillis() - startTime) / 1000).toInt(),
            speedMeasurements = speedMeasurements,
            latencyMeasurements = latencyMeasurements,
            hasSpikes = hasSpikes,
            spikeCount = spikeCount,
            stabilityScore = stabilityScore,
            stabilityDescription = stabilityDescription,
            averageSpeed = avgSpeed,
            minSpeed = minSpeed,
            maxSpeed = maxSpeed,
            speedVariation = speedVariation
        )

        progressCallback(100, "Test completed!")

        NetworkTestResult(
            downloadSpeed = avgSpeed,
            uploadSpeed = uploadSpeed,
            latency = avgLatency,
            jitter = jitter,
            packetLoss = packetLoss,
            connectionType = connectionType,
            isStable = isStable,
            connectionReport = connectionReport
        )
    }

    private fun getConnectionType(): ConnectionType {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return ConnectionType.UNKNOWN
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return ConnectionType.UNKNOWN

        return when {
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> ConnectionType.WIFI
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> {
                // Approximate based on capabilities
                when {
                    capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) -> ConnectionType.MOBILE_4G
                    else -> ConnectionType.MOBILE_3G
                }
            }
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> ConnectionType.ETHERNET
            else -> ConnectionType.UNKNOWN
        }
    }

    private suspend fun measureLatency(): Long = withContext(Dispatchers.IO) {
        val measurements = mutableListOf<Long>()
        repeat(5) {
            try {
                val address = InetAddress.getByName("1.1.1.1")
                val time = measureTimeMillis {
                    address.isReachable(3000)
                }
                measurements.add(time)
            } catch (e: Exception) {
                measurements.add(1000) // Default high latency on error
            }
        }
        measurements.average().toLong()
    }

    private suspend fun measureJitter(): Double = withContext(Dispatchers.IO) {
        val latencies = mutableListOf<Long>()
        repeat(10) {
            try {
                val time = measureTimeMillis {
                    val request = Request.Builder()
                        .url("https://www.google.com")
                        .head()
                        .build()
                    client.newCall(request).execute().use { response ->
                        response.isSuccessful
                    }
                }
                latencies.add(time)
            } catch (e: Exception) {
                latencies.add(1000)
            }
        }

        if (latencies.size < 2) return@withContext 0.0

        val mean = latencies.average()
        val variance = latencies.map { (it - mean) * (it - mean) }.average()
        kotlin.math.sqrt(variance)
    }

    private suspend fun measureDownloadSpeed(): Double = withContext(Dispatchers.IO) {
        try {
            // Using a larger test file for accurate download speed measurement
            val testUrl = "https://speed.cloudflare.com/__down?bytes=25000000" // 25MB test file
            val request = Request.Builder().url(testUrl).build()

            val startTime = System.currentTimeMillis()
            var totalBytes = 0L

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    response.body?.byteStream()?.use { inputStream ->
                        val buffer = ByteArray(8192)
                        var bytesRead: Int
                        while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                            totalBytes += bytesRead
                        }
                    }
                    val endTime = System.currentTimeMillis()
                    val timeTakenSeconds = (endTime - startTime) / 1000.0

                    if (timeTakenSeconds > 0) {
                        val speedBps = totalBytes / timeTakenSeconds
                        val speedMbps = (speedBps * 8) / (1000 * 1000) // Convert to Mbps (using 1000, not 1024)
                        speedMbps
                    } else {
                        0.0
                    }
                } else {
                    0.0
                }
            }
        } catch (e: Exception) {
            0.0
        }
    }

    private suspend fun measureDownloadSpeedQuick(): Double = withContext(Dispatchers.IO) {
        try {
            // Use 5MB file for quick measurements that complete within timeout
            // This allows measuring even slow connections while still accurate for fast ones
            val testUrl = "https://speed.cloudflare.com/__down?bytes=5000000" // 5MB test file
            val request = Request.Builder().url(testUrl).build()

            var totalBytes = 0L
            var firstByteTime = 0L
            var startTime = 0L

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    startTime = System.currentTimeMillis()

                    response.body?.byteStream()?.use { inputStream ->
                        val buffer = ByteArray(65536) // Larger buffer for faster reading
                        var bytesRead: Int
                        var isFirstChunk = true

                        while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                            totalBytes += bytesRead
                            if (isFirstChunk) {
                                firstByteTime = System.currentTimeMillis()
                                isFirstChunk = false
                            }
                        }
                    }

                    val endTime = System.currentTimeMillis()

                    // Calculate speed from first byte received to last byte (excludes connection setup)
                    val effectiveStartTime = if (firstByteTime > 0) firstByteTime else startTime
                    val timeTakenSeconds = (endTime - effectiveStartTime) / 1000.0

                    if (timeTakenSeconds > 0.1) { // Must take at least 0.1 seconds
                        val speedBps = totalBytes / timeTakenSeconds
                        val speedMbps = (speedBps * 8) / (1000 * 1000) // Convert to Mbps
                        speedMbps
                    } else {
                        0.0
                    }
                } else {
                    0.0
                }
            }
        } catch (e: Exception) {
            0.0
        }
    }

    private suspend fun measureLatencyQuick(): Long = withContext(Dispatchers.IO) {
        try {
            // Use ICMP ping via InetAddress.isReachable for true network latency
            val address = InetAddress.getByName("1.1.1.1")
            val time = measureTimeMillis {
                address.isReachable(3000) // 3 second timeout
            }
            time
        } catch (e: Exception) {
            // Fallback to HTTP HEAD if ICMP fails
            try {
                val time = measureTimeMillis {
                    val request = Request.Builder()
                        .url("http://1.1.1.1/") // Use HTTP not HTTPS to avoid TLS overhead
                        .head()
                        .build()
                    client.newCall(request).execute().use { response ->
                        response.isSuccessful || response.code == 301 || response.code == 302
                    }
                }
                time
            } catch (e2: Exception) {
                1000 // Default high latency on error
            }
        }
    }

    private suspend fun measureUploadSpeed(avgDownloadSpeed: Double): Double = withContext(Dispatchers.IO) {
        try {
            // Perform actual upload test with 2MB of data
            val testUrl = "https://speed.cloudflare.com/__up"

            // Create 2MB of data to upload (completes within timeout on slow connections)
            val uploadData = ByteArray(2000000) { (it % 256).toByte() }

            val mediaType = "application/octet-stream".toMediaType()
            val requestBody = uploadData.toRequestBody(mediaType)

            val request = Request.Builder()
                .url(testUrl)
                .post(requestBody)
                .build()

            val startTime = System.currentTimeMillis()

            client.newCall(request).execute().use { response ->
                val endTime = System.currentTimeMillis()
                if (response.isSuccessful) {
                    val timeTakenSeconds = (endTime - startTime) / 1000.0
                    if (timeTakenSeconds > 0.1) { // Must take at least 0.1 seconds
                        val speedBps = uploadData.size / timeTakenSeconds
                        val speedMbps = (speedBps * 8) / (1000 * 1000) // Convert to Mbps
                        speedMbps
                    } else {
                        avgDownloadSpeed * 0.2 // Fallback estimation
                    }
                } else {
                    avgDownloadSpeed * 0.2 // Fallback estimation
                }
            }
        } catch (e: Exception) {
            // Fallback to estimation if upload test fails
            avgDownloadSpeed * 0.2
        }
    }

    private fun calculateJitter(latencyMeasurements: List<LatencyMeasurement>): Double {
        if (latencyMeasurements.size < 2) return 0.0

        val latencies = latencyMeasurements.map { it.latency.toDouble() }
        val mean = latencies.average()
        val variance = latencies.map { (it - mean) * (it - mean) }.average()
        return kotlin.math.sqrt(variance)
    }

    private fun detectSpikes(speedMeasurements: List<SpeedMeasurement>): Pair<Boolean, Int> {
        if (speedMeasurements.size < 3) return Pair(false, 0)

        val speeds = speedMeasurements.map { it.speed }
        val mean = speeds.average()
        val stdDev = kotlin.math.sqrt(speeds.map { (it - mean) * (it - mean) }.average())

        var spikeCount = 0
        for (speed in speeds) {
            // Consider a spike if the speed deviates by more than 2 standard deviations
            if (kotlin.math.abs(speed - mean) > 2 * stdDev) {
                spikeCount++
            }
        }

        val hasSpikes = spikeCount > 0
        return Pair(hasSpikes, spikeCount)
    }

    private fun calculateStabilityScore(
        speedMeasurements: List<SpeedMeasurement>,
        latencyMeasurements: List<LatencyMeasurement>
    ): Double {
        if (speedMeasurements.isEmpty() || latencyMeasurements.isEmpty()) return 0.0

        var score = 1.0

        // Speed consistency (40% of score)
        val speeds = speedMeasurements.map { it.speed }
        val speedMean = speeds.average()
        val speedVariability = if (speedMean > 0) {
            val speedStdDev = kotlin.math.sqrt(speeds.map { (it - speedMean) * (it - speedMean) }.average())
            speedStdDev / speedMean
        } else 1.0
        score -= speedVariability * 0.4

        // Latency consistency (30% of score)
        val latencies = latencyMeasurements.map { it.latency.toDouble() }
        val latencyMean = latencies.average()
        val latencyVariability = if (latencyMean > 0) {
            val latencyStdDev = kotlin.math.sqrt(latencies.map { (it - latencyMean) * (it - latencyMean) }.average())
            latencyStdDev / latencyMean
        } else 1.0
        score -= latencyVariability * 0.3

        // Absolute performance penalties (30% of score)
        if (speedMean < 1.0) score -= 0.2 // Very slow connection
        if (latencyMean > 200) score -= 0.1 // High latency

        return kotlin.math.max(0.0, kotlin.math.min(1.0, score))
    }

    private fun getStabilityDescription(stabilityScore: Double, hasSpikes: Boolean, spikeCount: Int): String {
        return when {
            stabilityScore > 0.85 && !hasSpikes -> "Excellent - Very stable connection with consistent performance"
            stabilityScore > 0.75 && spikeCount <= 1 -> "Very Good - Stable connection with minimal fluctuations"
            stabilityScore > 0.65 && spikeCount <= 2 -> "Good - Generally stable with occasional variations"
            stabilityScore > 0.50 && spikeCount <= 3 -> "Fair - Acceptable stability for most streaming scenarios"
            stabilityScore > 0.35 -> "Moderate - Some instability, suitable for lower quality streams"
            hasSpikes && spikeCount > 5 -> "Poor - Unstable with multiple connection spikes detected"
            else -> "Critical - Highly unstable connection not recommended for streaming"
        }
    }

    private suspend fun measurePacketLoss(): Double = withContext(Dispatchers.IO) {
        var successfulPings = 0
        val totalPings = 10

        repeat(totalPings) {
            try {
                val request = Request.Builder()
                    .url("https://www.google.com")
                    .head()
                    .build()
                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        successfulPings++
                    }
                }
            } catch (e: Exception) {
                // Packet lost
            }
        }

        ((totalPings - successfulPings).toDouble() / totalPings) * 100
    }

    private fun isConnectionStable(downloadSpeed: Double, jitter: Double, packetLoss: Double): Boolean {
        // Based on streaming criteria:
        // - Jitter should be < 30ms for good quality (but < 50ms is still acceptable)
        // - Packet loss should be < 1% for good quality (< 2% is risky but possible)
        // - Download speed should be reasonable for any streaming

        // Count quality issues
        var issues = 0

        if (jitter >= 30.0) issues++           // Jitter in risky range
        if (packetLoss >= 1.0) issues++        // Packet loss in risky range
        if (downloadSpeed < 5.0) issues++      // Very low speed

        // Connection is stable if it has 0 or 1 issue (not 2+)
        return issues <= 1
    }
}