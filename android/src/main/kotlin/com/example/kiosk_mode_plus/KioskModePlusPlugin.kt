package com.example.kiosk_mode_plus

import android.app.Activity
import android.app.ActivityManager
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.UserManager
import android.util.Log
import androidx.annotation.NonNull
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.embedding.engine.plugins.activity.ActivityAware
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result

class KioskModePlusPlugin: FlutterPlugin, MethodCallHandler, ActivityAware {
  private lateinit var channel: MethodChannel
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
            // 정확한 패키지와 컴포넌트 경로 사용
            val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
            val packageName = context.packageName // 호스트 앱 패키지 이름
            val adminComponent = ComponentName(packageName, "com.example.kiosk_mode_plus.AdminReceiver")
            
            // 앱이 디바이스 오너인지 확인
            if (dpm.isDeviceOwnerApp(packageName)) {
              // 여기서 앱을 LockTask 화이트리스트에 추가
              dpm.setLockTaskPackages(adminComponent, arrayOf(packageName))
              
              // 키오스크 모드 시작
              it.startLockTask()
              result.success(true)
            } else {
              // 디바이스 오너가 아닌 경우 일반 스크린 피닝으로 폴백
              it.startLockTask()
              result.success(true)
            }
          } catch (e: Exception) {
            // 예외 발생 시 디버깅을 위해 자세한 오류 메시지 반환
            result.error("KIOSK_ERROR", "키오스크 모드 시작 실패: ${e.message}, ${e.stackTrace.joinToString()}", null)
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
      
      "setupLockTaskPackages" -> {
        try {
          val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
          val packageName = context.packageName
          val adminComponent = ComponentName(packageName, "com.example.kiosk_mode_plus.AdminReceiver")
          
          if (dpm.isDeviceOwnerApp(packageName)) {
            dpm.setLockTaskPackages(adminComponent, arrayOf(packageName))
            result.success(true)
          } else {
            result.error("NOT_DEVICE_OWNER", "앱이 디바이스 오너가 아닙니다", null)
          }
        } catch (e: Exception) {
          result.error("SETUP_ERROR", "LockTask 설정 실패: ${e.message}", null)
        }
      }
      
      "setAsDefaultLauncher" -> {
        try {
          val forceDialog = call.argument<Boolean>("forceDialog") ?: false
          val launcherPackage = call.argument<String>("launcherPackage") ?: "com.android.launcher3"
          val packageManager = context.packageManager
          val currentPackageName = context.packageName
          
          // 디바이스 오너 확인 및 런처 비활성화
          val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
          val adminComponent = ComponentName(currentPackageName, "com.example.kiosk_mode_plus.AdminReceiver")
          
          if (dpm.isDeviceOwnerApp(packageName)) {
            try {
              // 기존 런처 비활성화
              dpm.setApplicationHidden(adminComponent, launcherPackage, true)
              Log.i("KioskMode", "Launcher package $launcherPackage disabled")
            } catch (e: Exception) {
              Log.e("KioskMode", "Failed to disable launcher: ${e.message}")
            }
          }
          
          // 현재 기본 홈 앱 확인
          val intent = Intent(Intent.ACTION_MAIN)
          intent.addCategory(Intent.CATEGORY_HOME)
          intent.addCategory(Intent.CATEGORY_DEFAULT)
          
          val resolveInfo = packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
          val currentDefaultLauncher = resolveInfo?.activityInfo?.packageName
          
          // 이미 현재 앱이 기본 홈 앱이고 강제 다이얼로그가 아닌 경우 종료
          if (currentDefaultLauncher == currentPackageName && !forceDialog) {
            result.success(true)
            return
          }
          
          // 홈 선택 다이얼로그 표시
          val selectIntent = Intent(Intent.ACTION_MAIN)
          selectIntent.addCategory(Intent.CATEGORY_HOME)
          selectIntent.addCategory(Intent.CATEGORY_DEFAULT)
          selectIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
          
          activity?.startActivity(selectIntent) ?: context.startActivity(selectIntent)
          result.success(true)
        } catch (e: Exception) {
          result.error("SET_LAUNCHER_ERROR", "기본 런처 설정 실패: ${e.message}", null)
        }
      }
      
      "clearDefaultLauncher" -> {
        try {
          val launcherPackage = call.argument<String>("launcherPackage") ?: "com.android.launcher3"
          
          // 디바이스 오너인 경우 기존 런처 활성화
          val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
          val packageName = context.packageName
          val adminComponent = ComponentName(packageName, "com.example.kiosk_mode_plus.AdminReceiver")
          
          if (dpm.isDeviceOwnerApp(packageName)) {
            // 기존 런처 다시 활성화
            try {
              dpm.setApplicationHidden(adminComponent, launcherPackage, false)
              Log.i("KioskMode", "Launcher package $launcherPackage enabled")
            } catch (e: Exception) {
              Log.e("KioskMode", "Failed to enable launcher: ${e.message}")
            }
            
            // 추가 제한 해제
            clearDefaultLauncherAsDeviceOwner(context, dpm, adminComponent)
          } else {
            // 일반 접근 방식으로 기본 런처 설정 해제
            clearDefaultLauncher(context)
          }
          result.success(true)
        } catch (e: Exception) {
          result.error("CLEAR_ERROR", "기본 런처 설정 해제 실패: ${e.message}", null)
        }
      }
    }
  }

  private fun clearDefaultLauncherAsDeviceOwner(context: Context, dpm: DevicePolicyManager, adminComponent: ComponentName) {
    try {
      // 이전에 숨긴 런처 패키지를 다시 표시하기
      val launcherPackage = "com.android.launcher3" // 기본 런처 패키지명
      dpm.setApplicationHidden(adminComponent, launcherPackage, false)
      
      // 앱 제한 해제
      if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
        dpm.clearUserRestriction(adminComponent, UserManager.DISALLOW_APPS_CONTROL)
      }
      
      // 이 앱을 LockTask 허용 목록에서 제거
      dpm.setLockTaskPackages(adminComponent, arrayOf())
    } catch (e: Exception) {
      Log.e("KioskMode", "Failed to clear default launcher as device owner: ${e.message}")
    }
  }

  private fun clearDefaultLauncher(context: Context) {
    try {
      // 기본 런처 설정 화면으로 이동하는 인텐트 생성
      val intent = Intent(android.provider.Settings.ACTION_HOME_SETTINGS)
      intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      context.startActivity(intent)
    } catch (e: Exception) {
      Log.e("KioskMode", "Failed to clear default launcher: ${e.message}")
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
      val packageName = context.packageName
      val adminComponent = ComponentName(packageName, "com.example.kiosk_mode_plus.AdminReceiver")
      
      if (dpm.isDeviceOwnerApp(packageName)) {
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
