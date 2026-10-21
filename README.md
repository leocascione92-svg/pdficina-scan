# LumaFrame 2.0 — Galaxy S24 video-call light

Minimal offline native Android application. Creates a non-interactive, touch-through light frame above video-call apps (WhatsApp, Meet etc.) with a translucent center.

## What's new in v2
- **More effective light:** larger solid light band with a soft inner falloff; wider range up to 135 dp.
- **Galaxy S24 rounded corners:** reads Android rounded display corner radius (Android 12+), with a manual radius fallback/override.
- **Continuous colour temperature:** 2700–9000 K, in steps of 100 K, adjustable live while running.
- Optional full screen brightness with permission; previous system brightness restored on normal shutdown.
- Keeps original simple controls, offline operation and foreground stop notification.

## Build
On the isolated `lumaframe-build` branch, the GitHub Action compiles a debug APK on every push. Open the latest successful Actions run and download `LumaFrame-debug-APK`.

**APK debug signing warning:** GitHub Actions generates a different default debug signing key on each clean runner. An APK from a newer build might fail to install over a prior LumaFrame build with `INSTALL_FAILED_UPDATE_INCOMPATIBLE`. In that case uninstall the old app before installing this v2. This deletes preferences. Future production builds should use a stable and *private* release signing key.

## Limitations
A display is not a real photographic ring light: maximum intensity depends on display hardware, the fraction dedicated to the border, system screen brightness and the Android overlay transparency limit (0.8 to preserve touch-through). Some Android apps/screens may block overlays. After a forced system kill, brightness restoration is not guaranteed.
