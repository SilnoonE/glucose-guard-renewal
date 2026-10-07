package com.example.glucoseguard

import android.graphics.Bitmap
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.android.material.bottomnavigation.BottomNavigationView
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class NavigationInsetsTest {
    @Test fun bottomSystemInsetIsReservedOnceAcrossNavigationModesAndKeyboard() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            InstrumentationRegistry.getInstrumentation().waitForIdleSync()
            scenario.onActivity { activity ->
                val root=activity.findViewById<android.view.View>(R.id.main)
                val nav=activity.findViewById<BottomNavigationView>(R.id.bottom_navigation)
                val density=activity.resources.displayMetrics.density
                val dp={ value: Int -> (value*density+0.5f).toInt() }
                val original=ViewCompat.getRootWindowInsets(root)!!
                val baselinePadding=nav.paddingBottom
                assertEquals(dp(64),nav.height)
                assertEquals("The menu must not reserve another system inset",0,baselinePadding)
                assertEquals(dp(8),nav.itemPaddingBottom)
                for(bottom in listOf(dp(24),dp(48),dp(24))) {
                    val bars=WindowInsetsCompat.Builder(original)
                        .setInsets(WindowInsetsCompat.Type.systemBars(),Insets.of(0,dp(24),0,bottom))
                        .setVisible(WindowInsetsCompat.Type.ime(),false)
                        .setInsets(WindowInsetsCompat.Type.ime(),Insets.NONE).build()
                    val childInsets=ViewCompat.dispatchApplyWindowInsets(root,bars)
                    ViewCompat.dispatchApplyWindowInsets(nav,childInsets)
                    assertEquals(bottom,root.paddingBottom)
                    assertEquals("Gesture and three-button bars must not add menu padding",baselinePadding,nav.paddingBottom)
                    assertEquals(0,childInsets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom)
                }
                val keyboard=WindowInsetsCompat.Builder(original)
                    .setInsets(WindowInsetsCompat.Type.ime(),Insets.of(0,0,0,dp(280)))
                    .setVisible(WindowInsetsCompat.Type.ime(),true).build()
                ViewCompat.dispatchApplyWindowInsets(root,keyboard)
                assertEquals(dp(280),root.paddingBottom)
                assertEquals(android.view.View.GONE,nav.visibility)
                ViewCompat.dispatchApplyWindowInsets(root,original)
                ViewCompat.requestApplyInsets(root)
                assertEquals(android.view.View.VISIBLE,nav.visibility)
            }
            InstrumentationRegistry.getInstrumentation().waitForIdleSync()
            val context=InstrumentationRegistry.getInstrumentation().targetContext
            val dir=File(context.getExternalFilesDir(null),"qa").apply { mkdirs() }
            val shot=InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
            File(dir,"10_bottom_navigation_fixed.png").outputStream().use { shot.compress(Bitmap.CompressFormat.PNG,100,it) }
            assertTrue(File(dir,"10_bottom_navigation_fixed.png").length()>0)
        }
    }
}
