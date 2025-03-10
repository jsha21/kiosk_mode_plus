# kiosk_mode_plus

A new Flutter plugin project.

## Getting Started

### 사용 방법

플러그인을 사용하는 앱에서는 다음과 같이 간단히 사용할 수 있습니다:

```dart
import 'package:kiosk_mode_plus/kiosk_mode_plus.dart';

// 키오스크 모드 시작
await KioskModePlus.startKioskMode();

// 키오스크 모드 상태 확인
final mode = await KioskModePlus.getKioskMode();
print('현재 키오스크 모드: ${mode == KioskMode.enabled ? "활성화됨" : "비활성화됨"}');

// 키오스크 모드 종료
await KioskModePlus.stopKioskMode();

```

Device Owner 설정만 ADB 명령으로 해주면 됩니다:

```bash
adb shell dpm set-device-owner com.example.your_app/com.example.kiosk_mode_plus.AdminReceiver

```

이렇게 개발된 플러그인은 필요한 모든 네이티브 설정을 포함하고 있어, 사용자는 Flutter 코드만 작성하면 됩니다.

매번 앱에서 설정할 필요는 없습니다. 이 키오스크 모드 설정을 더 효율적으로 관리하는 방법이 있습니다:

## 키오스크 모드 자동화 방법

### 1. 앱 시작 시 자동 활성화

앱의 `main.dart` 파일에서 초기화 코드를 추가하면 앱이 시작될 때마다 자동으로 키오스크 모드가 활성화됩니다:

```dart
import 'package:flutter/material.dart';
import 'package:kiosk_mode_plus/kiosk_mode_plus.dart';

void main() async {
  WidgetsFlutterBinding.ensureInitialized();

  // 앱 시작 시 키오스크 모드 자동 활성화
  await KioskModePlus.startKioskMode();

  runApp(MyApp());
}

```

### 2. 상태 관리 클래스 사용

더 체계적인 접근을 위해 상태 관리 클래스를 만들 수 있습니다:

```dart
// kiosk_manager.dart
import 'package:kiosk_mode_plus/kiosk_mode_plus.dart';

class KioskManager {
  static final KioskManager _instance = KioskManager._internal();
  factory KioskManager() => _instance;
  KioskManager._internal();

  bool _isInitialized = false;

  Future<void> initialize() async {
    if (!_isInitialized) {
      await KioskModePlus.startKioskMode();
      _isInitialized = true;
    }
  }

  Future<bool> isInKioskMode() async {
    final mode = await KioskModePlus.getKioskMode();
    return mode == KioskMode.enabled;
  }

  Future<void> exitKioskMode() async {
    await KioskModePlus.stopKioskMode();
  }
}

```

이 클래스를 앱 시작 시 초기화하면 됩니다:

```dart
void main() async {
  WidgetsFlutterBinding.ensureInitialized();
  await KioskManager().initialize();
  runApp(MyApp());
}

```

### 3. 부팅 시 자동 실행

앱이 런처로 설정되어 있다면, 기기 부팅 후 앱이 자동으로 실행되고 위 코드로 인해 키오스크 모드도 자동으로 활성화됩니다.

이렇게 하면 매번 코드를 호출할 필요 없이 앱이 시작될 때마다 자동으로 키오스크 모드가 활성화됩니다. Device Owner 설정은 한 번만 하면 됩니다.