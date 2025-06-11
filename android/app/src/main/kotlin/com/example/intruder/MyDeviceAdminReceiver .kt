// android/app/src/main/kotlin/com/yourcompany/anti_theft_app/MyDeviceAdminReceiver.kt
package com.example.intruder

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast

class MyDeviceAdminReceiver : DeviceAdminReceiver() {

    private val TAG = "MyDeviceAdminReceiver"

    override fun onEnabled(context: Context, intent: Intent) {
        super.onEnabled(context, intent)
        Log.d(TAG, "Device Admin Enabled")
        Toast.makeText(context, "Protection Anti-Vol activée !", Toast.LENGTH_SHORT).show()
    }

    override fun onDisabled(context: Context, intent: Intent) {
        super.onDisabled(context, intent)
        Log.d(TAG, "Device Admin Disabled")
        Toast.makeText(context, "Protection Anti-Vol désactivée.", Toast.LENGTH_SHORT).show()
    }

    override fun onPasswordFailed(context: Context, intent: Intent) {
        super.onPasswordFailed(context, intent)
        Log.d(TAG, "Tentative de mot de passe échouée !")
        // Ici, nous allons démarrer notre service pour prendre la photo et l'envoyer
        val serviceIntent = Intent(context, AntiTheftService::class.java)
        serviceIntent.action = AntiTheftService.ACTION_PASSWORD_FAILED
        // Vous pouvez passer le nombre d'échecs configuré ici
        // serviceIntent.putExtra("threshold", getThresholdFromPrefs(context))
        context.startService(serviceIntent)
    }

    override fun onPasswordSucceeded(context: Context, intent: Intent) {
        super.onPasswordSucceeded(context, intent)
        Log.d(TAG, "Mot de passe réussi.")
        // Réinitialiser le compteur d'échecs si nécessaire
        val serviceIntent = Intent(context, AntiTheftService::class.java)
        serviceIntent.action = AntiTheftService.ACTION_PASSWORD_SUCCEEDED
        context.startService(serviceIntent)
    }

    override fun onLockTaskModeEntering(context: Context, intent: Intent, pkg: String) {
        super.onLockTaskModeEntering(context, intent, pkg)
        Log.d(TAG, "Entering lock task mode for package: $pkg")
    }

    override fun onLockTaskModeExiting(context: Context, intent: Intent) {
        super.onLockTaskModeExiting(context, intent)
        Log.d(TAG, "Exiting lock task mode.")
    }

    // Fonction utilitaire pour obtenir le composant administrateur de l'appareil
    companion object {
        fun getComponentName(context: Context): android.content.ComponentName {
            return android.content.ComponentName(context.applicationContext, MyDeviceAdminReceiver::class.java)
        }
    }
}
