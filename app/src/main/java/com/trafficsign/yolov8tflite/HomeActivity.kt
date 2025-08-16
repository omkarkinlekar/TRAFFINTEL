package com.trafficsign.yolov8tflite

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.trafficsign.yolov8tflite.databinding.ActivityHomeBinding // Import generated binding class

class HomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHomeBinding // Declare binding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Inflate layout using View Binding
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        updateActiveButton(binding.homeButton)

        binding.realTimeButton.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            updateActiveButton(binding.realTimeButton)
        }

        binding.imageButton.setOnClickListener {
            val intent = Intent(this, ImageActivity::class.java)
            startActivity(intent)
            updateActiveButton(binding.imageButton)
        }
    }

    private fun updateActiveButton(activeButton: LinearLayout) {
        val activeColor = ContextCompat.getColor(this, R.color.yellow)
        val inactiveColor = ContextCompat.getColor(this, R.color.white)

        val homeIcon = binding.homeButton.findViewById<ImageView>(R.id.homeIcon)
        val realTimeIcon = binding.realTimeButton.findViewById<ImageView>(R.id.realtimeIcon)
        val imageIcon = binding.imageButton.findViewById<ImageView>(R.id.imageIcon)

        val homeText = binding.homeButton.findViewById<TextView>(R.id.homeText)
        val realTimeText = binding.realTimeButton.findViewById<TextView>(R.id.realtimeText)
        val imageText = binding.imageButton.findViewById<TextView>(R.id.imageText)

        homeIcon.setColorFilter(inactiveColor)
        realTimeIcon.setColorFilter(inactiveColor)
        imageIcon.setColorFilter(inactiveColor)

        homeText.setTextColor(inactiveColor)
        realTimeText.setTextColor(inactiveColor)
        imageText.setTextColor(inactiveColor)

        when (activeButton) {
            binding.homeButton -> {
                homeIcon.setColorFilter(activeColor)
                homeText.setTextColor(activeColor)
            }
            binding.realTimeButton -> {
                realTimeIcon.setColorFilter(activeColor)
                realTimeText.setTextColor(activeColor)
            }
            binding.imageButton -> {
                imageIcon.setColorFilter(activeColor)
                imageText.setTextColor(activeColor)
            }
        }
    }
}
