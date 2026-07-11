package com.example.wificamerastreaming.camera

import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import android.content.ContentValues
import android.content.Context
import android.provider.MediaStore
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.suspendCancellableCoroutine
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class CameraController(private val context: Context) {

    private var imageCapture: ImageCapture? = null

    fun bindCamera(previewView: PreviewView, lifecycleOwner: LifecycleOwner) {
        val providerFuture = ProcessCameraProvider.getInstance(context)
        providerFuture.addListener({
            val provider = providerFuture.get()

            val preview = androidx.camera.core.Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }
            imageCapture = ImageCapture.Builder().build()

            provider.unbindAll()
            provider.bindToLifecycle(
                lifecycleOwner,
                androidx.camera.core.CameraSelector.DEFAULT_BACK_CAMERA,
                preview,
                imageCapture
            )
        }, ContextCompat.getMainExecutor(context))
    }

    suspend fun captureAndSaveToGallery(): ByteArray = suspendCancellableCoroutine { cont ->
        val capture = imageCapture ?: run {
            cont.resumeWithException(IllegalStateException("Camera not bound"))
            return@suspendCancellableCoroutine
        }

        val name = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(System.currentTimeMillis())
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, "IMG_$name")
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.RELATIVE_PATH, "Pictures/CamApp")
            } else {
                // API 24-28: путь на файловую систему напрямую
                val picturesDir = android.os.Environment.getExternalStoragePublicDirectory(
                    android.os.Environment.DIRECTORY_PICTURES
                )
                val appDir = java.io.File(picturesDir, "CamApp").apply { mkdirs() }
                put(MediaStore.MediaColumns.DATA, java.io.File(appDir, "IMG_$name.jpg").absolutePath)
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