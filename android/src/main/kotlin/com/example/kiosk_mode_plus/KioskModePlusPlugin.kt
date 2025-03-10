package com.example.kiosk_mode_plus

import android.app.Activity
import android.app.ActivityManager
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.SharedPreferences
import androidx.annotation.NonNull
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.embedding.engine.plugins.activity.ActivityAware
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result

class KioskModePlusPlugin: FlutterPlugin, MethodCallHandler, ActivityAware {
  private lateinit var channel : MethodChannel
  private lateinit var context: Context
  private var activity: Activity? = null
  private lateinit var sharedPreferences: SharedPreferences

  override fun onAttachedToEngine(@NonNull flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
    channel = MethodChannel(flutterPluginBinding.binaryMessenger, "kiosk_mode_plus")
    channel.setMethodCallHandler(this)
    context = flutterPluginBinding.applicationContext
    sharedPreferences = context.getSharedPreferences("kiosk_prefs", Context.MODE_PRIVATE)
  }

  override fun onMethodCall(@NonNull call: MethodCall, @NonNull result: Result) {
    when (call.method) {
      "startKioskMode" -> {
        activity?.let {
          try {
            // 패키지 이름 저장
            sharedPreferences.edit().putString("last_package", it.packageName).apply()
            
            // DevicePolicyManager 가져오기
            val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
            val adminComponent = ComponentName(context, AdminReceiver::class.java)
            
            // 관리자 권한 확인
            if (dpm.isDeviceOwnerApp(context.packageName)) {
              // LockTask 허용 패키지 설정
              dpm.setLockTaskPackages(adminComponent, arrayOf(it.packageName))
              
              // 키오스크 모드 시작
              it.startLockTask()
              result.success(true)
            } else {
              // 디바이스 오너가 아닌 경우 일반 LockTask 시도
              it.startLockTask()
              result.success(true)
            }
          } catch (e: Exception) {
            result.error("KIOSK_ERROR", "키오스크 모드 시작 실패: ${e.message}", null)
          }
        } ?: result.error("ACTIVITY_NULL", "Activity is null", null)
      }
      "stopKioskMode" -> {
        activity?.let {
          it.stopLockTask()
          result.success(true)
        } ?: result.error("ACTIVITY_NULL", "Activity is null", null)
      }
      "isInKioskMode" -> {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        result.success(am.isInLockTaskMode)
      }
      "setLockTaskPackages" -> {
        try {
          val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
          val adminComponent = ComponentName(context, AdminReceiver::class.java)
          val packageName = activity?.packageName ?: context.packageName
          
          if (dpm.isDeviceOwnerApp(context.packageName)) {
            dpm.setLockTaskPackages(adminComponent, arrayOf(packageName))
            result.success(true)
          } else {
            result.error("NOT_DEVICE_OWNER", "앱이 디바이스 오너가 아닙니다", null)
          }
        } catch (e: Exception) {
          result.error("LOCK_TASK_ERROR", "LockTask 패키지 설정 실패: ${e.message}", null)
        }
      }
      else -> {
        result.notImplemented()
      }
    }
  }

  override fun onDetachedFromEngine(@NonNull binding: FlutterPlugin.FlutterPluginBinding) {
    channel.setMethodCallHandler(null)
  }

  override fun onAttachedToActivity(binding: ActivityPluginBinding) {
    activity = binding.activity
    // 액티비티 연결 시 LockTask 패키지 설정 시도
    try {
      val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
      val adminComponent = ComponentName(context, AdminReceiver::class.java)
      val packageName = binding.activity.packageName
      
      if (dpm.isDeviceOwnerApp(context.packageName)) {
        dpm.setLockTaskPackages(adminComponent, arrayOf(packageName))
      }
    } catch (e: Exception) {
      // 오류 무시
    }
  }

  override fun onDetachedFromActivityForConfigChanges() {
    activity = null
  }

  override fun onReattachedToActivityForConfigChanges(binding: ActivityPluginBinding) {
    activity = binding.activity
  }

  override fun onDetachedFromActivity() {
    activity = null
  }
}
