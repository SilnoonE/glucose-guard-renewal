package com.example.glucoseguard.ads

import android.app.Activity
import android.os.SystemClock
import android.util.Log
import com.google.android.gms.ads.*
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.example.glucoseguard.R

/** Activity-owned ad; report opening always continues after dismiss or failure. */
class PdfInterstitial(private val activity: Activity) {
    private var ad: InterstitialAd?=null
    private var loadedAt=0L
    private var loading=false
    private var showing=false
    private var destroyed=false
    private var continuation: (() -> Unit)?=null
    fun isReady()=ad!=null && SystemClock.elapsedRealtime()-loadedAt<55*60*1000L && !destroyed
    fun preload() {
        if(destroyed || loading || showing || isReady() || activity.isFinishing || activity.isDestroyed) return
        ad=null;loading=true
        InterstitialAd.load(activity,activity.getString(R.string.admob_pdf_interstitial_id),AdRequest.Builder().build(),object : InterstitialAdLoadCallback() {
            override fun onAdLoaded(loaded: InterstitialAd) {
                loading=false
                if(destroyed) return
                ad=loaded;loadedAt=SystemClock.elapsedRealtime()
                Log.d("GlucoseAds","PDF interstitial loaded")
            }
            override fun onAdFailedToLoad(error: LoadAdError) {
                loading=false;ad=null
                Log.w("GlucoseAds","PDF interstitial load failed: ${error.code} ${error.message}")
            }
        })
    }
    fun showThen(onContinue: () -> Unit) {
        if(destroyed || activity.isFinishing || activity.isDestroyed || showing) return
        val ready=if(isReady()) ad else null
        if(ready==null) { preload();onContinue();return }
        ad=null;showing=true;continuation=onContinue
        ready.fullScreenContentCallback=object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() { finish() }
            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                Log.w("GlucoseAds","PDF interstitial show failed: ${error.code}");finish()
            }
            override fun onAdShowedFullScreenContent() { Log.d("GlucoseAds","PDF interstitial shown") }
        }
        try { ready.show(activity) }
        catch(e: Exception) { Log.w("GlucoseAds","PDF interstitial unavailable",e);finish() }
    }
    private fun finish() {
        if(!showing) return
        showing=false
        val next=continuation;continuation=null
        if(!destroyed && !activity.isFinishing && !activity.isDestroyed) { next?.invoke();preload() }
    }
    fun destroy() { destroyed=true;ad=null;continuation=null }
}
