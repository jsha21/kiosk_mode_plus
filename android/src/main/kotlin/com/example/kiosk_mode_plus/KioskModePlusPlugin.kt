package com.example.kiosk_mode_plus

import android.app.Activity
import android.app.ActivityManager
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
          // 패키지 이름 저장
          sharedPreferences.edit().putString("last_package", it.packageName).apply()

          // 키오스크 모드 시작
          it.startLockTask()
          result.success(true)
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

