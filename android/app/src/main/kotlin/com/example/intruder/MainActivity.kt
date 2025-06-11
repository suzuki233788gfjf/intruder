// android/app/src/main/kotlin/com/yourcompany/anti_theft_app/MainActivity.kt
package com.yourcompany.anti_theft_app

import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.widget.Toast
import android.net.Uri
import android.os.Build
import android.util.Log

class MainActivity: FlutterActivity() {
    private val CHANNEL = "com.yourcompany.anti_theft_app/device_admin"
    private lateinit var devicePolicyManager: DevicePolicyManager
    private lateinit var compName: ComponentName
    private val REQUEST_CODE_ENABLE_ADMIN = 1

    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)
        devicePolicyManager = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        compName = MyDeviceAdminReceiver.getComponentName(this)

        MethodChannel(flutterEngine.dartExecutor.binaryMessenger, CHANNEL).setMethodCallHandler {
            call, result ->
            when (call.method) {
                "requestDeviceAdmin" -> {
                    // Demander la permission d'administrateur de l'appareil
                    val isActive = devicePolicyManager.isAdminActive(compName)
                    if (!isActive) {
                        val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN)
                        intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, compName)
                        intent.putExtra(DevicePolicyManager.EXTRA_DPM_ADD_MSG, "Activez l'administrateur de l'appareil pour permettre à Anti-Vol de surveiller les tentatives de déverrouillage.")
                        startActivityForResult(intent, REQUEST_CODE_ENABLE_ADMIN)
                        // Le résultat sera géré dans onActivityResult
                        // On doit "mettre en attente" le résultat pour Flutter
                        // Nous utiliserons une variable globale ou un callback pour renvoyer le résultat
                        requestDeviceAdminResult = result
                    } else {
                        result.success(true) // Déjà actif
                    }
                }
                "disableDeviceAdmin" -> {
                    if (devicePolicyManager.isAdminActive(compName)) {
                        devicePolicyManager.removeActiveAdmin(compName)
                        result.success(true)
                    } else {
                        result.success(false) // Pas actif
                    }
                }
                "isDeviceAdminActive" -> {
                    result.success(devicePolicyManager.isAdminActive(compName))
                }
                "saveSettings" -> {
                    val email = call.argument<String>("email")
                    val threshold = call.argument<Int>("threshold")
                    val enabled = call.argument<Boolean>("enabled")

                    val prefs = getSharedPreferences("anti_theft_prefs", Context.MODE_PRIVATE)
                    with(prefs.edit()) {
                        putString("destination_email", email)
                        putInt("failed_attempts_threshold", threshold!!)
                        putBoolean("is_monitoring_enabled", enabled!!)
                        apply()
                    }
                    Log.d("MainActivity", "Settings saved: Email=$email, Threshold=$threshold, Enabled=$enabled")
                    result.success(true)
                }
                "loadSettings" -> {
                    val prefs = getSharedPreferences("anti_theft_prefs", Context.MODE_PRIVATE)
                    val email = prefs.getString("destination_email", "votre_email@example.com")
                    val threshold = prefs.getInt("failed_attempts_threshold", 3)
                    val enabled = prefs.getBoolean("is_monitoring_enabled", false)
                    val settings = mapOf(
                        "email" to email,
                        "threshold" to threshold,
                        "enabled" to enabled
                    )
                    result.success(settings)
                }
                else -> result.notImplemented()
            }
        }
    }

    private var requestDeviceAdminResult: MethodChannel.Result? = null

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_ENABLE_ADMIN) {
            if (devicePolicyManager.isAdminActive(compName)) {
                requestDeviceAdminResult?.success(true)
            } else {
                requestDeviceAdminResult?.success(false)
            }
            requestDeviceAdminResult = null // Réinitialiser le callback
        }
    }
}
