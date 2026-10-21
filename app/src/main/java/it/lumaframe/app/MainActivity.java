package it.lumaframe.app;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

public final class MainActivity extends Activity {
    private SharedPreferences prefs;
    private TextView status;
    private Button power;
    private boolean enabled;
    private static final int BG = Color.rgb(16, 23, 38);
    private int dp(int n) { return (int)(n * getResources().getDisplayMetrics().density + .5f); }

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        prefs = getSharedPreferences("lumaframe", MODE_PRIVATE);
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);
        if (!prefs.contains("kelvin")) {
            int tone = prefs.getInt("tone", 1);
            prefs.edit().putInt("kelvin", tone == 0 ? 7800 : tone == 2 ? 3200 : 5600).apply();
        }
        render();
        if (Build.VERSION.SDK_INT >= 33
                && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 51);
        }
    }

    private TextView label(String txt, int sp) {
        TextView t = new TextView(this);
        t.setText(txt);
        t.setTextSize(sp);
        t.setTextColor(Color.WHITE);
        t.setPadding(0, dp(10), 0, dp(8));
        return t;
    }

    private void render() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(BG);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(24), dp(24), dp(24), dp(24));
        scroll.addView(root);
        setContentView(scroll);

        TextView title = label("LumaFrame", 32);
        title.setTypeface(null, 1);
        root.addView(title);
        root.addView(label("Luce per videochiamate • Versione 2.0", 15));

        status = label("", 15);
        root.addView(status);
        power = new Button(this);
        root.addView(power);
        power.setOnClickListener(v -> toggle());

        root.addView(label("Spessore della cornice", 18));
        root.addView(seek("width", 18, 135, 70, 1, v -> v + " dp"));
        root.addView(label("Intensità della luce", 18));
        root.addView(seek("intensity", 20, 100, 100, 1, v -> v + "%"));

        root.addView(label("Temperatura della luce", 18));
        root.addView(seek("kelvin", 2700, 9000, 5600, 100, v -> {
            String desc = v < 4200 ? "calda" : (v < 6500 ? "naturale" : "fredda");
            return v + " K  •  " + desc;
        }));
        root.addView(label("2700 K: giallo caldo    •    5600 K: bianco naturale    •    9000 K: freddo", 12));

        root.addView(label("Bordi arrotondati", 18));
        CheckBox auto = new CheckBox(this);
        auto.setText("Adatta automaticamente gli angoli dello schermo");
        auto.setTextColor(Color.WHITE);
        auto.setChecked(prefs.getBoolean("auto_radius", true));
        root.addView(auto);

        View radiusControl = seek("radius", 8, 54, 25, 1, v -> "Raggio manuale: " + v + " dp");
        radiusControl.setAlpha(auto.isChecked() ? .48f : 1f);
        radiusControl.setEnabled(!auto.isChecked());
        root.addView(radiusControl);
        auto.setOnCheckedChangeListener((btn, checked) -> {
            prefs.edit().putBoolean("auto_radius", checked).apply();
            radiusControl.setAlpha(checked ? .48f : 1f);
            setEnabledRecursively(radiusControl, !checked);
            updateIfRunning();
        });
        setEnabledRecursively(radiusControl, !auto.isChecked());

        CheckBox boost = new CheckBox(this);
        boost.setText("Massima luminosità dello schermo");
        boost.setTextColor(Color.WHITE);
        boost.setChecked(prefs.getBoolean("boost", false));
        boost.setPadding(0, dp(14), 0, dp(5));
        root.addView(boost);
        boost.setOnCheckedChangeListener((b, checked) -> {
            prefs.edit().putBoolean("boost", checked).apply();
            if (checked && !Settings.System.canWrite(this)) {
                Toast.makeText(this, "Autorizza la modifica delle impostazioni di sistema", Toast.LENGTH_LONG).show();
                startActivity(new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS,
                        Uri.parse("package:" + getPackageName())));
            }
            updateIfRunning();
        });

        root.addView(label("Per ottenere più luce, aumenta lo spessore e abilita la luminosità massima. "
                + "La potenza resta limitata dalla luminosità fisica del display.", 13));
        root.addView(label("Accendi la cornice, poi apri WhatsApp o Meet. La luce resta ai bordi "
                + "e puoi spegnerla dalla notifica. Non servono root, rete o fotocamera.", 13));
        refresh();
    }

    private void setEnabledRecursively(View v, boolean enabled) {
        v.setEnabled(enabled);
        if (v instanceof android.view.ViewGroup) {
            android.view.ViewGroup vg = (android.view.ViewGroup) v;
            for (int i = 0; i < vg.getChildCount(); i++) setEnabledRecursively(vg.getChildAt(i), enabled);
        }
    }

    private interface Formatter { String fmt(int v); }

    private View seek(String key, int min, int max, int initial, int step, Formatter formatter) {
        LinearLayout section = new LinearLayout(this);
        section.setOrientation(LinearLayout.VERTICAL);
        TextView value = label("", 14);
        section.addView(value);
        SeekBar seek = new SeekBar(this);
        seek.setMax((max - min) / step);
        int stored = prefs.getInt(key, initial);
        int progress = Math.max(0, Math.min(seek.getMax(), Math.round((stored - min) / (float) step)));
        seek.setProgress(progress);
        section.addView(seek);
        value.setText(formatter.fmt(min + progress * step));
        seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar bar, int n, boolean user) {
                int val = min + n * step;
                value.setText(formatter.fmt(val));
                if (user) {
                    prefs.edit().putInt(key, val).apply();
                    updateIfRunning();
                }
            }
            public void onStartTrackingTouch(SeekBar b) {}
            public void onStopTrackingTouch(SeekBar b) {}
        });
        return section;
    }

    private void refresh() {
        if (power == null || status == null) return;
        enabled = prefs.getBoolean("running", false);
        status.setText(enabled ? "● Cornice accesa" : "○ Cornice spenta");
        power.setText(enabled ? "SPEGNI CORNICE" : "ACCENDI CORNICE");
    }

    private void toggle() {
        if (enabled) {
            stopService(new Intent(this, OverlayService.class));
            prefs.edit().putBoolean("running", false).apply();
            refresh();
            return;
        }
        if (!Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "Concedi Visualizzazione sopra le altre app", Toast.LENGTH_LONG).show();
            startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName())));
            return;
        }
        try {
            startForegroundService(new Intent(this, OverlayService.class).setAction("START"));
            prefs.edit().putBoolean("running", true).apply();
        } catch (RuntimeException e) {
            Toast.makeText(this, "Impossibile accendere la cornice: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
        refresh();
    }

    private void updateIfRunning() {
        if (prefs.getBoolean("running", false)) {
            try { startService(new Intent(this, OverlayService.class).setAction("UPDATE")); }
            catch (RuntimeException ignored) {}
        }
    }

    @Override protected void onResume() { super.onResume(); refresh(); }
}
