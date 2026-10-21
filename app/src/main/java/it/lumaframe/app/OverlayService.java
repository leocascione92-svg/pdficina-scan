package it.lumaframe.app;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ServiceInfo;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.RectF;
import android.os.Build;
import android.os.IBinder;
import android.provider.Settings;
import android.view.RoundedCorner;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowManager;

public final class OverlayService extends Service {
    private static final int NOTIFICATION_ID = 214;
    private SharedPreferences prefs;
    private WindowManager windowManager;
    private Frame frame;

    @Override public void onCreate() {
        super.onCreate();
        prefs = getSharedPreferences("lumaframe", MODE_PRIVATE);
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent == null ? "STOP" : intent.getAction();
        if ("STOP".equals(action)) {
            stopSelf();
            return START_NOT_STICKY;
        }
        if (!Settings.canDrawOverlays(this)) {
            stopSelf();
            return START_NOT_STICKY;
        }
        try {
            NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            nm.createNotificationChannel(new NotificationChannel("light", "Cornice luminosa",
                    NotificationManager.IMPORTANCE_LOW));
            Intent stop = new Intent(this, OverlayService.class).setAction("STOP");
            PendingIntent off = PendingIntent.getService(this, 0, stop,
                    PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
            PendingIntent home = PendingIntent.getActivity(this, 1,
                    new Intent(this, MainActivity.class),
                    PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
            Notification notification = new Notification.Builder(this, "light")
                    .setSmallIcon(android.R.drawable.ic_menu_view)
                    .setContentTitle("LumaFrame attivo")
                    .setContentText("Cornice luminosa attiva • Tocca Spegni per fermarla")
                    .setContentIntent(home)
                    .setOngoing(true)
                    .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Spegni", off)
                    .build();

            if (Build.VERSION.SDK_INT >= 34) {
                startForeground(NOTIFICATION_ID, notification,
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
            } else {
                startForeground(NOTIFICATION_ID, notification);
            }
            if (frame == null) {
                frame = new Frame();
                int flagsOverlay = WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                        | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN;
                WindowManager.LayoutParams lp = new WindowManager.LayoutParams(
                        -1, -1, WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                        flagsOverlay, PixelFormat.TRANSLUCENT);
                // Android 12+ rejects touch-through overlays with combined opacity > 0.8.
                // Keep a single overlay under this threshold and make its core bright.
                lp.alpha = .80f;
                lp.setTitle("LumaFrame");
                if (Build.VERSION.SDK_INT >= 28) {
                    lp.layoutInDisplayCutoutMode =
                            WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS;
                }
                windowManager.addView(frame, lp);
            }
            frame.invalidate();
            setBrightness(prefs.getBoolean("boost", false));
            prefs.edit().putBoolean("running", true).apply();
        } catch (Exception e) {
            stopSelf();
        }
        return START_NOT_STICKY;
    }

    private void setBrightness(boolean boost) {
        if (!Settings.System.canWrite(this)) return;
        try {
            if (boost) {
                if (!prefs.getBoolean("saved", false)) {
                    int oldValue = Settings.System.getInt(getContentResolver(), Settings.System.SCREEN_BRIGHTNESS);
                    int oldMode = Settings.System.getInt(getContentResolver(),
                            Settings.System.SCREEN_BRIGHTNESS_MODE);
                    prefs.edit().putInt("old", oldValue).putInt("oldmode", oldMode)
                            .putBoolean("saved", true).commit();
                }
                Settings.System.putInt(getContentResolver(),
                        Settings.System.SCREEN_BRIGHTNESS_MODE,
                        Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL);
                Settings.System.putInt(getContentResolver(), Settings.System.SCREEN_BRIGHTNESS, 255);
            } else {
                restoreBrightness();
            }
        } catch (Exception ignored) {}
    }

    private void restoreBrightness() {
        if (!prefs.getBoolean("saved", false) || !Settings.System.canWrite(this)) return;
        try {
            boolean first = Settings.System.putInt(getContentResolver(),
                    Settings.System.SCREEN_BRIGHTNESS, prefs.getInt("old", 120));
            boolean second = Settings.System.putInt(getContentResolver(),
                    Settings.System.SCREEN_BRIGHTNESS_MODE,
                    prefs.getInt("oldmode", Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC));
            if (first && second) prefs.edit().putBoolean("saved", false).apply();
        } catch (Exception ignored) {}
    }

    @Override public void onDestroy() {
        if (frame != null) {
            try { windowManager.removeView(frame); } catch (Exception ignored) {}
            frame = null;
        }
        restoreBrightness();
        prefs.edit().putBoolean("running", false).apply();
        stopForeground(STOP_FOREGROUND_REMOVE);
        super.onDestroy();
    }

    @Override public IBinder onBind(Intent intent) { return null; }

    private final class Frame extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.DITHER_FLAG);
        private final Path band = new Path();
        private final RectF outside = new RectF();
        private final RectF inside = new RectF();
        private final float density = getResources().getDisplayMetrics().density;

        Frame() { super(OverlayService.this); setWillNotDraw(false); }

        private float dp(float value) { return value * density; }

        private float cornerRadius() {
            if (!prefs.getBoolean("auto_radius", true)) {
                return dp(Math.max(8, Math.min(54, prefs.getInt("radius", 25))));
            }
            if (Build.VERSION.SDK_INT >= 31) {
                WindowInsets insets = getRootWindowInsets();
                if (insets != null) {
                    int[] positions = { RoundedCorner.POSITION_TOP_LEFT,
                            RoundedCorner.POSITION_TOP_RIGHT,
                            RoundedCorner.POSITION_BOTTOM_RIGHT,
                            RoundedCorner.POSITION_BOTTOM_LEFT };
                    float max = 0;
                    for (int position : positions) {
                        RoundedCorner rounded = insets.getRoundedCorner(position);
                        if (rounded != null) max = Math.max(max, rounded.getRadius());
                    }
                    if (max > 0) return max;
                }
            }
            return dp(Math.max(8, Math.min(54, prefs.getInt("radius", 25))));
        }

        @Override protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            final float width = getWidth(), height = getHeight();
            if (width <= 0 || height <= 0) return;
            final float thick = Math.min(Math.min(width, height) * .36f,
                    dp(Math.max(18, Math.min(135, prefs.getInt("width", 70)))));
            final float radius = Math.min(Math.min(width, height) * .5f, cornerRadius());
            final int color = kelvinColor(Math.max(2700, Math.min(9000, prefs.getInt("kelvin", 5600))));
            final int intensity = Math.max(20, Math.min(100, prefs.getInt("intensity", 100)));
            final float strength = intensity / 100f;
            final float solid = thick * .74f;
            final float fade = thick - solid;
            paint.setStyle(Paint.Style.FILL);
            paint.setShader(null);

            // Main fully saturated broad band; much brighter than v1 edge gradients.
            drawRing(canvas, 0, solid, radius, color, (int)(255 * strength));
            // Soft transition towards the transparent video-call area.
            final int steps = 16;
            for (int i = 0; i < steps; i++) {
                float t0 = i / (float)steps;
                float t1 = (i + 1) / (float)steps;
                float start = solid + fade * t0;
                float end = solid + fade * t1;
                float attenuation = (float)Math.pow(1f - t0, 1.55);
                int alpha = Math.max(0, Math.min(255, Math.round(255 * strength * attenuation)));
                drawRing(canvas, start, end, radius, color, alpha);
            }
        }

        private void drawRing(Canvas c, float insetOuter, float insetInner,
                              float radius, int color, int alpha) {
            float w = getWidth(), h = getHeight();
            if (insetInner >= Math.min(w, h) / 2) return;
            outside.set(insetOuter, insetOuter, w - insetOuter, h - insetOuter);
            inside.set(insetInner, insetInner, w - insetInner, h - insetInner);
            band.reset();
            band.setFillType(Path.FillType.EVEN_ODD);
            band.addRoundRect(outside, Math.max(0, radius - insetOuter),
                    Math.max(0, radius - insetOuter), Path.Direction.CW);
            band.addRoundRect(inside, Math.max(0, radius - insetInner),
                    Math.max(0, radius - insetInner), Path.Direction.CW);
            paint.setColor(Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color)));
            c.drawPath(band, paint);
        }

        private int kelvinColor(int kelvin) {
            // Approximation to a black-body radiator; returns sRGB and no colour permissions needed.
            double x = kelvin / 100.0;
            double red = x <= 66 ? 255 : 329.698727446 * Math.pow(x - 60, -0.1332047592);
            double green = x <= 66 ? 99.4708025861 * Math.log(x) - 161.1195681661
                    : 288.1221695283 * Math.pow(x - 60, -0.0755148492);
            double blue = x >= 66 ? 255 : (x <= 19 ? 0 :
                    138.5177312231 * Math.log(x - 10) - 305.0447927307);
            return Color.rgb(clamp(red), clamp(green), clamp(blue));
        }

        private int clamp(double n) { return (int)Math.max(0, Math.min(255, Math.round(n))); }
    }
}
