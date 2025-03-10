package com.example.kiosk_mode_plus

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            // 마지막으로 실행한 앱 패키지 이름 가져오기
            val sharedPreferences = context.getSharedPreferences("kiosk_prefs", Context.MODE_PRIVATE)
            val packageName = sharedPreferences.getString("last_package", null)

            if (packageName != null) {
                val launchIntent = context.packageManager.getLaunchIntentForPackage(packageName)
                launchIntent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(launchIntent)
            }
        }
    }
}
