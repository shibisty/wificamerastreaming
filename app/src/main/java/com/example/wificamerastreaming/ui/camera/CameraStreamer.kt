package com.example.wificamerastreaming.camera

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Rect
import android.graphics.YuvImage
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.ByteArrayOutputStream
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class CameraStreamer(private val context: Context) {

    private var imageCapture: ImageCapture? = null

    // Последний кадр как Bitmap — для локального отображения
    private val _latestFrame = MutableStateFlow<Bitmap?>(null)
    val latestFrame: StateFlow<Bitmap?> = _latestFrame

    // Поток JPEG-байтов кадров — для раздачи по сети
    private val _frameJpegFlow = MutableSharedFlow<ByteArray>(replay = 0, extraBufferCapacity = 1)
    val frameJpegFlow = _frameJpegFlow.asSharedFlow()

    private var lastEmitTime = 0L
    private val minFrameIntervalMs = 100 // ~10 fps, чтобы не грузить сеть/CPU
    private val captureMutex = Mutex()

    fun bindCamera(lifecycleOwner: LifecycleOwner) {
        val providerFuture = ProcessCameraProvider.getInstance(context)
        providerFuture.addListener({
            val provider = providerFuture.get()

            imageCapture = ImageCapture.Builder().build()

            val analysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()

            analysis.setAnalyzer(ContextCompat.getMainExecutor(context)) { imageProxy ->
                val now = System.currentTimeMillis()
                if (now - lastEmitTime >= minFrameIntervalMs) {
                    lastEmitTime = now
                    val jpegBytes = imageProxyToJpeg(imageProxy)
                    if (jpegBytes != null) {
                        val bmp = BitmapFactory.decodeByteArray(jpegBytes, 0, jpegBytes.size)
                        _latestFrame.value = bmp
                        _frameJpegFlow.tryEmit(jpegBytes)
                    }
                }
                imageProxy.close()
            }

            provider.unbindAll()
            provider.bindToLifecycle(
                lifecycleOwner,
                CameraSelector.DEFAULT_BACK_CAMERA,
                imageCapture,
                analysis
            )
        }, ContextCompat.getMainExecutor(context))
    }

    private fun imageProxyToJpeg(imageProxy: ImageProxy): ByteArray? {
        return try {
            val yBuffer = imageProxy.planes[0].buffer
            val uBuffer = imageProxy.planes[1].buffer
            val vBuffer = imageProxy.planes[2].buffer

            val ySize = yBuffer.remaining()
            val uSize = uBuffer.remaining()
            val vSize = vBuffer.remaining()

            val nv21 = ByteArray(ySize + uSize + vSize)
            yBuffer.get(nv21, 0, ySize)
            vBuffer.get(nv21, ySize, vSize)
            uBuffer.get(nv21, ySize + vSize, uSize)

            val yuvImage = YuvImage(nv21, ImageFormat.NV21, imageProxy.width, imageProxy.height, null)
            val out = ByteArrayOutputStream()
            yuvImage.compressToJpeg(Rect(0, 0, imageProxy.width, imageProxy.height), 70, out)
            val rawJpeg = out.toByteArray()

            val rotationDegrees = imageProxy.imageInfo.rotationDegrees
            if (rotationDegrees == 0) {
                rawJpeg
            } else {
                rotateJpeg(rawJpeg, rotationDegrees)
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun rotateJpeg(jpegBytes: ByteArray, degrees: Int): ByteArray {
        val bitmap = BitmapFactory.decodeByteArray(jpegBytes, 0, jpegBytes.size)
        val matrix = android.graphics.Matrix().apply { postRotate(degrees.toFloat()) }
        val rotatedBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)

        val out = ByteArrayOutputStream()
        rotatedBitmap.compress(Bitmap.CompressFormat.JPEG, 70, out)

        bitmap.recycle()
        rotatedBitmap.recycle()

        return out.toByteArray()
    }

    suspend fun captureAndSaveToGallery(): ByteArray = captureMutex.withLock {
        suspendCancellableCoroutine { cont ->
            val capture = imageCapture ?: run {
                cont.resumeWithException(IllegalStateException("Camera not bound"))
                return@suspendCancellableCoroutine
            }

            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.US).format(System.currentTimeMillis())
            val fileName = "IMG_$timestamp"

            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.MediaColumns.RELATIVE_PATH, "Pictures/CamApp")
                } else {
                    val picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                    val appDir = File(picturesDir, "CamApp").apply { mkdirs() }
                    put(MediaStore.MediaColumns.DATA, File(appDir, "$fileName.jpg").absolutePath)
                }
            }

            val outputOptions = ImageCapture.OutputFileOptions.Builder(
                context.contentResolver,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                contentValues
            ).build()

            capture.takePicture(
                outputOptions,
                ContextCompat.getMainExecutor(context),
                object : ImageCapture.OnImageSavedCallback {
                    override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                        val uri = output.savedUri
                        val bytes = uri?.let { context.contentResolver.openInputStream(it)?.readBytes() }
                        cont.resume(bytes ?: ByteArray(0))
                    }
                    override fun onError(exc: ImageCaptureException) {
                        cont.resumeWithException(exc)
                    }
                }
            )
        }
    }
}
