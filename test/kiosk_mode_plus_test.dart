import 'package:flutter_test/flutter_test.dart';
import 'package:kiosk_mode_plus/kiosk_mode_plus.dart';
import 'package:kiosk_mode_plus/kiosk_mode_plus_platform_interface.dart';
import 'package:kiosk_mode_plus/kiosk_mode_plus_method_channel.dart';
import 'package:plugin_platform_interface/plugin_platform_interface.dart';

class MockKioskModePlusPlatform
    with MockPlatformInterfaceMixin
    implements KioskModePlusPlatform {

  @override
  Future<String?> getPlatformVersion() => Future.value('42');
}

void main() {
  final KioskModePlusPlatform initialPlatform = KioskModePlusPlatform.instance;

  test('$MethodChannelKioskModePlus is the default instance', () {
    expect(initialPlatform, isInstanceOf<MethodChannelKioskModePlus>());
  });

  test('getPlatformVersion', () async {
    KioskModePlus kioskModePlusPlugin = KioskModePlus();
    MockKioskModePlusPlatform fakePlatform = MockKioskModePlusPlatform();
    KioskModePlusPlatform.instance = fakePlatform;

    expect(await kioskModePlusPlugin.getPlatformVersion(), '42');
  });
}
