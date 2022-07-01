package com.example.todaywisesaying

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.example.todaywisesaying.databinding.ActivityMainBinding
import com.google.firebase.ktx.Firebase
import com.google.firebase.remoteconfig.ktx.remoteConfig
import com.google.firebase.remoteconfig.ktx.remoteConfigSettings
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.abs
import kotlin.math.absoluteValue
import kotlin.math.max

class MainActivity : AppCompatActivity() {
    val TAG: String = "로그"

    companion object {
        private const val ZOOM_OUT_MIN_SCALE = 0.85F
        private const val DEPTH_MIN_SCALE = 0.75F
        private const val MIN_ALPHA = 0.5F
    }

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initRemoteConfig()

    }

    private fun initRemoteConfig() {
        // RemoteConfig 객체 생성
        val remoteConfig = Firebase.remoteConfig
        // 관련 설정 값
        val configSettings = remoteConfigSettings {
            minimumFetchIntervalInSeconds = 1000
        }
        // 설정 값 연동
        remoteConfig.setConfigSettingsAsync(configSettings)

        // 인앱 매개변수 기본 값 설정
        remoteConfig.setDefaultsAsync(R.xml.remote_config_defaults)
        // Fetch & Activate
        remoteConfig.fetchAndActivate()
            .addOnCompleteListener { task ->
                binding.loadingIndicator.visibility = View.GONE
                if (task.isSuccessful) {
                    val quotes = parseJSON(remoteConfig.getString("quotes"))
                    val isNameRevealed = remoteConfig.getBoolean("is_name_revealed")

                    initViewPager(quotes, isNameRevealed)
                } else {
                    Toast.makeText(this, "Fetch failed",
                        Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun parseJSON(json: String): List<Quote> {
        val jsonArray = JSONArray(json)
        var jsonList = emptyList<JSONObject>()

        for (idx in 0 until jsonArray.length()) {
            val jsonObject = jsonArray.getJSONObject(idx)
            jsonObject?.let {
                jsonList = jsonList + it
            }
        }

        return jsonList.map {
            Quote(
                quote = it.getString("quote"),
                name = it.getString("name")
            )
        }
    }

    private fun initViewPager(quotes: List<Quote>, isNameRevealed: Boolean) {
        // RecyclerView.Adapter
        val adapter = QuotesPagerAdapter(
            quotes = quotes,
            isNameRevealed = isNameRevealed
        )
        // viewPager와 RecylcerView 연동
        binding.viewPager.adapter = adapter

        // 앱 시작 시 처음 아이템에서 시작하게 함
        binding.viewPager.setCurrentItem(adapter.itemCount / 2 - 3, false)
        // viewPager의 orientation
        binding.viewPager.orientation = ViewPager2.ORIENTATION_HORIZONTAL

        // 페이지가 바뀔 때마다 호출되는 메소드
        binding.viewPager.registerOnPageChangeCallback(
            object : ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    super.onPageSelected(position)
                    Log.d(TAG, "initViewPager() - onPageSelected() called :: Page=${position + 1}")
                }
            }
        )

        // PageTransformer 구현
        // Paging될 때 애니메이션 효과
        setPageTransform()
    }

    private fun setPageTransform() {
        binding.viewPager.setPageTransformer { page, position ->
            // 방법 총 3가지 있음

            // 1. 페이지 축소하며 변화
//            zoomOutTransform(page, position)
            // 2. 심도 애니메이션을 사용하며 변화
//            depthTransform(page, position)
            // 3. 여운을 남기는 효과 (강의 방식)
            lingerTransform(page, position)
        }
    }

    private fun zoomOutTransform(page: View, position: Float) {
        when {
            position < -1 -> {  // [-Infinity, -1)
                // 첫 번째 아이템에서 왼쪽 방향으로 넘길 때
                // 넘겨지지 않게 함
                page.alpha = 0f
            }
            position <= 1 -> {  // [-1, 1]
                // 슬라이드 변화
                val scaleFactor = max(ZOOM_OUT_MIN_SCALE, 1 - abs(position))
                val vertMargin = page.height * (1 - scaleFactor) / 2
                val horzMargin = page.width * (1 - scaleFactor) / 2

                page.translationX = if (position < 0) {
                    horzMargin - vertMargin / 2
                } else {
                    horzMargin + vertMargin / 2
                }

                // 페이지 축소 ( MIN_SCALE 과 1사이)
                page.scaleX = scaleFactor
                page.scaleY = scaleFactor

                // 페이지가 점점 사라지도록 하기
                page.alpha = (MIN_ALPHA +
                        (((scaleFactor - ZOOM_OUT_MIN_SCALE) / (1 - ZOOM_OUT_MIN_SCALE)) * (1 - MIN_ALPHA)))
            }
            else -> {   // (1, +Infinity]
                // 마지막 아이템에서 오른쪽 방향으로 넘길 때
                // 넘겨지지 않게 함
                page.alpha = 0f
            }
        }
    }

    private fun depthTransform(page: View, position: Float) {
        when {
            position < -1 -> {  // [-Infinity, -1)
                // 첫 번째 아이템에서 왼쪽 방향으로 넘길 때
                // 넘겨지지 않게 함
                page.alpha = 0f
            }
            position <= 0 -> {  // [-1, 0]
                // 왼쪽 방향으로 넘길 때
                page.apply {
                    alpha = 1f
                    translationX = 0f
                    scaleX = 1f
                    scaleY = 1f
                }
            }
            position <= 1 -> {  // [0, 1]
                // 오른쪽 방향으로 넘길 때
                // 페이드 아웃 방식 사용
                page.alpha = 1 - position

                // 기본 애니메이션 대응 (X를 음수화하여 화면 슬라이드 차단)
                page.translationX = -position * page.width

                // 페이지 축소 ( DEPTH_MIN_SCALE 과 1 사이)
                val scaleFactor = (DEPTH_MIN_SCALE + (1 - DEPTH_MIN_SCALE) * (1 - abs(position)))
                page.scaleX = scaleFactor
                page.scaleY = scaleFactor
            }
            else -> {   // (1, +Infinity]
                // 마지막 아이템에서 오른쪽 방향으로 넘길 때
                // 넘겨지지 않게 함
                page.alpha = 0f
            }
        }
    }

    private fun lingerTransform(page: View, position: Float) {
        when {
            position.absoluteValue > 1f -> {    // [-Infinity, -1) OR (1, +Infinity]
                page.alpha = 0f
            }
            position == 0f -> { // 0
                page.alpha = 1f
            }
            else -> {   // [-1, 0) OR (0, 1]
                page.alpha = 1f - 1.5f * position.absoluteValue
            }
        }
    }
}