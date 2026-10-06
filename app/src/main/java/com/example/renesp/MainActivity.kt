package com.example.renesp

import android.Manifest
import android.graphics.*
import android.os.Bundle
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.task.vision.detector.ObjectDetector
import java.util.concurrent.Executors
import kotlin.math.tan

class MainActivity : ComponentActivity() {
    private lateinit var preview: PreviewView
    private lateinit var overlay: OverlayView
    private lateinit var detector: ObjectDetector
    private val exec = Executors.newSingleThreadExecutor()

    private val info = mapOf(
        "person" to ("Orang" to 1.7f), "bicycle" to ("Sepeda" to 1.0f),
        "motorcycle" to ("Motor" to 1.2f), "car" to ("Mobil" to 1.5f),
        "bus" to ("Bus" to 3.0f), "truck" to ("Truk" to 3.0f),
        "dog" to ("Anjing" to 0.5f), "cat" to ("Kucing" to 0.3f),
        "traffic light" to ("Lampu Merah" to 0.8f), "bench" to ("Bangku" to 0.8f)
    )
    private val vFovDeg = 50.0

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        preview = PreviewView(this).apply { scaleType = PreviewView.ScaleType.FILL_CENTER }
        overlay = OverlayView(this)
        setContentView(FrameLayout(this).apply { addView(preview); addView(overlay) })

        detector = ObjectDetector.createFromFileAndOptions(
            this, "efficientdet_lite0.tflite",
            ObjectDetector.ObjectDetectorOptions.builder()
                .setMaxResults(8).setScoreThreshold(0.2f).build()
        )
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
            if (ok) startCamera()
        }.launch(Manifest.permission.CAMERA)
    }

    private fun startCamera() {
        val f = ProcessCameraProvider.getInstance(this)
        f.addListener({
            val p = f.get()
            val prev = Preview.Builder().build().also { it.setSurfaceProvider(preview.surfaceProvider) }
            val ana = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                .build()
            ana.setAnalyzer(exec) { proxy -> analyze(proxy) }
            p.unbindAll()
            p.bindToLifecycle(this, CameraSelector.DEFAULT_BACK_CAMERA, prev, ana)
        }, ContextCompat.getMainExecutor(this))
    }

    private fun analyze(proxy: ImageProxy) {
        val rot = proxy.imageInfo.rotationDegrees
        var bmp = proxy.toBitmap()
        if (rot != 0) bmp = Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height,
            Matrix().apply { postRotate(rot.toFloat()) }, true)
        proxy.close()

        val focalPx = bmp.height / (2.0 * tan(Math.toRadians(vFovDeg / 2)))
        val out = detector.detect(TensorImage.fromBitmap(bmp)).mapNotNull { d ->
            val c = d.categories.first()
            val (name, h) = info[c.label] ?: (c.label to 1.0f)
            val boxH = d.boundingBox.height()
            if (boxH <= 1f) return@mapNotNull null
            Det(d.boundingBox, name, c.score, (h * focalPx / boxH).toFloat())
        }
        overlay.update(out, bmp.width, bmp.height)
    }
}
