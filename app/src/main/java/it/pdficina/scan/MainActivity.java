package it.pdficina.scan;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Bundle;
import android.os.Build;
import android.view.WindowInsets;
import android.provider.MediaStore;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends Activity {
    private static final int CAMERA=1201,IMPORT=1202,EDITOR=1203;
    private final ExecutorService io=Executors.newSingleThreadExecutor();
    private final ExecutorService thumbs=Executors.newSingleThreadExecutor();
    private final ArrayList<Page> pages=new ArrayList<>();
    private ScanStorage storage;
    private Uri pendingCamera;
    private int editingIndex=-1;
    private LinearLayout pageList;
    private TextView subtitle, empty;
    private Button cameraButton,importButton,pdfButton;
    private ProgressBar spinner;
    private boolean busy=false;

    static class Page {
        Uri original,edited;
        Page(Uri o,Uri e){original=o;edited=e;}
        Uri effective(){return edited==null?original:edited;}
    }
    @Override public void onCreate(Bundle state){
        super.onCreate(state);
        getWindow().setStatusBarColor(Ui.BG);
        getWindow().setNavigationBarColor(Ui.BG);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        storage=new ScanStorage(this);
        if(state!=null){String p=state.getString("pending_camera");if(p!=null)pendingCamera=Uri.parse(p);editingIndex=state.getInt("editing_index",-1);}
        restore(); buildUi(); render();
    }
    @Override protected void onSaveInstanceState(Bundle out){
        super.onSaveInstanceState(out);
        if(pendingCamera!=null)out.putString("pending_camera",pendingCamera.toString());
        out.putInt("editing_index",editingIndex);
    }
    private void buildUi(){
        LinearLayout root=Ui.column(this);root.setBackgroundColor(Ui.BG);
        root.setPadding(Ui.dp(this,16),Ui.dp(this,14),Ui.dp(this,16),Ui.dp(this,10));
        setContentView(root);
        if(Build.VERSION.SDK_INT>=35)root.setOnApplyWindowInsetsListener((view,insets)->{
            android.graphics.Insets safe=insets.getInsets(WindowInsets.Type.systemBars());
            root.setPadding(Ui.dp(this,16),Ui.dp(this,14)+safe.top,Ui.dp(this,16),Ui.dp(this,10)+safe.bottom);
            return insets;
        });

        LinearLayout header=Ui.row(this);
        LinearLayout titles=Ui.column(this);
        titles.addView(Ui.text(this,"PDFicina Scan",26,Ui.INK,true));
        titles.addView(Ui.text(this,"I documenti restano tuoi. Sempre.",12,Ui.MUTED,false));
        header.addView(titles,new LinearLayout.LayoutParams(0,-2,1));
        TextView badge=Ui.text(this,"OFFLINE",11,Ui.BLUE,true);
        badge.setPadding(Ui.dp(this,12),Ui.dp(this,8),Ui.dp(this,12),Ui.dp(this,8));
        badge.setBackground(Ui.bg(0xFFE8F3FD,0,16,this));
        header.addView(badge);
        root.addView(header,Ui.lp(this,-1,-2,5));
        Ui.space(root,this,12);

        LinearLayout actions=Ui.row(this);
        cameraButton=Ui.button(this,"＋  Fotografa",true);
        importButton=Ui.button(this,"↥  Importa",false);
        actions.addView(cameraButton,new LinearLayout.LayoutParams(0,Ui.dp(this,52),1));
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,Ui.dp(this,52),1);p.leftMargin=Ui.dp(this,10);actions.addView(importButton,p);
        cameraButton.setOnClickListener(v->takePicture());importButton.setOnClickListener(v->importPictures());
        root.addView(actions,Ui.lp(this,-1,52,0));
        Ui.space(root,this,14);

        LinearLayout card=Ui.column(this);
        card.setPadding(Ui.dp(this,16),Ui.dp(this,14),Ui.dp(this,16),Ui.dp(this,14));
        card.setBackground(Ui.bg(Ui.WHITE,Ui.BORDER,16,this));
        subtitle=Ui.text(this,"0 pagine selezionate",16,Ui.INK,true);card.addView(subtitle);
        TextView storageNote=Ui.text(this,"Originali: Immagini/PDFicinaScan  ·  PDF: Download/PDFicinaScan",12,Ui.MUTED,false);
        storageNote.setPadding(0,Ui.dp(this,7),0,0);card.addView(storageNote);
        root.addView(card,Ui.lp(this,-1,-2,0));
        Ui.space(root,this,12);

        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);
        pageList=Ui.column(this);pageList.setPadding(0,0,0,Ui.dp(this,12));
        scroll.addView(pageList);
        root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));

        LinearLayout bottom=Ui.column(this);
        bottom.setBackground(Ui.bg(Ui.WHITE,Ui.BORDER,16,this));
        bottom.setPadding(Ui.dp(this,14),Ui.dp(this,12),Ui.dp(this,14),Ui.dp(this,12));
        LinearLayout bottomRow=Ui.row(this);
        pdfButton=Ui.button(this,"Crea PDF multipagina",true);
        bottomRow.addView(pdfButton,new LinearLayout.LayoutParams(0,Ui.dp(this,50),1));
        spinner=new ProgressBar(this);spinner.setVisibility(View.GONE);
        bottomRow.addView(spinner,Ui.lp(this,42,42,6));
        pdfButton.setOnClickListener(v->exportPdf());
        bottom.addView(bottomRow);
        TextView privacy=Ui.text(this,"Nessun account, pubblicità, cloud o permesso Internet.",12,Ui.MUTED,false);
        privacy.setGravity(Gravity.CENTER);privacy.setPadding(0,Ui.dp(this,8),0,0);bottom.addView(privacy);
        root.addView(bottom,Ui.lp(this,-1,-2,2));
    }
    private void render(){
        subtitle.setText(pages.size()+ (pages.size()==1?" pagina selezionata":" pagine selezionate"));
        pageList.removeAllViews();
        if(pages.isEmpty()){
            LinearLayout hero=Ui.column(this);hero.setGravity(Gravity.CENTER);
            TextView ico=Ui.text(this,"▤",86,Ui.BLUE,true);ico.setGravity(Gravity.CENTER);
            TextView title=Ui.text(this,"Pronto per la prima scansione",18,Ui.INK,true);title.setGravity(Gravity.CENTER);
            TextView desc=Ui.text(this,"Fotografa un documento oppure importa foto già salvate. Poi regola i bordi e crea un PDF.",14,Ui.MUTED,false);
            desc.setGravity(Gravity.CENTER);desc.setPadding(Ui.dp(this,18),Ui.dp(this,8),Ui.dp(this,18),0);
            hero.addView(ico);hero.addView(title);hero.addView(desc);
            pageList.addView(hero,Ui.lp(this,-1,300,10));
        }else{
            for(int i=0;i<pages.size();i++)appendPage(i);
        }
        pdfButton.setEnabled(!busy && !pages.isEmpty());
        cameraButton.setEnabled(!busy);importButton.setEnabled(!busy);
        spinner.setVisibility(busy?View.VISIBLE:View.GONE);
    }
    private void appendPage(int i){
        Page page=pages.get(i);
        LinearLayout card=Ui.row(this);card.setPadding(Ui.dp(this,10),Ui.dp(this,10),Ui.dp(this,8),Ui.dp(this,10));
        card.setBackground(Ui.bg(Ui.WHITE,Ui.BORDER,14,this));
        ImageView preview=new ImageView(this);preview.setScaleType(ImageView.ScaleType.CENTER_CROP);
        preview.setBackground(Ui.bg(0xFFE6EEF5,0,8,this));
        card.addView(preview,Ui.lp(this,76,104,3));
        // Thumbnails are read off the UI thread; do not leak decoded full-resolution images.
        Uri previewUri=page.effective();
        thumbs.submit(()->{
            try{Bitmap thumb=ImageTools.decode(this,previewUri,220);
                runOnUiThread(()->{if(previewUri.equals(page.effective()))preview.setImageBitmap(thumb);else thumb.recycle();});
            }catch(Exception ignored){}
        });
        LinearLayout details=Ui.column(this);details.setPadding(Ui.dp(this,7),0,0,0);
        details.addView(Ui.text(this,"Pagina "+(i+1),16,Ui.INK,true));
        details.addView(Ui.text(this,page.edited==null?"Originale da ritagliare":"Scansione corretta",12,Ui.MUTED,false));
        Ui.space(details,this,8);
        LinearLayout line=Ui.row(this);
        Button edit=Ui.button(this,"Modifica",false);edit.setTextSize(12);
        line.addView(edit,new LinearLayout.LayoutParams(0,Ui.dp(this,40),1));
        edit.setOnClickListener(v->editPage(i));
        Button up=Ui.button(this,"↑",false),down=Ui.button(this,"↓",false),del=Ui.button(this,"×",false);
        up.setEnabled(i>0);down.setEnabled(i<pages.size()-1);
        line.addView(up,Ui.lp(this,40,40,2));line.addView(down,Ui.lp(this,40,40,2));line.addView(del,Ui.lp(this,40,40,2));
        up.setOnClickListener(v->move(i,-1));down.setOnClickListener(v->move(i,1));
        del.setOnClickListener(v->new AlertDialog.Builder(this).setTitle("Rimuovere la pagina?")
            .setMessage("Verrà rimossa solo da questo PDF. Le immagini salvate nel telefono NON saranno cancellate.")
            .setNegativeButton("Annulla",null).setPositiveButton("Rimuovi",(d,w)->{pages.remove(i);save();render();}).show());
        details.addView(line);
        card.addView(details,new LinearLayout.LayoutParams(0,-2,1));
        pageList.addView(card,Ui.lp(this,-1,-2,4));
    }
    private void move(int index,int offset){int to=index+offset;if(to<0||to>=pages.size())return;
        Page p=pages.remove(index);pages.add(to,p);save();render();}
    private void takePicture(){
        try{
            Uri u=storage.newOriginal();pendingCamera=u;
            Intent intent=new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            intent.putExtra(MediaStore.EXTRA_OUTPUT,u);
            intent.setClipData(ClipData.newUri(getContentResolver(),"PDFicina Scan",u));
            intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION|Intent.FLAG_GRANT_READ_URI_PERMISSION);
            if(intent.resolveActivity(getPackageManager())==null){storage.discard(u);pendingCamera=null;toast("Nessuna fotocamera disponibile");return;}
            startActivityForResult(intent,CAMERA);
        }catch(Exception e){if(pendingCamera!=null)storage.discard(pendingCamera);pendingCamera=null;error(e);}
    }
    private void importPictures(){
        Intent intent=new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);intent.setType("image/*");intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE,true);
        startActivityForResult(intent,IMPORT);
    }
    private void editPage(int index){if(busy)return;editingIndex=index;
        Intent intent=new Intent(this,EditorActivity.class);
        intent.setData(pages.get(index).original);
        startActivityForResult(intent,EDITOR);
    }
    @Override protected void onActivityResult(int req,int result,Intent data){
        super.onActivityResult(req,result,data);
        if(req==CAMERA){Uri u=pendingCamera;pendingCamera=null;
            if(u==null)return;
            if(result==RESULT_OK){try{
                try(android.content.res.AssetFileDescriptor fd=getContentResolver().openAssetFileDescriptor(u,"r")){
                    if(fd==null||fd.getLength()==0)throw new IllegalStateException("Foto non acquisita");
                }
                storage.publish(u);pages.add(new Page(u,null));save();render();editPage(pages.size()-1);}

                catch(Exception e){storage.discard(u);error(e);}}
            else{storage.discard(u);}return;
        }
        if(req==IMPORT&&result==RESULT_OK&&data!=null){
            List<Uri> incoming=new ArrayList<>();
            if(data.getClipData()!=null){for(int i=0;i<data.getClipData().getItemCount();i++)incoming.add(data.getClipData().getItemAt(i).getUri());}
            else if(data.getData()!=null)incoming.add(data.getData());
            if(incoming.isEmpty())return;
            setBusy(true);
            io.submit(()->{ArrayList<Uri> saved=new ArrayList<>();int failed=0;
                for(Uri uri:incoming)try{saved.add(storage.copyOriginal(uri));}catch(Exception ex){failed++;}
                final int lost=failed;
                runOnUiThread(()->{for(Uri u:saved)pages.add(new Page(u,null));save();setBusy(false);
                    toast(saved.size()+" foto importate"+(lost>0?", "+lost+" non importate":""));});
            });return;
        }
        if(req==EDITOR){int idx=editingIndex;editingIndex=-1;
            if(result==RESULT_OK&&data!=null&&data.getData()!=null&&idx>=0&&idx<pages.size()){
                pages.get(idx).edited=data.getData();save();render();toast("Scansione corretta salvata");
            }
        }
    }
    private void exportPdf(){
        if(busy||pages.isEmpty())return;
        final ArrayList<Uri> imgs=new ArrayList<>();for(Page p:pages)imgs.add(p.effective());
        setBusy(true);
        io.submit(()->{
            Uri output=null;
            try{
                output=storage.newPdf();PdfDocument pdf=new PdfDocument();
                try{
                    for(int i=0;i<imgs.size();i++){
                        Bitmap b=ImageTools.decode(this,imgs.get(i),2200);
                        try{
                            PdfDocument.PageInfo info=new PdfDocument.PageInfo.Builder(595,842,i+1).create();
                            PdfDocument.Page page=pdf.startPage(info);
                            Canvas canvas=page.getCanvas();canvas.drawColor(Color.WHITE);
                            float maxW=547,maxH=794;
                            float scale=Math.min(maxW/b.getWidth(),maxH/b.getHeight());
                            float width=b.getWidth()*scale,height=b.getHeight()*scale;
                            RectF dest=new RectF((595-width)/2,(842-height)/2,(595+width)/2,(842+height)/2);
                            Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);
                            canvas.drawBitmap(b,null,dest,paint);pdf.finishPage(page);
                        }finally{b.recycle();}
                    }
                    try(OutputStream out=getContentResolver().openOutputStream(output,"w")){
                        if(out==null)throw new IllegalStateException("Impossibile creare il file PDF");pdf.writeTo(out);
                    }
                }finally{pdf.close();}
                storage.publish(output);
                Uri done=output;
                runOnUiThread(()->{setBusy(false);
                    new AlertDialog.Builder(this).setTitle("PDF creato")
                        .setMessage(imgs.size()+" pagine. Salvato in Download/PDFicinaScan. Le foto originali sono conservate.")
                        .setNegativeButton("Chiudi",null)
                        .setPositiveButton("Condividi",(d,w)->share(done)).show();});
            }catch(Exception e){storage.discard(output);runOnUiThread(()->{setBusy(false);error(e);});}
        });
    }
    private void share(Uri pdf){try{Intent i=new Intent(Intent.ACTION_SEND);i.setType("application/pdf");
        i.putExtra(Intent.EXTRA_STREAM,pdf);i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(Intent.createChooser(i,"Condividi il PDF"));}catch(Exception e){error(e);}}
    private void setBusy(boolean v){busy=v;render();}
    private void save(){
        try{JSONArray arr=new JSONArray();for(Page p:pages){JSONObject obj=new JSONObject();obj.put("o",p.original.toString());
            obj.put("e",p.edited==null?JSONObject.NULL:p.edited.toString());arr.put(obj);}
            getPreferences(MODE_PRIVATE).edit().putString("session",arr.toString()).apply();
        }catch(Exception ignored){}
    }
    private void restore(){
        try{String raw=getPreferences(MODE_PRIVATE).getString("session","[]");JSONArray arr=new JSONArray(raw);
            for(int i=0;i<arr.length();i++){JSONObject o=arr.getJSONObject(i);pages.add(new Page(Uri.parse(o.getString("o")),o.isNull("e")?null:Uri.parse(o.getString("e"))));}
        }catch(Exception e){pages.clear();}
    }
    private void toast(String m){Toast.makeText(this,m,Toast.LENGTH_LONG).show();}
    private void error(Exception e){toast("Errore: "+(e.getMessage()==null?e.getClass().getSimpleName():e.getMessage()));}
    @Override protected void onDestroy(){io.shutdown();thumbs.shutdown();super.onDestroy();}
}
