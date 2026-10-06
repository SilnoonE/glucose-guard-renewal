package com.example.glucoseguard

import android.graphics.Bitmap
import android.view.KeyEvent
import android.view.View
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.android.gms.ads.AdView
import com.example.glucoseguard.ads.PdfInterstitial
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean

@RunWith(AndroidJUnit4::class)
class AdsInstrumentedTest {
    private fun screenshot(name: String) {
        val instrument=InstrumentationRegistry.getInstrumentation()
        val context=instrument.targetContext
        val directory=File(context.getExternalFilesDir(null),"qa").apply { mkdirs() }
        File(directory,"$name.png").outputStream().use { instrument.uiAutomation.takeScreenshot().compress(Bitmap.CompressFormat.PNG,100,it) }
    }
    @Test fun testAdsLoadShowAndContinueAfterDismiss() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            var ready=false
            for(attempt in 0 until 60) {
                scenario.onActivity { activity ->
                    assertEquals("ca-app-pub-3940256099942544/6300978111",activity.getString(R.string.admob_banner_id))
                    ready=activity.isPdfAdReady() && activity.findViewById<AdView>(R.id.adView).visibility==View.VISIBLE
                }
                if(ready) break
                Thread.sleep(500)
            }
            assertTrue("Test banner and interstitial must load",ready)
            screenshot("08_banner_ad")
            val continued=AtomicBoolean(false)
            scenario.onActivity { it.showPdfInterstitial { continued.set(true) } }
            Thread.sleep(2500)
            assertFalse("Opening must wait until the test interstitial is dismissed",continued.get())
            screenshot("09_pdf_interstitial")
            Thread.sleep(3500)
            InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_BACK)
            for(attempt in 0 until 40) {
                if(continued.get()) break
                Thread.sleep(250)
            }
            assertTrue("PDF continuation must run after dismiss",continued.get())
        }
    }
    @Test fun unloadedAdDoesNotBlockReport() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val ad=PdfInterstitial(activity)
                var continued=false
                ad.showThen { continued=true }
                assertTrue(continued)
                ad.destroy()
            }
        }
    }
}
