
package com.example.intruder

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.ImageFormat
import android.hardware.camera2.*
import android.media.ImageReader
import android.os.*
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit
import android.util.Size
import kotlin.collections.ArrayList
import android.graphics.Matrix
import android.graphics.RectF
import android.graphics.SurfaceTexture
import android.hardware.camera2.params.StreamConfigurationMap
import android.view.Surface
import android.media.Image
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraManager
import java.nio.ByteBuffer
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.ByteArrayOutputStream
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class AntiTheftService : Service() {

    private val TAG = "AntiTheftService"
    private var failedAttemptCount = 0
    private val DEFAULT_THRESHOLD = 3 // Le seuil par défaut

    // Pour la gestion de la caméra
    private var cameraManager: CameraManager? = null
    private var cameraId: String? = null
    private var cameraDevice: CameraDevice? = null
    private var captureSession: CameraCaptureSession? = null
    private var imageReader: ImageReader? = null
    private var backgroundHandler: Handler? = null
    private var backgroundThread: HandlerThread? = null
    private val cameraOpenCloseLock = Semaphore(1)

    companion object {
        const val ACTION_PASSWORD_FAILED = "com.yourcompany.anti_theft_app.PASSWORD_FAILED"
        const val ACTION_PASSWORD_SUCCEEDED = "com.yourcompany.anti_theft_app.PASSWORD_SUCCEEDED"
        const val ACTION_TOGGLE_MONITORING = "com.yourcompany.anti_theft_app.TOGGLE_MONITORING"
        const val EXTRA_ENABLED_STATE = "com.yourcompany.anti_theft_app.ENABLED_STATE"

        // Ajoutez une méthode pour obtenir le seuil configuré par l'utilisateur
        fun getThresholdFromPrefs(context: Context): Int {
            val prefs = context.getSharedPreferences("anti_theft_prefs", Context.MODE_PRIVATE)
            return prefs.getInt("failed_attempts_threshold", 3) // Valeur par défaut 3
        }

        // Ajoutez une méthode pour obtenir l'email configuré par l'utilisateur
        fun getEmailFromPrefs(context: Context): String {
            val prefs = context.getSharedPreferences("anti_theft_prefs", Context.MODE_PRIVATE)
            return prefs.getString("destination_email", "votre_email@example.com") ?: "votre_email@example.com"
        }

         fun isMonitoringEnabled(context: Context): Boolean {
            val prefs = context.getSharedPreferences("anti_theft_prefs", Context.MODE_PRIVATE)
            return prefs.getBoolean("is_monitoring_enabled", false)
        }
    }

    override fun onCreate() {
        super.onCreate()
        startBackgroundThread()
        cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
        Log.d(TAG, "AntiTheftService created.")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand: ${intent?.action}")

        val isMonitoringActive = isMonitoringEnabled(this)
        if (!isMonitoringActive) {
            Log.d(TAG, "Monitoring is disabled. Ignoring event.")
            return START_NOT_STICKY
        }

        when (intent?.action) {
            ACTION_PASSWORD_FAILED -> {
                failedAttemptCount++
                Log.d(TAG, "Failed attempts: $failedAttemptCount")
                val threshold = getThresholdFromPrefs(this)
                if (failedAttemptCount >= threshold) {
                    Log.d(TAG, "Threshold reached! Taking photo.")
                    takePhoto()
                    // Réinitialiser le compteur après avoir pris la photo
                    failedAttemptCount = 0
                }
            }
            ACTION_PASSWORD_SUCCEEDED -> {
                Log.d(TAG, "Password succeeded. Resetting failed attempts.")
                failedAttemptCount = 0
            }
             ACTION_TOGGLE_MONITORING -> {
                // Cette action est gérée par la classe Flutter et n'affecte pas le compteur ici
                // La logique d'activation/désactivation de la surveillance est gérée par le DeviceAdminReceiver
                // qui active/désactive le service selon l'état de l'administrateur de l'appareil.
                val enabled = intent.getBooleanExtra(EXTRA_ENABLED_STATE, false)
                Log.d(TAG, "Monitoring toggled to: $enabled")
                if (!enabled) {
                    // Si la surveillance est désactivée, réinitialiser le compteur
                    failedAttemptCount = 0
                }
             }
        }
        return START_NOT_STICKY // Le service ne sera pas recréé si le système le tue
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null // Ce service n'est pas lié
    }

    override fun onDestroy() {
        stopBackgroundThread()
        Log.d(TAG, "AntiTheftService destroyed.")
        super.onDestroy()
    }

    private fun startBackgroundThread() {
        backgroundThread = HandlerThread("CameraBackground").apply { start() }
        backgroundHandler = Handler(backgroundThread!!.looper)
    }

    private fun stopBackgroundThread() {
        backgroundThread?.quitSafely()
        try {
            backgroundThread?.join()
            backgroundThread = null
            backgroundHandler = null
        } catch (e: InterruptedException) {
            Log.e(TAG, "Interrupted while stopping background thread", e)
        }
    }

    private fun takePhoto() {
        if (cameraManager == null || backgroundHandler == null) {
            Log.e(TAG, "CameraManager or backgroundHandler not initialized.")
            return
        }

        try {
            for (id in cameraManager!!.cameraIdList) {
                val characteristics = cameraManager!!.getCameraCharacteristics(id)
                val facing = characteristics.get(CameraCharacteristics.LENS_FACING)
                if (facing == CameraCharacteristics.LENS_FACING_FRONT) {
                    cameraId = id
                    break
                }
            }

            if (cameraId == null) {
                Log.e(TAG, "No front camera found.")
                return
            }

            val characteristics = cameraManager!!.getCameraCharacteristics(cameraId!!)
            val map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
            if (map == null) {
                Log.e(TAG, "Cannot get StreamConfigurationMap.")
                return
            }

            // Pour une capture silencieuse, nous prenons la plus petite taille ou une taille appropriée
            val largest: Size = map.getOutputSizes(ImageFormat.JPEG)?.maxByOrNull { it.width * it.height }
                ?: Size(640, 480) // Taille par défaut

            imageReader = ImageReader.newInstance(largest.width, largest.height, ImageFormat.JPEG, 2).apply {
                setOnImageAvailableListener(onImageAvailableListener, backgroundHandler)
            }

            if (!cameraOpenCloseLock.tryAcquire(2500, TimeUnit.MILLISECONDS)) {
                throw RuntimeException("Time out waiting to lock camera opening.")
            }

            cameraManager!!.openCamera(cameraId!!, stateCallback, backgroundHandler)

        } catch (e: CameraAccessException) {
            Log.e(TAG, "CameraAccessException in takePhoto: ${e.message}")
        } catch (e: InterruptedException) {
            Log.e(TAG, "Interrupted while trying to lock camera opening.", e)
        } catch (e: Exception) {
            Log.e(TAG, "Error opening camera: ${e.message}")
        }
    }

    private val stateCallback = object : CameraDevice.StateCallback() {
        override fun onOpened(camera: CameraDevice) {
            cameraOpenCloseLock.release()
            cameraDevice = camera
            createCameraCaptureSession()
            Log.d(TAG, "Camera opened.")
        }

        override fun onDisconnected(camera: CameraDevice) {
            cameraOpenCloseLock.release()
            camera.close()
            cameraDevice = null
            Log.d(TAG, "Camera disconnected.")
        }

        override fun onError(camera: CameraDevice, error: Int) {
            cameraOpenCloseLock.release()
            camera.close()
            cameraDevice = null
            Log.e(TAG, "Camera error: $error")
            // Envoyer une notification si erreur majeure ?
        }
    }

    private fun createCameraCaptureSession() {
        try {
            val captureRequestBuilder = cameraDevice!!.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE).apply {
                addTarget(imageReader!!.surface)
                // Désactiver le flash pour une capture discrète
                set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON)
                set(CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_OFF)
                // Demander une qualité d'image élevée si possible, ou une taille adaptée
                set(CaptureRequest.JPEG_ORIENTATION, getJpegOrientation(applicationContext))
            }

            cameraDevice!!.createCaptureSession(listOf(imageReader!!.surface),
                object : CameraCaptureSession.StateCallback() {
                    override fun onConfigured(session: CameraCaptureSession) {
                        captureSession = session
                        try {
                            session.capture(captureRequestBuilder.build(), captureCallback, backgroundHandler)
                            Log.d(TAG, "Capture initiated.")
                        } catch (e: CameraAccessException) {
                            Log.e(TAG, "CameraAccessException during capture: ${e.message}")
                        }
                    }

                    override fun onConfigureFailed(session: CameraCaptureSession) {
                        Log.e(TAG, "Failed to configure camera capture session.")
                    }
                }, null
            )
        } catch (e: CameraAccessException) {
            Log.e(TAG, "CameraAccessException in createCameraCaptureSession: ${e.message}")
        }
    }

    private val captureCallback = object : CameraCaptureSession.CaptureCallback() {
        override fun onCaptureCompleted(session: CameraCaptureSession, request: CaptureRequest, result: TotalCaptureResult) {
            super.onCaptureCompleted(session, request, result)
            Log.d(TAG, "Capture completed.")
            closeCamera()
        }

        override fun onCaptureFailed(session: CameraCaptureSession, request: CaptureRequest, failure: CaptureFailure) {
            super.onCaptureFailed(session, request, failure)
            Log.e(TAG, "Capture failed: ${failure.reason}")
            closeCamera()
        }
    }

    private val onImageAvailableListener = ImageReader.OnImageAvailableListener { reader ->
        backgroundHandler?.post {
            var image: Image? = null
            try {
                image = reader.acquireLatestImage()
                if (image != null) {
                    val buffer: ByteBuffer = image.planes[0].buffer
                    val bytes = ByteArray(buffer.remaining())
                    buffer.get(bytes)

                    val path = saveImage(bytes)
                    if (path != null) {
                        Log.d(TAG, "Image saved to: $path")
                        sendEmailWithImage(path)
                    } else {
                        Log.e(TAG, "Failed to save image.")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error processing image: ${e.message}")
            } finally {
                image?.close()
            }
        }
    }

    private fun saveImage(bytes: ByteArray): String? {
        val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
        val filename = "intruder_photo_$timestamp.jpg"
        val outputDir = File(cacheDir, "anti_theft_photos") // Utiliser le cache de l'app
        if (!outputDir.exists()) {
            outputDir.mkdirs()
        }
        val file = File(outputDir, filename)
        var fos: FileOutputStream? = null
        try {
            fos = FileOutputStream(file)
            fos.write(bytes)
            return file.absolutePath
        } catch (e: IOException) {
            Log.e(TAG, "Error saving image: ${e.message}")
            return null
        } finally {
            fos?.close()
        }
    }

    private fun sendEmailWithImage(imagePath: String) {
        val email = getEmailFromPrefs(this)
        if (email.isBlank() || email == "votre_email@example.com") {
            Log.e(TAG, "Destination email not configured. Cannot send email.")
            return
        }

        // --- IMPLÉMENTATION DE L'ENVOI D'E-MAIL ---
        // Option 1: Via un service backend (RECOMMANDÉ pour la discrétion et la fiabilité)
        // L'application enverrait l'image (en base64 ou en multipart) à votre serveur,
        // et le serveur enverrait l'e-mail.

        // Exemple (pseudo-code) pour l'envoi à un backend:
        // val imageFile = File(imagePath)
        // val requestBody = RequestBody.create(MediaType.parse("image/jpeg"), imageFile)
        // val multipartBody = MultipartBody.Part.createFormData("photo", imageFile.name, requestBody)
        // val client = OkHttpClient()
        // val request = Request.Builder()
        //     .url("https://votre-backend.com/send-intruder-photo")
        //     .post(multipartBody)
        //     .addHeader("X-Auth-Token", "your_secret_token") // Pour authentifier votre application
        //     .build()
        // client.newCall(request).enqueue(object : Callback { ... })

        // Option 2: Ouvrir une application de messagerie (NON RECOMMANDÉ pour la discrétion)
        // Cela ouvrirait l'application de messagerie avec l'image attachée,
        // mais l'utilisateur devrait cliquer sur "Envoyer". Non silencieux.
        // val emailIntent = Intent(Intent.ACTION_SEND).apply {
        //     type = "image/jpeg"
        //     putExtra(Intent.EXTRA_EMAIL, arrayOf(email))
        //     putExtra(Intent.EXTRA_SUBJECT, "Alerte Anti-Vol - Tentative d'intrusion !")
        //     putExtra(Intent.EXTRA_TEXT, "Quelqu'un a tenté de déverrouiller votre téléphone et a échoué.")
        //     val fileUri = FileProvider.getUriForFile(applicationContext, "${applicationContext.packageName}.fileprovider", File(imagePath))
        //     putExtra(Intent.EXTRA_STREAM, fileUri)
        //     addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        // }
        // emailIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) // Nécessaire si appelé depuis un service
        // try {
        //     startActivity(emailIntent)
        //     Log.d(TAG, "Opened email client with image.")
        // } catch (e: Exception) {
        //     Log.e(TAG, "No email client found: ${e.message}")
        // }

        // Pour la démonstration, nous allons simuler un envoi et supprimer l'image
        // En vrai, attendez la confirmation d'envoi avant de supprimer
        Log.d(TAG, "Simulating email send to: $email with image: $imagePath")
        // Supprimez l'image après l'envoi (ou l'échec d'envoi après quelques tentatives)
        val file = File(imagePath)
        if (file.exists()) {
             // En cas de succès de l'envoi:
             // file.delete()
             // Log.d(TAG, "Image deleted after simulated send.")
        }
    }


    private fun closeCamera() {
        try {
            captureSession?.close()
            captureSession = null
            cameraDevice?.close()
            cameraDevice = null
            imageReader?.close()
            imageReader = null
        } catch (e: Exception) {
            Log.e(TAG, "Error closing camera: ${e.message}")
        } finally {
            cameraOpenCloseLock.release()
        }
    }

    // Helper pour obtenir l'orientation de la photo
    private fun getJpegOrientation(context: Context): Int {
        // C'est un sujet complexe. L'orientation des images de caméra frontale peut varier.
        // Vous devrez peut-être ajuster en fonction des caractéristiques de votre appareil.
        // Une implémentation plus robuste impliquerait de lire les EXIF ou de prendre en compte
        // l'orientation de l'appareil au moment de la capture.
        // Pour l'instant, une valeur par défaut ou une rotation simple est suffisante pour le test.
        val rotation = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
             context.display?.rotation ?: 0
        } else {
            @Suppress("DEPRECATION")
            (context.getSystemService(Context.WINDOW_SERVICE) as android.view.WindowManager).defaultDisplay.rotation
        }
        return when (rotation) {
            Surface.ROTATION_0 -> 90
            Surface.ROTATION_90 -> 0
            Surface.ROTATION_180 -> 270
            Surface.ROTATION_270 -> 180
            else -> 0
        }
    }
}
