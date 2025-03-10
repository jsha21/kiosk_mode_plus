import 'dart:async';
import 'package:flutter/services.dart';

enum KioskMode {
  enabled,
  disabled,
}

class KioskModePlus {
  static const MethodChannel _channel = MethodChannel('kiosk_mode_plus');

  /// 키오스크 모드 시작
  static Future<bool> startKioskMode() async {
    try {
      return await _channel.invokeMethod('startKioskMode') ?? false;
    } catch (e) {
      print('키오스크 모드 시작 오류: $e');
      return false;
    }
  }

  /// 키오스크 모드 종료
  static Future<bool> stopKioskMode() async {
    try {
      return await _channel.invokeMethod('stopKioskMode') ?? false;
    } catch (e) {
      print('키오스크 모드 종료 오류: $e');
      return false;
    }
  }

  /// 현재 키오스크 모드 상태 확인
  static Future<KioskMode> getKioskMode() async {
    try {
      final bool isEnabled = await _channel.invokeMethod('isInKioskMode') ?? false;
      return isEnabled ? KioskMode.enabled : KioskMode.disabled;
    } catch (e) {
      print('키오스크 모드 상태 확인 오류: $e');
      return KioskMode.disabled;
    }
  }
}

