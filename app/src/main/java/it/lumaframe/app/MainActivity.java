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
import android.view.Gravity;
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
    private static final int BG = Color.rgb(16,23,38);
    private int dp(int n) { return (int)(n * getResources().getDisplayMetrics().density + .5f); }

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        prefs = getSharedPreferences("lumaframe", MODE_PRIVATE);
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);
        render();
        if (Build.VERSION.SDK_INT >= 33 &&
                checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 51);
        }
    }
    private TextView label(String txt, int sp) {
        TextView t = new TextView(this);
        t.setText(txt); t.setTextSize(sp); t.setTextColor(Color.WHITE);
        t.setPadding(0,dp(10),0,dp(8)); return t;
    }
    private void render() {
        ScrollView scroll = new ScrollView(this); scroll.setFillViewport(true); scroll.setBackgroundColor(BG);
        LinearLayout root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(24),dp(26),dp(24),dp(24));
        scroll.addView(root); setContentView(scroll);
        TextView title = label("LumaFrame", 32); title.setTypeface(null,1); root.addView(title);
        root.addView(label("Trasforma il bordo dello schermo in una luce per il viso durante la videochiamata.",15));

        status=label("",15); root.addView(status);
        power=new Button(this); root.addView(power);
        power.setOnClickListener(v -> toggle());

        root.addView(label("Spessore della cornice",18));
        root.addView(seek("width", 12, 80,43, v -> v+" dp"));
        root.addView(label("Intensità della luce",18));
        root.addView(seek("intensity",25,100,90,v->v+"%"));
        root.addView(label("Temperatura della luce",18));
        LinearLayout tones = new LinearLayout(this); tones.setOrientation(LinearLayout.HORIZONTAL);
        String[] toneNames={"Fredda","Bianca","Calda"};
        for (int i=0;i<3;i++) {
            final int which=i; Button btn=new Button(this); btn.setText(toneNames[i]);
            tones.addView(btn,new LinearLayout.LayoutParams(0,dp(56),1));
            btn.setOnClickListener(v -> { prefs.edit().putInt("tone",which).apply(); updateIfRunning(); Toast.makeText(this,"Tonalità: "+toneNames[which],Toast.LENGTH_SHORT).show(); });
        }
        root.addView(tones);

        CheckBox boost=new CheckBox(this); boost.setText("Massima luminosità dello schermo");
        boost.setTextColor(Color.WHITE); boost.setChecked(prefs.getBoolean("boost",false));
        boost.setPadding(0,dp(18),0,dp(16)); root.addView(boost);
        boost.setOnCheckedChangeListener((b,check) -> {
            prefs.edit().putBoolean("boost",check).apply();
            if(check && !Settings.System.canWrite(this)) {
                Toast.makeText(this,"Autorizza modifica impostazioni di sistema",Toast.LENGTH_LONG).show();
                startActivity(new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS,Uri.parse("package:"+getPackageName())));
            }
            updateIfRunning();
        });
        root.addView(label("Attiva LumaFrame e poi apri WhatsApp, Meet o un’altra app. Tocca la notifica per spegnere la cornice. Il centro resta trasparente e i tocchi passano alle altre app.",14));
        refresh();
    }
    private interface Formatter {String fmt(int v);}
    private View seek(String key,int min,int max,int initial,Formatter formatter) {
        LinearLayout section=new LinearLayout(this); section.setOrientation(LinearLayout.VERTICAL);
        TextView value=label("",14); section.addView(value);
        SeekBar seek=new SeekBar(this); seek.setMax(max-min);
        seek.setProgress(prefs.getInt(key,initial)-min); section.addView(seek);
        value.setText(formatter.fmt(min+seek.getProgress()));
        seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
            public void onProgressChanged(SeekBar bar,int n,boolean user){
                int v=min+n;value.setText(formatter.fmt(v));
                if(user) {prefs.edit().putInt(key,v).apply(); updateIfRunning();}
            }
            public void onStartTrackingTouch(SeekBar b){}
            public void onStopTrackingTouch(SeekBar b){}
        });
        return section;
    }
    private void refresh() {
        enabled=prefs.getBoolean("running",false);
        status.setText(enabled ? "● Cornice accesa" : "○ Cornice spenta");
        power.setText(enabled ? "SPEGNI CORNICE" : "ACCENDI CORNICE");
    }
    private void toggle() {
        if (enabled) { stopService(new Intent(this,OverlayService.class)); prefs.edit().putBoolean("running",false).apply(); refresh(); return; }
        if(!Settings.canDrawOverlays(this)){
            Toast.makeText(this,"Concedi Visualizzazione sopra le altre app",Toast.LENGTH_LONG).show();
            startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,Uri.parse("package:"+getPackageName())));
            return;
        }
        Intent it = new Intent(this,OverlayService.class).setAction("START");
        startForegroundService(it); prefs.edit().putBoolean("running",true).apply(); refresh();
    }
    private void updateIfRunning() {
        if(prefs.getBoolean("running",false))
            startService(new Intent(this,OverlayService.class).setAction("UPDATE"));
    }
    @Override protected void onResume(){super.onResume();if(status!=null)refresh();}
}
