package com.trafficsign.yolov8tflite

import android.content.Intent
import android.os.Bundle
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.ImageView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.trafficsign.yolov8tflite.databinding.ActivitySplashScreenBinding
import com.bumptech.glide.Glide


class SplashScreenActivity : AppCompatActivity(), Detector.DetectorListener{

    private lateinit var binding: ActivitySplashScreenBinding
    private lateinit var detector: Detector
    private var isDetectorInitialized = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Inflate and set binding layout
        binding = ActivitySplashScreenBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Handle system window insets (status bar, etc.)
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Initial state: invisible and slightly smaller
        binding.splash.apply {
            alpha = 0f
            scaleX = 0.8f
            scaleY = 0.8f

            // Animate: fade in and scale up with interpolator
            animate()
                .alpha(1f)
                .scaleX(1.0f)
                .scaleY(1.0f)
                .setDuration(1800)
                .setInterpolator(AccelerateDecelerateInterpolator())
                .withEndAction {
                    startActivity(Intent(this@SplashScreenActivity, MainActivity::class.java))
                    overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
                    finish()
                }
                .start()
        }
        val policeGif = findViewById<ImageView>(R.id.policeGif)
        Glide.with(this).asGif().load(R.drawable.traffic_police).into(policeGif)
    }

    override fun onDetect(boundingBoxes: List<BoundingBox>, inferenceTime: Long) {
        // you can leave this empty if you don't need detection results here
    }

    override fun onEmptyDetect() {
        // leave empty if you want
    }
}
