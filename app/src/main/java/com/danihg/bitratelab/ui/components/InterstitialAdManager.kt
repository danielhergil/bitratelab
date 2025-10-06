package com.danihg.bitratelab.ui.components

import android.app.Activity
import android.content.Context
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback

class InterstitialAdManager(
    private val context: Context,
    private val adUnitId: String = "ca-app-pub-3940256099942544/1033173712" // Test interstitial ad unit ID
) {
    private var interstitialAd: InterstitialAd? = null
    private var isLoading = false
    private var onAdDismissed: (() -> Unit)? = null

    fun loadAd() {
        if (isLoading || interstitialAd != null) return

        isLoading = true
        val adRequest = AdRequest.Builder().build()

        InterstitialAd.load(
            context,
            adUnitId,
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdFailedToLoad(adError: LoadAdError) {
                    isLoading = false
                    interstitialAd = null
                    // Silently fail - don't block user flow
                }

                override fun onAdLoaded(ad: InterstitialAd) {
                    isLoading = false
                    interstitialAd = ad
                }
            }
        )
    }

    fun showAd(activity: Activity, onDismissed: () -> Unit) {
        onAdDismissed = onDismissed

        interstitialAd?.let { ad ->
            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    // Ad dismissed, continue to results
                    interstitialAd = null
                    onAdDismissed?.invoke()
                    // Preload next ad
                    loadAd()
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    // Failed to show, continue anyway
                    interstitialAd = null
                    onAdDismissed?.invoke()
                    loadAd()
                }

                override fun onAdShowedFullScreenContent() {
                    // Ad is showing
                    interstitialAd = null
                }
            }
            ad.show(activity)
        } ?: run {
            // No ad loaded, continue immediately
            onAdDismissed?.invoke()
            // Try to load for next time
            loadAd()
        }
    }

    fun isAdReady(): Boolean = interstitialAd != null
}
