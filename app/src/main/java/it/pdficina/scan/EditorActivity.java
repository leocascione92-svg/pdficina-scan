package it.pdficina.scan;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Build;
import android.view.WindowInsets;
import android.view.Gravity;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class EditorActivity extends Activity {
    private final ExecutorService io=Executors.newSingleThreadExecutor();
    private CropOverlayView overlay;
    private Spinner filter;
    private Button auto,save;
    private ProgressBar progress;
    private Uri original;
    private float[] savedCorners;
    private int savedFilter=0;
    private boolean working=false;
    @Override public void onCreate(Bundle state){
        super.onCreate(state);original=getIntent().getData();
        if(original==null){finish();return;}
        if(state!=null){savedCorners=state.getFloatArray("corners");savedFilter=state.getInt("filter",0);}
        LinearLayout root=Ui.column(this);root.setBackgroundColor(Ui.BG);
        root.setPadding(Ui.dp(this,12),Ui.dp(this,9),Ui.dp(this,12),Ui.dp(this,12));setContentView(root);
        if(Build.VERSION.SDK_INT>=35)root.setOnApplyWindowInsetsListener((view,insets)->{
            android.graphics.Insets safe=insets.getInsets(WindowInsets.Type.systemBars());
            root.setPadding(Ui.dp(this,12),Ui.dp(this,9)+safe.top,Ui.dp(this,12),Ui.dp(this,12)+safe.bottom);
            return insets;
        });
        LinearLayout head=Ui.row(this);
        TextView title=Ui.text(this,"Ritaglia e migliora",20,Ui.INK,true);
        head.addView(title,new LinearLayout.LayoutParams(0,-2,1));
        Button cancel=Ui.button(this,"Annulla",false);head.addView(cancel,Ui.lp(this,94,44,0));
        cancel.setOnClickListener(v->finish());root.addView(head,Ui.lp(this,-1,48,3));
        TextView hint=Ui.text(this,"Trascina i quattro punti sugli angoli del foglio.",13,Ui.MUTED,false);
        hint.setGravity(Gravity.CENTER_VERTICAL);root.addView(hint,Ui.lp(this,-1,36,2));
        overlay=new CropOverlayView(this);
        root.addView(overlay,new LinearLayout.LayoutParams(-1,0,1));
        LinearLayout tools=Ui.column(this);tools.setPadding(Ui.dp(this,12),Ui.dp(this,12),Ui.dp(this,12),Ui.dp(this,14));
        tools.setBackground(Ui.bg(Ui.WHITE,Ui.BORDER,16,this));
        LinearLayout row=Ui.row(this);
        auto=Ui.button(this,"⊞  Suggerisci bordi",false);
        row.addView(auto,new LinearLayout.LayoutParams(0,Ui.dp(this,46),1));
        auto.setOnClickListener(v->{Bitmap b=overlay.getBitmap();if(b!=null){overlay.setCorners(ImageTools.suggestBounds(b));toast("Controlla e regola i vertici rilevati");}});
        tools.addView(row);
        Ui.space(tools,this,10);
        filter=new Spinner(this);
        String[] labels={"Originale a colori","Scala di grigi","Colori migliorati","Bianco e nero"};
        ArrayAdapter<String> adapter=new ArrayAdapter<>(this,android.R.layout.simple_spinner_item,labels);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);filter.setAdapter(adapter);
        filter.setSelection(savedFilter);
        tools.addView(filter,Ui.lp(this,-1,46,0));
        Ui.space(tools,this,8);
        LinearLayout bottom=Ui.row(this);
        save=Ui.button(this,"✓  Salva scansione",true);
        bottom.addView(save,new LinearLayout.LayoutParams(0,Ui.dp(this,50),1));
        progress=new ProgressBar(this);progress.setVisibility(View.GONE);
        bottom.addView(progress,Ui.lp(this,44,44,6));
        save.setOnClickListener(v->saveImage());tools.addView(bottom);
        root.addView(tools,Ui.lp(this,-1,-2,0));
        load();
    }
    private void load(){
        working=true;save.setEnabled(false);auto.setEnabled(false);progress.setVisibility(View.VISIBLE);
        io.submit(()->{
            try{Bitmap bitmap=ImageTools.decode(this,original,2500);
                runOnUiThread(()->{
                    if(isFinishing()||isDestroyed()){bitmap.recycle();return;}
                    overlay.setBitmap(bitmap);
                    if(savedCorners!=null)overlay.setCorners(savedCorners);
                    working=false;save.setEnabled(true);auto.setEnabled(true);progress.setVisibility(View.GONE);
                });
            }catch(Exception e){runOnUiThread(()->{toast("Errore immagine: "+e.getMessage());finish();});}
        });
    }
    private void saveImage(){
        if(working||overlay.getBitmap()==null)return;
        final float[] corners=overlay.getCorners();final int mode=filter.getSelectedItemPosition();
        working=true;save.setEnabled(false);auto.setEnabled(false);progress.setVisibility(View.VISIBLE);
        io.submit(()->{
            Bitmap bitmap=null;
            try{
                bitmap=ImageTools.decode(this,original,2600);
                Bitmap processed=ImageTools.rectify(bitmap,corners,mode);
                Uri uri;
                try{uri=new ScanStorage(this).saveProcessed(processed);}finally{processed.recycle();}
                runOnUiThread(()->{if(isFinishing()||isDestroyed())return;
                    Intent result=new Intent();result.setData(uri);setResult(RESULT_OK,result);finish();});
            }catch(Exception e){runOnUiThread(()->{working=false;save.setEnabled(true);auto.setEnabled(true);progress.setVisibility(View.GONE);
                toast("Ritaglio non riuscito: "+e.getMessage());});}
            finally{if(bitmap!=null)bitmap.recycle();}
        });
    }
    @Override protected void onSaveInstanceState(Bundle out){super.onSaveInstanceState(out);
        if(overlay!=null)out.putFloatArray("corners",overlay.getCorners());
        if(filter!=null)out.putInt("filter",filter.getSelectedItemPosition());
    }
    private void toast(String m){Toast.makeText(this,m,Toast.LENGTH_LONG).show();}
    @Override protected void onDestroy(){io.shutdown();super.onDestroy();}
}
