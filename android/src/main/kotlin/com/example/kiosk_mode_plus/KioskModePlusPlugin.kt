package com.example.kiosk_mode_plus

import android.app.Activity
import android.app.ActivityManager
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
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
      /**
       * 키오스크 모드 시작
       * - 디바이스 오너인 경우 LockTaskPackages 등록
       * - 일반 모드에서도 startLockTask() 호출하여 스크린 고정
       */
      "startKioskMode" -> {
        activity?.let {
          try {
            val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
            val packageName = context.packageName
            val adminComponent = ComponentName(packageName, "com.example.kiosk_mode_plus.AdminReceiver")

            // 앱이 디바이스 오너인지 확인
            if (dpm.isDeviceOwnerApp(packageName)) {
              // 이 앱을 LockTask 허용 목록에 등록
              dpm.setLockTaskPackages(adminComponent, arrayOf(packageName))
            }
            // 스크린 고정 (키오스크 시작)
            it.startLockTask()
            result.success(true)
          } catch (e: Exception) {
            result.error(
              "KIOSK_ERROR",
              "키오스크 모드 시작 실패: ${e.message}, ${e.stackTrace.joinToString()}",
              null
            )
          }
        } ?: result.error("ACTIVITY_NULL", "Activity is null", null)
      }

      /**
       * 키오스크 모드 종료
       * - stopLockTask()로 스크린 고정 해제
       * - 디바이스 오너라면 LockTaskPackages 비우고, 필요하다면 UserRestriction 해제
       */
      "stopKioskMode" -> {
        activity?.let {
          try {
            // 1) 스크린 고정 해제
            it.stopLockTask()

            // 2) 디바이스 오너라면 LockTask 허용 목록도 해제
            val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
            val packageName = context.packageName
            val adminComponent = ComponentName(packageName, "com.example.kiosk_mode_plus.AdminReceiver")

            if (dpm.isDeviceOwnerApp(packageName)) {
              // LockTask 허용 목록 초기화
              dpm.setLockTaskPackages(adminComponent, arrayOf())

              // 필요 시 User Restriction 해제 (예: 앱 설치/제거 막았었다면)
              if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                dpm.clearUserRestriction(adminComponent, UserManager.DISALLOW_APPS_CONTROL)
                // 그 외 DISALLOW_UNINSTALL_APPS 등 제한을 추가로 해제해야 할 경우 여기서 처리
              }
            }

            result.success(true)
          } catch (e: Exception) {
            result.error("STOP_KIOSK_ERROR", "키오스크 모드 종료 실패: ${e.message}", null)
          }
        } ?: result.error("ACTIVITY_NULL", "Activity is null", null)
      }

      /**
       * 현재 LockTask(키오스크) 상태인지 확인
       */
      "isInKioskMode" -> {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        result.success(am.isInLockTaskMode)
      }

      /**
       * DeviceOwner 상태에서 LockTask 패키지를 등록하는 메서드 (옵션)
       */
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

      /**
       * 앱을 기본 런처로 설정 (홈 선택 다이얼로그 뜨게 함)
       * - forceDialog=true면 이미 기본 홈이어도 강제로 선택 다이얼로그를 띄움
       * - launcherPackage는 보통 'com.android.launcher3' 등을 의미 (기존 런처)
       */
      "setAsDefaultLauncher" -> {
        try {
          val forceDialog = call.argument<Boolean>("forceDialog") ?: false
          val launcherPackage = call.argument<String>("launcherPackage") ?: "com.android.launcher3"
          val packageManager = context.packageManager
          val currentPackageName = context.packageName

          // 디바이스 오너인 경우, 기존 런처를 숨길 수 있음
          val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
          val adminComponent = ComponentName(currentPackageName, "com.example.kiosk_mode_plus.AdminReceiver")

          if (dpm.isDeviceOwnerApp(currentPackageName)) {
            // 기존 런처 비활성화 시도
            try {
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

          // 이미 현재 앱이 기본 홈 앱이고, 강제 다이얼로그가 아니면 그냥 성공 반환
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

      /**
       * 기본 런처 설정 해제 (system launcher 복원)
       */
      "clearDefaultLauncher" -> {
        try {
          val launcherPackage = call.argument<String>("launcherPackage") ?: "com.android.launcher3"

          // 디바이스 오너인 경우 기존 런처 다시 활성화
          val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
          val packageName = context.packageName
          val adminComponent = ComponentName(packageName, "com.example.kiosk_mode_plus.AdminReceiver")

          if (dpm.isDeviceOwnerApp(packageName)) {
            // (1) 기존 런처 unhide
            try {
              dpm.setApplicationHidden(adminComponent, launcherPackage, false)
              Log.i("KioskMode", "Launcher package $launcherPackage enabled")
            } catch (e: Exception) {
              Log.e("KioskMode", "Failed to enable launcher: ${e.message}")
            }

            // (2) 이 플러터 앱의 LauncherActivity 비활성화
            try {
              val pm = context.packageManager
              val launcherComponent = ComponentName(packageName, "com.example.kiosk_mode_plus.LauncherActivity")
              pm.setComponentEnabledSetting(
                launcherComponent,
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP
              )
              Log.i("KioskMode", "Launcher activity disabled")
            } catch (e: Exception) {
              Log.e("KioskMode", "Failed to disable launcher activity: ${e.message}")
            }

            // (3) 홈 화면 우선순위 해제
            try {
              val pm = context.packageManager
              pm.clearPackagePreferredActivities(packageName)
              Log.i("KioskMode", "Cleared preferred activities")
            } catch (e: Exception) {
              Log.e("KioskMode", "Failed to clear preferred activities: ${e.message}")
            }

          } else {
            // 일반 모드라면 Settings의 홈 설정 화면으로 이동
            clearDefaultLauncherNormally(context)
          }
          result.success(true)
        } catch (e: Exception) {
          result.error("CLEAR_ERROR", "기본 런처 설정 해제 실패: ${e.message}", null)
        }
      }
    }
  }

  /**
   * 디바이스 오너가 아닌 일반 기기에서 기본 런처 해제를 유도할 때
   */
  private fun clearDefaultLauncherNormally(context: Context) {
    try {
      val intent = Intent(android.provider.Settings.ACTION_HOME_SETTINGS)
      intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      context.startActivity(intent)
    } catch (e: Exception) {
      Log.e("KioskMode", "Failed to clear default launcher normally: ${e.message}")
    }
  }

  override fun onDetachedFromEngine(@NonNull binding: FlutterPlugin.FlutterPluginBinding) {
    channel.setMethodCallHandler(null)
  }

  override fun onAttachedToActivity(binding: ActivityPluginBinding) {
    activity = binding.activity

    // 액티비티 연결 시도 시 LockTask 패키지 재설정
    try {
      val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
      val packageName = context.packageName
      val adminComponent = ComponentName(packageName, "com.example.kiosk_mode_plus.AdminReceiver")

      if (dpm.isDeviceOwnerApp(packageName)) {
        dpm.setLockTaskPackages(adminComponent, arrayOf(packageName))
      }
    } catch (e: Exception) {
      // 오류는 무시
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
