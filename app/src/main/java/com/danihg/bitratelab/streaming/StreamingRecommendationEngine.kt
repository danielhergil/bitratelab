package com.danihg.bitratelab.streaming

import com.danihg.bitratelab.network.NetworkTestResult
import kotlin.math.min

data class StreamingConfiguration(
    val resolution: Resolution,
    val fps: Int,
    val bitrate: Int, // kbps
    val codec: Codec,
    val quality: String,
    val riskLevel: RiskLevel,
    val description: String
)

enum class Resolution(val width: Int, val height: Int, val displayName: String) {
    R_480P(854, 480, "480p"),
    R_720P(1280, 720, "720p"),
    R_1080P(1920, 1080, "1080p"),
    R_1440P(2560, 1440, "1440p"),
    R_4K(3840, 2160, "4K")
}

enum class Codec(val displayName: String, val efficiency: Float) {
    H264("H.264", 1.0f),
    H265("H.265/HEVC", 0.7f), // 30% more efficient
    AV1("AV1", 0.6f) // 40% more efficient
}

enum class RiskLevel(val displayName: String, val color: String) {
    LOW("Recommended", "#4CAF50"),
    MEDIUM("Medium", "#FF9800"),
    HIGH("Risky", "#F44336"),
    CRITICAL("Impossible", "#9C27B0")
}

class StreamingRecommendationEngine {

    fun generateRecommendations(networkResult: NetworkTestResult): List<StreamingConfiguration> {
        val configurations = mutableListOf<StreamingConfiguration>()
        val availableBandwidthMbps = networkResult.downloadSpeed
        val stabilityMultiplier = getStabilityMultiplier(networkResult)

        // Generate realistic configurations for different resolutions and frame rates
        for (resolution in Resolution.values()) {
            for (fps in listOf(30, 60)) {
                for (codec in Codec.values()) {
                    val realisticBitrate = calculateRealisticBitrate(resolution, fps, codec)
                    val requiredBandwidthMbps = (realisticBitrate / 1000.0) * 1.3 // Add 30% buffer for stable streaming

                    // Only include configurations that could potentially work
                    if (requiredBandwidthMbps <= availableBandwidthMbps * 2.0) { // Allow even challenging configs
                        val riskLevel = calculateRealisticRiskLevel(
                            realisticBitrate,
                            availableBandwidthMbps,
                            networkResult,
                            stabilityMultiplier
                        )

                        val quality = determineQuality(riskLevel, networkResult.isStable)
                        val description = generateDescription(resolution, fps, codec, riskLevel)

                        configurations.add(
                            StreamingConfiguration(
                                resolution = resolution,
                                fps = fps,
                                bitrate = realisticBitrate,
                                codec = codec,
                                quality = quality,
                                riskLevel = riskLevel,
                                description = description
                            )
                        )
                    }
                }
            }
        }

        return configurations.sortedWith(
            compareBy<StreamingConfiguration> { it.riskLevel.ordinal }
                .thenByDescending { it.resolution.width * it.resolution.height }
                .thenByDescending { it.fps }
        )
    }

    private fun calculateRealisticBitrate(resolution: Resolution, fps: Int, codec: Codec): Int {
        // Real-world streaming bitrates in kbps based on criteria (using upper range for conservative estimates)
        // These are VIDEO bitrates - audio (160 kbps) will be added separately
        val baseBitrate = when (resolution) {
            Resolution.R_480P -> when (fps) {
                30 -> 2500   // 2.5 Mbps for 480p30 (H.264 upper range)
                60 -> 3500   // 3.5 Mbps for 480p60
                else -> 2500
            }
            Resolution.R_720P -> when (fps) {
                30 -> 5000   // 5 Mbps for 720p30
                60 -> 6500   // 6.5 Mbps for 720p60
                else -> 5000
            }
            Resolution.R_1080P -> when (fps) {
                30 -> 8000   // 8 Mbps for 1080p30
                60 -> 10000  // 10 Mbps for 1080p60
                else -> 8000
            }
            Resolution.R_1440P -> when (fps) {
                30 -> 12000  // 12 Mbps for 1440p30
                60 -> 16000  // 16 Mbps for 1440p60
                else -> 12000
            }
            Resolution.R_4K -> when (fps) {
                30 -> 18000  // 18 Mbps for 4K30
                60 -> 25000  // 25 Mbps for 4K60
                else -> 18000
            }
        }

        // Apply codec efficiency (HEVC/AV1 are ~30-40% more efficient)
        val videoBitrate = (baseBitrate * codec.efficiency).toInt()

        // Add audio bitrate (160 kbps = 0.16 Mbps)
        return videoBitrate + 160
    }

    private fun getStabilityMultiplier(networkResult: NetworkTestResult): Double {
        return networkResult.connectionReport.stabilityScore
    }

    private fun calculateRealisticRiskLevel(
        bitrateKbps: Int,
        availableBandwidthMbps: Double,
        networkResult: NetworkTestResult,
        stabilityMultiplier: Double
    ): RiskLevel {
        // For streaming, we use UPLOAD speed (critical for live streaming)
        val availableUploadMbps = networkResult.uploadSpeed
        val requiredBitrateMbps = bitrateKbps / 1000.0

        // Calculate headroom ratio (how much upload bandwidth vs required bitrate)
        val headroomRatio = availableUploadMbps / requiredBitrateMbps

        // Network quality metrics
        val packetLoss = networkResult.packetLoss
        val jitter = networkResult.jitter
        val latency = networkResult.latency

        // Start with bandwidth-based classification (primary criteria)
        var baseLevel = when {
            headroomRatio >= 2.0 -> RiskLevel.LOW           // Recommended (≥2.0x)
            headroomRatio >= 1.5 -> RiskLevel.MEDIUM        // Medium (1.5-1.99x)
            headroomRatio >= 1.2 -> RiskLevel.HIGH          // Risky (1.2-1.49x)
            else -> RiskLevel.CRITICAL                       // Impossible (<1.2x)
        }

        // Count network quality failures based on criteria
        var qualityFailures = 0

        // Packet loss check
        when {
            packetLoss > 2.0 -> qualityFailures += 2  // Critical
            packetLoss > 1.0 -> qualityFailures += 1  // Risky
            packetLoss > 0.2 -> qualityFailures += 1  // Medium (but counted as 1 failure)
        }

        // Jitter check
        when {
            jitter > 50 -> qualityFailures += 2       // Critical
            jitter > 30 -> qualityFailures += 1       // Risky
            jitter > 15 -> qualityFailures += 1       // Medium
        }

        // Latency check (RTT under load / bufferbloat indicator)
        // Using latency as proxy for bufferbloat
        when {
            latency > 300 -> qualityFailures += 2     // Critical
            latency > 150 -> qualityFailures += 1     // Risky
            latency > 50 -> qualityFailures += 1      // Medium
        }

        // Downgrade one category if 2+ quality metrics fail
        if (qualityFailures >= 2) {
            baseLevel = when (baseLevel) {
                RiskLevel.LOW -> RiskLevel.MEDIUM
                RiskLevel.MEDIUM -> RiskLevel.HIGH
                RiskLevel.HIGH -> RiskLevel.CRITICAL
                RiskLevel.CRITICAL -> RiskLevel.CRITICAL
            }
        }

        // Debug logging
        android.util.Log.d("RiskCalc", "Bitrate: ${String.format("%.2f", requiredBitrateMbps)}Mbps, " +
                "Upload: ${String.format("%.2f", availableUploadMbps)}Mbps, " +
                "Headroom: ${String.format("%.2f", headroomRatio)}x, " +
                "Loss: ${String.format("%.2f", packetLoss)}%, " +
                "Jitter: ${String.format("%.1f", jitter)}ms, " +
                "Latency: ${latency}ms, " +
                "QualityFails: $qualityFailures -> $baseLevel")

        return baseLevel
    }

    private fun determineQuality(riskLevel: RiskLevel, isStable: Boolean): String {
        return when (riskLevel) {
            RiskLevel.LOW -> if (isStable) "Excellent" else "Good"
            RiskLevel.MEDIUM -> if (isStable) "Good" else "Fair"
            RiskLevel.HIGH -> "Poor"
            RiskLevel.CRITICAL -> "Very Poor"
        }
    }

    private fun generateDescription(
        resolution: Resolution,
        fps: Int,
        codec: Codec,
        riskLevel: RiskLevel
    ): String {
        val baseDesc = "${resolution.displayName} @ ${fps}fps (${codec.displayName})"

        val riskDesc = when (riskLevel) {
            RiskLevel.LOW -> "Recommended - 2x headroom, smooth streaming with buffer for spikes"
            RiskLevel.MEDIUM -> "Acceptable - 1.5x headroom, may struggle with network fluctuations"
            RiskLevel.HIGH -> "Risky - 1.2x headroom, frequent buffering likely during drops"
            RiskLevel.CRITICAL -> "Impossible - insufficient upload bandwidth"
        }

        return "$baseDesc - $riskDesc"
    }
}