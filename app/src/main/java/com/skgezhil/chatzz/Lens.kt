package com.skgezhil.chatzz

import android.annotation.SuppressLint
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition

class Lens : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_lens)
    }
}

//private class YourImageAnalyzer : ImageAnalysis.Analyzer {
//
//    @SuppressLint("UnsafeExperimentalUsageError")
//    override fun analyze(imageProxy: ImageProxy) {
//        val mediaImage = imageProxy.image
//        if (mediaImage != null) {
//            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
//            // Pass image to an ML Kit Vision API
//            // ...
//            val recognizer = TextRecognition.getClient()
//            val result = recognizer.process(image)
//                .addOnSuccessListener { visionText ->
//                    // Task completed successfully
//                    // ...
//                }
//                .addOnFailureListener { e ->
//                    // Task failed with an exception
//                    // ...
//                }
//            val resultText = result.text
//            for (block in result.textBlocks) {
//                val blockText = block.text
//                val blockCornerPoints = block.cornerPoints
//                val blockFrame = block.boundingBox
//                for (line in block.lines) {
//                    val lineText = line.text
//                    val lineCornerPoints = line.cornerPoints
//                    val lineFrame = line.boundingBox
//                    for (element in line.elements) {
//                        val elementText = element.text
//                        val elementCornerPoints = element.cornerPoints
//                        val elementFrame = element.boundingBox
//                    }
//                }
//            }
//        }
//    }
//}





