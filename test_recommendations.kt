// Test script to verify recommendations with real data
import com.danihg.bitratelab.network.*
import com.danihg.bitratelab.streaming.*

fun main() {
    // Simulate your actual network results: 493 Mbps down, 93 Mbps up, 24ms latency
    val testResult = NetworkTestResult(
        downloadSpeed = 493.44,
        uploadSpeed = 93.2,
        latency = 24,
        jitter = 5.0,
        packetLoss = 0.0,
        connectionType = ConnectionType.WIFI,
        isStable = true,
        connectionReport = ConnectionReport(
            testDurationSeconds = 22,
            speedMeasurements = listOf(),
            latencyMeasurements = listOf(),
            hasSpikes = false,
            spikeCount = 0,
            stabilityScore = 0.95,
            stabilityDescription = "Excellent",
            averageSpeed = 493.44,
            minSpeed = 485.0,
            maxSpeed = 500.0,
            speedVariation = 3.0
        )
    )

    val engine = StreamingRecommendationEngine()
    val recommendations = engine.generateRecommendations(testResult)

    println("========== RECOMMENDATIONS FOR 493 Mbps / 93 Mbps / 24ms ==========")
    recommendations.take(15).forEach { config ->
        println("${config.resolution.displayName} @ ${config.fps}fps (${config.codec.displayName}) - ${config.bitrate/1000}Mbps")
        println("  Risk: ${config.riskLevel.displayName} | Quality: ${config.quality}")
        println("  ${config.description}")
        println()
    }
}
