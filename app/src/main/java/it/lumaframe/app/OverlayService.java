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
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Shader;
import android.os.Build;
import android.os.IBinder;
import android.provider.Settings;
import android.view.WindowManager;
import android.view.View;

public final class OverlayService extends Service {
    private static final int ID=214;
    private SharedPreferences p;
    private WindowManager wm;
    private Frame view;

    @Override public void onCreate() {
        super.onCreate(); p=getSharedPreferences("lumaframe",MODE_PRIVATE);
        wm=(WindowManager)getSystemService(WINDOW_SERVICE);
    }
    @Override public int onStartCommand(Intent intent,int flags,int startId){
        String action=intent==null?"STOP":intent.getAction();
        if("STOP".equals(action)){stopSelf();return START_NOT_STICKY;}
        if(!Settings.canDrawOverlays(this)){stopSelf();return START_NOT_STICKY;}
        try {
            NotificationManager nm=(NotificationManager)getSystemService(NOTIFICATION_SERVICE);
            nm.createNotificationChannel(new NotificationChannel("light","Cornice luminosa",NotificationManager.IMPORTANCE_LOW));
            Intent stop=new Intent(this,OverlayService.class).setAction("STOP");
            PendingIntent end=PendingIntent.getService(this,0,stop,PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
            PendingIntent home=PendingIntent.getActivity(this,1,new Intent(this,MainActivity.class),
                    PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
            Notification n=new Notification.Builder(this,"light").setSmallIcon(android.R.drawable.ic_menu_view)
                    .setContentTitle("LumaFrame attivo").setContentText("Cornice luminosa accesa")
                    .setContentIntent(home).setOngoing(true).addAction(android.R.drawable.ic_menu_close_clear_cancel,"Spegni",end).build();
            if(Build.VERSION.SDK_INT>=34)startForeground(ID,n, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
            else startForeground(ID,n);
            if(view==null){
                view=new Frame();
                int fl=WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE|WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE|WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN;
                WindowManager.LayoutParams lp=new WindowManager.LayoutParams(-1,-1,WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,fl,PixelFormat.TRANSLUCENT);
                lp.alpha=.79f;lp.setTitle("LumaFrame");
                if(Build.VERSION.SDK_INT>=28)lp.layoutInDisplayCutoutMode=WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS;
                wm.addView(view,lp);
            }
            view.invalidate();
            brightness(p.getBoolean("boost",false));
            p.edit().putBoolean("running",true).apply();
        } catch(Exception e){stopSelf();}
        return START_NOT_STICKY;
    }
    private void brightness(boolean boost) {
        if(!Settings.System.canWrite(this))return;
        try{
            if(boost){
                if(!p.getBoolean("saved",false)){
                    int old=Settings.System.getInt(getContentResolver(),Settings.System.SCREEN_BRIGHTNESS);
                    int mode=Settings.System.getInt(getContentResolver(),Settings.System.SCREEN_BRIGHTNESS_MODE);
                    p.edit().putInt("old",old).putInt("oldmode",mode).putBoolean("saved",true).commit();
                }
                Settings.System.putInt(getContentResolver(),Settings.System.SCREEN_BRIGHTNESS_MODE,Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL);
                Settings.System.putInt(getContentResolver(),Settings.System.SCREEN_BRIGHTNESS,255);
            } else restore();
        }catch(Exception ignored){}
    }
    private void restore(){
        if(!p.getBoolean("saved",false)||!Settings.System.canWrite(this))return;
        try{
            boolean a=Settings.System.putInt(getContentResolver(),Settings.System.SCREEN_BRIGHTNESS,p.getInt("old",120));
            boolean b=Settings.System.putInt(getContentResolver(),Settings.System.SCREEN_BRIGHTNESS_MODE,
                    p.getInt("oldmode",Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC));
            if(a&&b)p.edit().putBoolean("saved",false).apply();
        }catch(Exception ignored){}
    }
    @Override public void onDestroy() {
        if(view!=null){try{wm.removeView(view);}catch(Exception ignored){}view=null;}
        restore();p.edit().putBoolean("running",false).apply();stopForeground(STOP_FOREGROUND_REMOVE);super.onDestroy();
    }
    @Override public IBinder onBind(Intent i){return null;}
    private class Frame extends View {
        Frame(){super(OverlayService.this);setWillNotDraw(false);}
        @Override protected void onDraw(Canvas c){
            super.onDraw(c);
            float w=getWidth(),h=getHeight(),d=getResources().getDisplayMetrics().density;
            float t=Math.min(Math.min(w,h)*.30f,Math.max(12,Math.min(80,p.getInt("width",43)))*d);
            int intensity=Math.max(25,Math.min(100,p.getInt("intensity",90)));
            int tone=p.getInt("tone",1);
            int rgb=tone==0?Color.rgb(215,235,255):(tone==2?Color.rgb(255,223,179):Color.WHITE);
            int a=(int)(intensity*2.55f);
            int inner=Color.argb(0,Color.red(rgb),Color.green(rgb),Color.blue(rgb));
            int outer=Color.argb(a,Color.red(rgb),Color.green(rgb),Color.blue(rgb));
            int mid=Color.argb((int)(a*.96f),Color.red(rgb),Color.green(rgb),Color.blue(rgb));
            int[] colors={outer,mid,inner};float[] stops={0,.52f,1};
            Paint pt=new Paint(Paint.ANTI_ALIAS_FLAG | Paint.DITHER_FLAG);
            pt.setShader(new LinearGradient(0,0,t,0,colors,stops,Shader.TileMode.CLAMP));c.drawRect(0,0,t,h,pt);
            pt.setShader(new LinearGradient(w,0,w-t,0,colors,stops,Shader.TileMode.CLAMP));c.drawRect(w-t,0,w,h,pt);
            pt.setShader(new LinearGradient(0,0,0,t,colors,stops,Shader.TileMode.CLAMP));c.drawRect(0,0,w,t,pt);
            pt.setShader(new LinearGradient(0,h,0,h-t,colors,stops,Shader.TileMode.CLAMP));c.drawRect(0,h-t,w,h,pt);
        }
    }
}
