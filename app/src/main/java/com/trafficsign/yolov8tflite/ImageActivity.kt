package com.trafficsign.yolov8tflite

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.trafficsign.yolov8tflite.databinding.ActivityImageBinding
import java.io.IOException

class ImageActivity : AppCompatActivity(), Detector.DetectorListener {

    private lateinit var binding: ActivityImageBinding
    private lateinit var detector: Detector
    private var selectedImage: Bitmap? = null
    private lateinit var imageView: ImageView
    private lateinit var resultsText: TextView
    private lateinit var imageNameText: TextView
    private lateinit var inferenceTimeText: TextView
    private lateinit var resultsContainer: View
    private lateinit var selectImage: Button
    private lateinit var downloadResults: Button
   // private lateinit var progressBar: ProgressBar
    private val listOfResults = mutableListOf<String>()

    private fun updateActiveButton(activeButton: LinearLayout) {
        // Define the color resources for active and inactive states
        val activeColor = ContextCompat.getColor(this, R.color.yellow)  // Replace with your active color
        val inactiveColor = ContextCompat.getColor(this, R.color.white) // Replace with your inactive color

        // Get ImageViews for each button
        val homeIcon = binding.homeButton.findViewById<ImageView>(R.id.homeIcon)
        val realTimeIcon = binding.realTimeButton.findViewById<ImageView>(R.id.realtimeIcon)
        val imageIcon = binding.imageButton.findViewById<ImageView>(R.id.imageIcon)

        // Get TextViews for each button label
        val homeText = binding.homeButton.findViewById<TextView>(R.id.homeText)
        val realTimeText = binding.realTimeButton.findViewById<TextView>(R.id.realtimeText)
        val imageText = binding.imageButton.findViewById<TextView>(R.id.imageText)

        // Reset all buttons to inactive state
        homeIcon.setColorFilter(inactiveColor)
        realTimeIcon.setColorFilter(inactiveColor)
        imageIcon.setColorFilter(inactiveColor)

        homeText.setTextColor(inactiveColor)
        realTimeText.setTextColor(inactiveColor)
        imageText.setTextColor(inactiveColor)

        // Set the clicked button to active state
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityImageBinding.inflate(layoutInflater)
        setContentView(binding.root)
        imageView = findViewById(R.id.image_view)
        resultsText = findViewById(R.id.pick_results_list)
        imageNameText = findViewById(R.id.image_name)
        inferenceTimeText = findViewById(R.id.inference_time)
        resultsContainer = findViewById(R.id.results_container)
        selectImage = findViewById(R.id.select_button)
        downloadResults = findViewById(R.id.download_button)
        val inactiveColor = ContextCompat.getColor(this, R.color.white) // Replace with your inactive color

        // Set initial active state
        updateActiveButton(binding.imageButton)

        binding.homeButton.setOnClickListener {
            val intent = Intent(this, HomeActivity::class.java)
            startActivity(intent)
            updateActiveButton(binding.homeButton)
        }

        binding.realTimeButton.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            updateActiveButton(binding.realTimeButton)
        }

        try {
            detector = Detector(applicationContext, Constants.MODEL_PATH, Constants.LABELS_PATH, this)
            //detector.setup()
        } catch (e: Exception) {
            Log.e("ImageActivity", "Error initializing detector", e)
        }

        selectImage.setOnClickListener { imageChooser.launch("image/*") }
        downloadResults.setOnClickListener {
            if (listOfResults.isNotEmpty()) {
                resultsContainer.visibility = View.VISIBLE
                Toast.makeText(this, "Results Displayed", Toast.LENGTH_SHORT).show()
                downloadResults.setTextColor(inactiveColor)
            } else {
                //resultsContainer.visibility = View.VISIBLE
                Toast.makeText(this, "No results available yet", Toast.LENGTH_SHORT).show()
                downloadResults.setTextColor(inactiveColor)
            }
        }
    }

    private val imageChooser = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            try {
                selectedImage = MediaStore.Images.Media.getBitmap(contentResolver, it)
                imageView.setImageBitmap(selectedImage)
                val fileName = getFileNameFromUri(it)
                imageNameText.text = "Image Name: $fileName"
                //progressBar.visibility = View.VISIBLE
                resultsContainer.visibility = View.GONE
                val activeColor = ContextCompat.getColor(this, R.color.yellow)
                downloadResults.setTextColor(activeColor)
                val resizedImage = Bitmap.createScaledBitmap(selectedImage!!, 640, 640, false)
                //detector.detect(resizedImage)
                Thread {
                    try {
                        detector.detect(resizedImage!!)
                    } catch (e: Exception) {
                        Log.e("DetectorThread", "Error during detection", e)
                    }
                }.start()
            } catch (e: IOException) {
                Log.e("ImageActivity", "Error loading image", e)

            }
        }
    }

    private fun getFileNameFromUri(uri: Uri): String {
        val cursor = contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            val nameIndex = it.getColumnIndex(MediaStore.Images.Media.DISPLAY_NAME)
            it.moveToFirst()
            return it.getString(nameIndex)
        }
        return "unknown.jpg"
    }

    override fun onEmptyDetect() {
        runOnUiThread {
            //progressBar.visibility = View.GONE
            binding.overlay.invalidate()
            resultsText.text = "Signs Detected: No signs detected."
        }
    }

    override fun onDetect(boundingBoxes: List<BoundingBox>, inferenceTime: Long) {
        runOnUiThread {
            Log.d("ImageActivity", "onDetect called with ${boundingBoxes.size} boxes")
            //progressBar.visibility = View.GONE
            resultsContainer.visibility = View.GONE
            listOfResults.clear()
            val classToBestBox = mutableMapOf<String, BoundingBox>()

            boundingBoxes.forEach { box ->
                val existingBox = classToBestBox[box.clsName]
                if (existingBox == null || box.cnf > existingBox.cnf) {
                    classToBestBox[box.clsName] = box
                }
            }

            listOfResults.addAll(classToBestBox.values.map { box ->
                "${box.clsName} (${box.cnf}%)"
            })
            resultsText.text = "Signs Detected: ${listOfResults.size}\n${listOfResults.joinToString("\n")}"
            inferenceTimeText.text = "Latency: ${inferenceTime}ms"
            binding.overlay.apply {
                setResults(boundingBoxes)
                invalidate()
            }
        }
    }
}
