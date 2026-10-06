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

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.onAttach(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Status Bar 설정 (다크 그린 배경에 맞게 화이트 아이콘)
        window.statusBarColor = androidx.core.content.ContextCompat.getColor(this, R.color.primary)
        androidx.core.view.WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = false
        }

        // Toolbar 설정
        setSupportActionBar(binding.toolbar)
        // 기본 타이틀 제거 (커스텀 TextView 사용 중)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        // Toolbar 아이콘 및 텍스트 색상을 흰색으로 강제 지정
        binding.toolbar.setTitleTextColor(android.graphics.Color.WHITE)

        // AdMob 초기화
        MobileAds.initialize(this) {}
        val adRequest = AdRequest.Builder().build()
        binding.adView.loadAd(adRequest)

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
        navController.addOnDestinationChangedListener { _, _, _ ->
            // 뒤로가기 버튼 색상을 흰색으로 강제 (녹색 배경 대비)
            binding.toolbar.post {
                binding.toolbar.navigationIcon?.let {
                    androidx.core.graphics.drawable.DrawableCompat.setTint(it, android.graphics.Color.WHITE)
                }
            }
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
        val visibility = if (visible) View.VISIBLE else View.GONE
        binding.appBar.visibility = visibility
        binding.bottomNavigation.visibility = visibility
        binding.adView.visibility = visibility
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
        binding.adView.destroy()
        super.onDestroy()
    }
}
