# LumaFrame – Android video-call light

Standalone Android native application that draws a translucent, touch-through luminous frame on top of video calls. Works without root and has no networking, ad SDK, camera or microphone access.

- Adjustable frame width, intensity and warm/neutral/cool temperature.
- Optional system brightness boost (requires a separate permission).
- Notification action to stop.
- Manual activation; requires Display Over Other Apps permission.
- Android 10+; targets Android 15; intended for Galaxy S24.

## APK build
See [Actions](../../actions/workflows/android-apk.yml) and download the `LumaFrame-debug-APK` artifact from the successful run. The APK is a **debug build**, not a signed production release, and has not been tested on a physical phone.

## Notes
Overlay behavior can differ by Android version and calling app; Android may block overlays in protected screens. Restoring brightness on forced process termination is not guaranteed.
