package com.example.kiosk_mode_plus

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED || 
            intent.action == Intent.ACTION_LOCKED_BOOT_COMPLETED) {
            
            // 마지막으로 실행한 앱 패키지 이름 가져오기
            val sharedPreferences = context.getSharedPreferences("kiosk_prefs", Context.MODE_PRIVATE)
            val packageName = sharedPreferences.getString("last_package", null) ?: context.packageName
            
            // 추가 플래그로 앱 시작
            val launchIntent = context.packageManager.getLaunchIntentForPackage(packageName)
            launchIntent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or 
                                  Intent.FLAG_ACTIVITY_CLEAR_TASK or 
                                  Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
            launchIntent?.addCategory(Intent.CATEGORY_HOME)
            launchIntent?.let {
                context.startActivity(it)
            }
        }
    }
}

