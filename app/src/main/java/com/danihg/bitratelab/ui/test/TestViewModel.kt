package com.danihg.bitratelab.ui.test

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.danihg.bitratelab.network.NetworkTestResult
import com.danihg.bitratelab.network.NetworkTester
import com.danihg.bitratelab.streaming.StreamingConfiguration
import com.danihg.bitratelab.streaming.StreamingRecommendationEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class TestUiState(
    val isLoading: Boolean = false,
    val testProgress: Int = 0,
    val currentTestStep: String = "",
    val testResult: NetworkTestResult? = null,
    val recommendations: List<StreamingConfiguration> = emptyList(),
    val error: String? = null
)

class TestViewModel(application: Application) : AndroidViewModel(application) {
    private val networkTester = NetworkTester(application)
    private val recommendationEngine = StreamingRecommendationEngine()

    private val _uiState = MutableStateFlow(TestUiState())
    val uiState: StateFlow<TestUiState> = _uiState.asStateFlow()

    fun startNetworkTest() {
        viewModelScope.launch {
            try {
                _uiState.value = TestUiState(
                    isLoading = true,
                    testProgress = 0,
                    currentTestStep = "Initializing comprehensive test..."
                )

                // Run the actual comprehensive network test with progress callbacks
                val testResult = networkTester.runComprehensiveTest { progress, step ->
                    updateProgress(progress, step)
                }

                updateProgress(98, "Generating streaming recommendations...")
                val recommendations = recommendationEngine.generateRecommendations(testResult)

                updateProgress(100, "Analysis complete!")
                kotlinx.coroutines.delay(500)

                _uiState.value = TestUiState(
                    isLoading = false,
                    testProgress = 100,
                    currentTestStep = "Test completed successfully",
                    testResult = testResult,
                    recommendations = recommendations
                )

            } catch (e: Exception) {
                _uiState.value = TestUiState(
                    isLoading = false,
                    error = "Test failed: ${e.message}"
                )
            }
        }
    }

    private fun updateProgress(progress: Int, step: String) {
        _uiState.value = _uiState.value.copy(
            testProgress = progress,
            currentTestStep = step
        )
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun resetTest() {
        _uiState.value = TestUiState()
    }
}