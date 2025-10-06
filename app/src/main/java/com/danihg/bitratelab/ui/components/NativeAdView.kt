package com.danihg.bitratelab.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdOptions
import android.view.LayoutInflater
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import com.danihg.bitratelab.R
import com.google.android.gms.ads.nativead.NativeAdView as GoogleNativeAdView

@Composable
fun NativeAdCard(
    adUnitId: String = "ca-app-pub-3940256099942544/2247696110", // Test native ad unit ID
    modifier: Modifier = Modifier
) {
    var nativeAd by remember { mutableStateOf<NativeAd?>(null) }
    var isAdLoaded by remember { mutableStateOf(false) }
    val context = androidx.compose.ui.platform.LocalContext.current

    DisposableEffect(Unit) {
        val adLoader = AdLoader.Builder(context, adUnitId)
            .forNativeAd { ad ->
                nativeAd = ad
                isAdLoaded = true
            }
            .withAdListener(object : AdListener() {
                override fun onAdFailedToLoad(adError: LoadAdError) {
                    isAdLoaded = false
                    nativeAd = null
                }
            })
            .withNativeAdOptions(
                NativeAdOptions.Builder()
                    .build()
            )
            .build()

        adLoader.loadAd(AdRequest.Builder().build())

        onDispose {
            nativeAd?.destroy()
        }
    }

    if (isAdLoaded && nativeAd != null) {
        Card(
            modifier = modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
        ) {
            AndroidView(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                factory = { ctx ->
                    val inflater = LayoutInflater.from(ctx)
                    val adView = inflater.inflate(
                        R.layout.native_ad_layout,
                        null,
                        false
                    ) as GoogleNativeAdView

                    nativeAd?.let { ad ->
                        // Populate the native ad view
                        adView.headlineView = adView.findViewById(R.id.ad_headline)
                        adView.bodyView = adView.findViewById(R.id.ad_body)
                        adView.callToActionView = adView.findViewById(R.id.ad_call_to_action)
                        adView.iconView = adView.findViewById(R.id.ad_app_icon)

                        (adView.headlineView as? TextView)?.text = ad.headline
                        (adView.bodyView as? TextView)?.text = ad.body
                        (adView.callToActionView as? Button)?.text = ad.callToAction

                        ad.icon?.let { icon ->
                            (adView.iconView as? ImageView)?.setImageDrawable(icon.drawable)
                            adView.iconView?.visibility = android.view.View.VISIBLE
                        } ?: run {
                            adView.iconView?.visibility = android.view.View.GONE
                        }

                        adView.setNativeAd(ad)
                    }

                    adView
                }
            )
        }
    }
}
