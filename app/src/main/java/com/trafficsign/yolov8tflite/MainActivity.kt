package com.trafficsign.yolov8tflite

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Matrix
import android.os.Bundle
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.AspectRatio
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.trafficsign.yolov8tflite.Constants.LABELS_PATH
import com.trafficsign.yolov8tflite.Constants.MODEL_PATH
import com.trafficsign.yolov8tflite.databinding.ActivityMainBinding
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import android.speech.tts.TextToSpeech
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import java.util.Locale
private var lastSpokenSigns = setOf<String>()

class MainActivity : AppCompatActivity(), Detector.DetectorListener {
    private lateinit var binding: ActivityMainBinding
    private val isFrontCamera = false

    private var preview: Preview? = null
    private var imageAnalyzer: ImageAnalysis? = null
    private var camera: Camera? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private var detector: Detector? = null

    private lateinit var cameraExecutor: ExecutorService
    private lateinit var tts: TextToSpeech

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
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        tts = TextToSpeech(this) {
            if (it == TextToSpeech.SUCCESS) {
                tts.language = Locale.US
            }
        }

        cameraExecutor = Executors.newSingleThreadExecutor()

        cameraExecutor.execute {
            detector = Detector(baseContext, MODEL_PATH, LABELS_PATH, this)
        }

        updateActiveButton(binding.realTimeButton)

        binding.homeButton.setOnClickListener {
            val intent = Intent(this, HomeActivity::class.java)
            startActivity(intent)
            updateActiveButton(binding.homeButton)
        }

        binding.realTimeButton.setOnClickListener {
            startCamera() // Restart real-time detection
            updateActiveButton(binding.realTimeButton)
        }

        binding.imageButton.setOnClickListener {
            val intent = Intent(this, ImageActivity::class.java)
            startActivity(intent)
            updateActiveButton(binding.imageButton)
        }

        if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }

        bindListeners()
    }

    private fun bindListeners() {
        binding.apply {
            isGpu.setOnCheckedChangeListener { buttonView, isChecked ->
                cameraExecutor.submit {
                    detector?.restart(isGpu = isChecked)
                }
                if (isChecked) {
                    buttonView.setBackgroundColor(ContextCompat.getColor(baseContext, R.color.orange))
                } else {
                    buttonView.setBackgroundColor(ContextCompat.getColor(baseContext, R.color.gray))
                }
            }
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            cameraProvider  = cameraProviderFuture.get()
            bindCameraUseCases()
        }, ContextCompat.getMainExecutor(this))
    }

    private fun bindCameraUseCases() {
        val cameraProvider = cameraProvider ?: throw IllegalStateException("Camera initialization failed.")

        val rotation = binding.viewFinder.display.rotation

        val cameraSelector = CameraSelector
            .Builder()
            .requireLensFacing(CameraSelector.LENS_FACING_BACK)
            .build()

        preview =  Preview.Builder()
            .setTargetAspectRatio(AspectRatio.RATIO_4_3)
            .setTargetRotation(rotation)
            .build()

        imageAnalyzer = ImageAnalysis.Builder()
            .setTargetAspectRatio(AspectRatio.RATIO_4_3)
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .setTargetRotation(binding.viewFinder.display.rotation)
            .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
            .build()

        imageAnalyzer?.setAnalyzer(cameraExecutor) { imageProxy ->
            val bitmapBuffer =
                Bitmap.createBitmap(
                    imageProxy.width,
                    imageProxy.height,
                    Bitmap.Config.ARGB_8888
                )
            imageProxy.use { bitmapBuffer.copyPixelsFromBuffer(imageProxy.planes[0].buffer) }
            imageProxy.close()

            val matrix = Matrix().apply {
                postRotate(imageProxy.imageInfo.rotationDegrees.toFloat())

                if (isFrontCamera) {
                    postScale(
                        -1f,
                        1f,
                        imageProxy.width.toFloat(),
                        imageProxy.height.toFloat()
                    )
                }
            }

            val rotatedBitmap = Bitmap.createBitmap(
                bitmapBuffer, 0, 0, bitmapBuffer.width, bitmapBuffer.height,
                matrix, true
            )

            detector?.detect(rotatedBitmap)
        }

        cameraProvider.unbindAll()

        try {
            camera = cameraProvider.bindToLifecycle(
                this,
                cameraSelector,
                preview,
                imageAnalyzer
            )

            preview?.setSurfaceProvider(binding.viewFinder.surfaceProvider)
        } catch(exc: Exception) {
            Log.e(TAG, "Use case binding failed", exc)
        }
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()) {
        if (it[Manifest.permission.CAMERA] == true) { startCamera() }
    }

    override fun onDestroy() {
        super.onDestroy()
        detector?.close()
        cameraExecutor.shutdown()
    }

    override fun onResume() {
        super.onResume()
        if (allPermissionsGranted()){
            startCamera()
        } else {
            requestPermissionLauncher.launch(REQUIRED_PERMISSIONS)
        }
    }

    companion object {
        private const val TAG = "Camera"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = mutableListOf (
            Manifest.permission.CAMERA
        ).toTypedArray()
    }

    override fun onEmptyDetect() {
        runOnUiThread {
            binding.overlay.clear()
        }
    }

    override fun onDetect(boundingBoxes: List<BoundingBox>, inferenceTime: Long) {
        runOnUiThread {
            val detectedSigns = mutableSetOf<String>()
            val cleanedSignsForTTS = mutableSetOf<String>()
            val expectedSigns = listOf(
                "give_way", "bus_stop", "do_not_enter", "do_not_stop", "do_not_turn_l", "do_not_turn_r", "do_not_u_turn",
                "enter_left_lane", "green_light", "left_right_lane", "no_parking", "parking", "ped_crossing",
                "ped_zebra_cross", "railway_crossing", "red_light", "stop", "t_intersection", "traffic_light",
                "u_turn", "warning", "yellow_light", "left_turn", "right_turn", "speed_breaker", "school_ahead",
                "one_way", "petrol_pump", "no_horn", "compulsory_turn_ahead_or_left",
                "compulsory_turn_ahead_or_right", "barrier_ahead", "compulsory_turn_ahead", "two_way",
                "speed_limit_30", "speed_limit_50", "speed_limit_80", "speed_limit_100"
            )

            for (box in boundingBoxes) {
                val detectedLabel = box.clsName
                if (expectedSigns.contains(detectedLabel)) {
                    detectedSigns.add(detectedLabel)
                    cleanedSignsForTTS.add(detectedLabel.replace("_", " "))
                }
            }

            //binding.inferenceTime.text = "Inference Time: ${inferenceTime}ms"
           // binding.detectionCount.text = "Detected: ${detectedSigns.size}"

            val newSignsToSpeak = cleanedSignsForTTS.subtract(lastSpokenSigns.map { it.replace("_", " ") }.toSet())

            if (newSignsToSpeak.isNotEmpty()) {
                lastSpokenSigns = detectedSigns
                val textToSpeak = newSignsToSpeak.joinToString(separator = ", ")
                tts.speak(textToSpeak, TextToSpeech.QUEUE_FLUSH, null, null)
            }
            binding.inferenceTime.text = "${inferenceTime}ms"
            binding.overlay.apply {
                setResults(boundingBoxes)
                invalidate()
            }
            val imageViews = listOf(
                binding.detectedSignImage1,
                binding.detectedSignImage2,
                // Add more if needed
            )
            for (i in imageViews.indices) {
                if (i < detectedSigns.size) {
                    val resourceId =
                        resources.getIdentifier(detectedSigns.elementAt(i), "drawable", packageName)
                    if (resourceId != 0) {
                        imageViews[i].setImageResource(resourceId)
                        imageViews[i].visibility = View.VISIBLE
                        imageViews[i].bringToFront()
                    } else {
                        imageViews[i].visibility = View.GONE
                    }
                } else {
                    imageViews[i].visibility = View.GONE
                }
            }
        }
    }
}
