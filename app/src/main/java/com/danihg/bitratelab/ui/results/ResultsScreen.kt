package com.danihg.bitratelab.ui.results

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.danihg.bitratelab.network.ConnectionType
import com.danihg.bitratelab.streaming.RiskLevel
import com.danihg.bitratelab.streaming.StreamingConfiguration
import com.danihg.bitratelab.ui.test.TestViewModel
import com.danihg.bitratelab.ui.theme.Success
import com.danihg.bitratelab.ui.theme.Warning
import com.danihg.bitratelab.ui.theme.Error

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultsScreen(
    viewModel: TestViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Test Results") },
                navigationIcon = {
                    IconButton(onClick = {
                        viewModel.resetTest()
                        onNavigateBack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Network Test Results Summary
            uiState.testResult?.let { result ->
                item {
                    NetworkSummaryCard(result)
                }

                // Detailed Connection Report
                item {
                    ConnectionReportCard(result.connectionReport)
                }
            }

            // Streaming Recommendations Header
            item {
                Text(
                    text = "Recommended Streaming Settings",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            // Legend
            item {
                RiskLegend()
            }

            // Streaming Configuration Cards
            items(uiState.recommendations) { config ->
                StreamingConfigCard(config)
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun NetworkSummaryCard(result: com.danihg.bitratelab.network.NetworkTestResult) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Connection Analysis",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )

                ConnectionTypeChip(result.connectionType)
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                MetricItem(
                    label = "Download",
                    value = "${String.format("%.1f", result.downloadSpeed)} Mbps",
                    modifier = Modifier.weight(1f)
                )
                MetricItem(
                    label = "Upload",
                    value = "${String.format("%.1f", result.uploadSpeed)} Mbps",
                    modifier = Modifier.weight(1f)
                )
                MetricItem(
                    label = "Latency",
                    value = "${result.latency}ms",
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                MetricItem(
                    label = "Jitter",
                    value = "${String.format("%.1f", result.jitter)}ms",
                    modifier = Modifier.weight(1f)
                )
                MetricItem(
                    label = "Packet Loss",
                    value = "${String.format("%.1f", result.packetLoss)}%",
                    modifier = Modifier.weight(1f)
                )
                MetricItem(
                    label = "Stability",
                    value = if (result.isStable) "Stable" else "Unstable",
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun ConnectionTypeChip(connectionType: ConnectionType) {
    val text = when (connectionType) {
        ConnectionType.WIFI -> "WiFi"
        ConnectionType.MOBILE_5G -> "5G"
        ConnectionType.MOBILE_4G -> "4G"
        ConnectionType.MOBILE_3G -> "3G"
        ConnectionType.MOBILE_2G -> "2G"
        ConnectionType.ETHERNET -> "Ethernet"
        ConnectionType.UNKNOWN -> "Unknown"
    }

    Surface(
        modifier = Modifier
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer),
        color = MaterialTheme.colorScheme.primaryContainer
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun MetricItem(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = label,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
    }
}

@Composable
private fun RiskLegend() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Risk Level Guide",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                LegendItem(RiskLevel.LOW, "Recommended")
                LegendItem(RiskLevel.MEDIUM, "Caution")
                LegendItem(RiskLevel.HIGH, "Risky")
                LegendItem(RiskLevel.CRITICAL, "Avoid")
            }
        }
    }
}

@Composable
private fun LegendItem(riskLevel: RiskLevel, description: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        RiskIcon(riskLevel, size = 16.dp)
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = description,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun StreamingConfigCard(config: StreamingConfiguration) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = when (config.riskLevel) {
                RiskLevel.LOW -> MaterialTheme.colorScheme.surface
                RiskLevel.MEDIUM -> MaterialTheme.colorScheme.surface
                RiskLevel.HIGH -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f)
                RiskLevel.CRITICAL -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f)
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${config.resolution.displayName} • ${config.fps}fps",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = config.codec.displayName,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RiskIcon(config.riskLevel, size = 24.dp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = config.riskLevel.displayName,
                        fontWeight = FontWeight.Medium,
                        color = when (config.riskLevel) {
                            RiskLevel.LOW -> Color(0xFF4CAF50)      // Success green
                            RiskLevel.MEDIUM -> Color(0xFFF4A538)   // Yellow Orange
                            RiskLevel.HIGH -> Color(0xFFF27E2D)     // Warm Orange
                            RiskLevel.CRITICAL -> Color(0xFFD4472E) // Red Orange
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Bitrate: ${config.bitrate} kbps",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                )
                Text(
                    text = "Quality: ${config.quality}",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = config.description,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                lineHeight = 16.sp
            )
        }
    }
}

@Composable
private fun RiskIcon(riskLevel: RiskLevel, size: androidx.compose.ui.unit.Dp) {
    when (riskLevel) {
        RiskLevel.LOW -> Icon(
            Icons.Default.CheckCircle,
            contentDescription = null,
            tint = Color(0xFF4CAF50),      // Success green
            modifier = Modifier.size(size)
        )
        RiskLevel.MEDIUM -> Icon(
            Icons.Default.Warning,
            contentDescription = null,
            tint = Color(0xFFF4A538),      // Yellow Orange
            modifier = Modifier.size(size)
        )
        RiskLevel.HIGH -> Icon(
            Icons.Default.Warning,
            contentDescription = null,
            tint = Color(0xFFF27E2D),      // Warm Orange
            modifier = Modifier.size(size)
        )
        RiskLevel.CRITICAL -> Icon(
            Icons.Default.Close,
            contentDescription = null,
            tint = Color(0xFFD4472E),      // Red Orange
            modifier = Modifier.size(size)
        )
    }
}

@Composable
private fun ConnectionReportCard(report: com.danihg.bitratelab.network.ConnectionReport) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Connection Analysis Report",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "${report.testDurationSeconds}s test",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Stability Overview
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val stabilityColor = when {
                    report.stabilityScore > 0.8 -> Success
                    report.stabilityScore > 0.6 -> Warning
                    else -> Error
                }

                Icon(
                    imageVector = when {
                        report.stabilityScore > 0.8 -> Icons.Default.CheckCircle
                        report.stabilityScore > 0.6 -> Icons.Default.Warning
                        else -> Icons.Default.Close
                    },
                    contentDescription = null,
                    tint = stabilityColor,
                    modifier = Modifier.size(24.dp)
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Stability Score: ${String.format("%.1f", report.stabilityScore * 100)}%",
                        fontWeight = FontWeight.Bold,
                        color = stabilityColor
                    )
                    Text(
                        text = report.stabilityDescription,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        lineHeight = 16.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Speed Statistics
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                SpeedStatItem(
                    label = "Average",
                    value = "${String.format("%.1f", report.averageSpeed)} Mbps",
                    modifier = Modifier.weight(1f)
                )
                SpeedStatItem(
                    label = "Minimum",
                    value = "${String.format("%.1f", report.minSpeed)} Mbps",
                    modifier = Modifier.weight(1f)
                )
                SpeedStatItem(
                    label = "Maximum",
                    value = "${String.format("%.1f", report.maxSpeed)} Mbps",
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Spike Detection Results
            if (report.hasSpikes) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = null,
                            tint = Warning,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "${report.spikeCount} connection spike(s) detected during test",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                        )
                    }
                }
            } else {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Success,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "No connection spikes detected - stable performance",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Speed Variation
            Text(
                text = "Speed Variation: ${String.format("%.1f", report.speedVariation)}%",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
private fun SpeedStatItem(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = label,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
    }
}