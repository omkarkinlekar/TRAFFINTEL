# 🚦 TRAFFINTEL – Real-Time Traffic Sign Detection App

TRAFFINTEL is an Android app that uses **YOLOv8 + TensorFlow Lite** to detect traffic signs in real time and provide driving recommendations. It runs completely **offline** on-device, offering smooth performance with a clean, modern UI.

---

✨ **Features**

- 📸 Real-time traffic sign detection using YOLOv8-TFLite  
- 🎙️ Voice + text recommendations based on detected signs  
- ⚡ Works fully offline (no internet needed)  
- 📱 Runs at ~30 FPS on supported devices  
- 🖼️ Clean and responsive Material Design UI  
- 🔔 Edge-to-edge layout support with smooth interactions  

---

🛠 **Tech Stack**

- **Language**: Kotlin  
- **Framework**: Android SDK + Jetpack Components  
- **ML Model**: YOLOv8 (TensorFlow Lite, Float32)  
- **UI**: Jetpack Compose + Material 3  
- **Voice Alerts**: Android TextToSpeech (TTS) API  

---

🚀 **How It Works**

1. Open the app on your Android device.  
2. Grant camera permissions when prompted.
3. The a navidation bar at bottom with home , realtime and image screens . (Note : The active screen with be highlighted in yellow in navbar)
4. Start real-time detection – the app identifies traffic signs on the camera feed. (Note :Gpu toggle button when in orange means active and in grey means not active)  
5. The detected sign is displayed on screen with a **driving recommendation**.  
6. A **voice alert** is also provided for hands-free assistance.
7. Image detection Screen Click The select image button to pic sign for information.Then tap the results button for information.  

---

📸 **Screenshots**

<table>
  <tr>
    <td align="center"><b>HOME SCREEN</b></td>
    <td align="center"><b>DETECTION SCREEN</b></td>
  </tr>
  <tr>
    <td align="center"><img src="screenshots/home.jpg" alt="Home Screen" width="300"></td>
    <td align="center"><img src="screenshots/detection.jpg" alt="Detection Screen" width="300"></td>
  </tr>
</table>

---

👨‍💻 **Author**

Developed by [Omkarkinlekar](https://github.com/omkarkinlekar)  



