package com.example.glucoseguard

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.MobileAds
import com.example.glucoseguard.databinding.ActivityMainBinding
import com.example.glucoseguard.util.LocaleHelper
import android.content.Context

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var pdfInterstitial: com.example.glucoseguard.ads.PdfInterstitial
    private var bannerLoaded=false
    private var mainUiVisible=true
    private var keyboardVisible=false
    private fun updateAdVisibility() {
        binding.adView.visibility=if(bannerLoaded && mainUiVisible && !keyboardVisible) View.VISIBLE else View.GONE
    }
    fun showPdfInterstitial(onContinue: () -> Unit) { pdfInterstitial.showThen(onContinue) }
    internal fun isPdfAdReady() = pdfInterstitial.isReady()

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.onAttach(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Status Bar 설정 (다크 그린 배경에 맞게 화이트 아이콘)
        window.statusBarColor = androidx.core.content.ContextCompat.getColor(this, R.color.bg_light)
        androidx.core.view.WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = true
        }

        // Toolbar 설정
        setSupportActionBar(binding.toolbar)
        // 기본 타이틀 제거 (커스텀 TextView 사용 중)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        // Toolbar 아이콘 및 텍스트 색상을 흰색으로 강제 지정
        binding.toolbar.setTitleTextColor(getColor(R.color.text_main))

        pdfInterstitial=com.example.glucoseguard.ads.PdfInterstitial(this)
        binding.adView.adListener=object : com.google.android.gms.ads.AdListener() {
            override fun onAdLoaded() {
                if(isDestroyed) return
                bannerLoaded=true;updateAdVisibility()
                android.util.Log.d("GlucoseAds","Banner loaded")
            }
            override fun onAdFailedToLoad(error: com.google.android.gms.ads.LoadAdError) {
                if(isDestroyed) return
                bannerLoaded=false;updateAdVisibility()
                android.util.Log.w("GlucoseAds","Banner load failed: ${error.code} ${error.message}")
            }
        }
        MobileAds.initialize(this) {
            runOnUiThread {
                if(!isFinishing && !isDestroyed) {
                    binding.adView.loadAd(AdRequest.Builder().build())
                    pdfInterstitial.preload()
                }
            }
        }

        val navController = findNavController(R.id.nav_host_fragment)
        
        // 상위 레벨 데스티네이션 설정 (백버튼 안 나올 곳들)
        val appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.navigation_home,
                R.id.navigation_record,
                R.id.navigation_chart,
                R.id.navigation_settings
            )
        )
        
        setupActionBarWithNavController(navController, appBarConfiguration)
        binding.bottomNavigation.setupWithNavController(navController)

        // 바텀 네비게이션과 툴바 타이틀 연동 등을 위해 목적지 변경 리스너 추가
        navController.addOnDestinationChangedListener { _, destination, _ ->
            binding.toolbarTitle.text = if(destination.id == R.id.navigation_home) "혈당지킴이" else destination.label
            updateAdVisibility()
            // 뒤로가기 버튼 색상을 흰색으로 강제 (녹색 배경 대비)
            binding.toolbar.post {
                binding.toolbar.navigationIcon?.let {
                    androidx.core.graphics.drawable.DrawableCompat.setTint(it, getColor(R.color.text_main))
                }
            }
        }

        androidx.core.view.WindowCompat.setDecorFitsSystemWindows(window, false)
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            val bars=insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars())
            val ime=insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.ime())
            view.setPadding(bars.left,bars.top,bars.right, maxOf(bars.bottom,ime.bottom))
            keyboardVisible=insets.isVisible(androidx.core.view.WindowInsetsCompat.Type.ime())
            binding.bottomNavigation.visibility=if(keyboardVisible || !mainUiVisible) View.GONE else View.VISIBLE
            updateAdVisibility()
            insets
        }
        openNotificationInput(intent)
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent);setIntent(intent);openNotificationInput(intent)
    }
    private fun openNotificationInput(intent: android.content.Intent) {
        if(intent.getBooleanExtra("open_glucose",false)) {
            intent.removeExtra("open_glucose")
            val nav=findNavController(R.id.nav_host_fragment)
            if(nav.currentDestination?.id != R.id.glucoseInputFragment) nav.navigate(R.id.glucoseInputFragment)
        }
    }
    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment)
        return navController.navigateUp() || super.onSupportNavigateUp()
    }

    /**
     * 전체화면 모드(차트 확대 등)를 위해 외부 UI 요소들의 가시성을 조절합니다.
     */
    fun setMainUIVisible(visible: Boolean) {
        mainUiVisible=visible
        val visibility = if (visible) View.VISIBLE else View.GONE
        binding.appBar.visibility = visibility
        binding.bottomNavigation.visibility = visibility
        updateAdVisibility()
    }

    override fun onPause() {
        binding.adView.pause()
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
        binding.adView.resume()
    }

    override fun onDestroy() {
        pdfInterstitial.destroy()
        binding.adView.destroy()
        super.onDestroy()
    }
}
