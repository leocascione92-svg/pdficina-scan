package it.pdficina.scan;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.graphics.Bitmap;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/** User-owned shared storage. No private document copies are required. */
final class ScanStorage {
    private final Context context;
    ScanStorage(Context c) { context=c.getApplicationContext(); }
    static String stamp(){return new SimpleDateFormat("yyyyMMdd_HHmmss_SSS",Locale.ITALY).format(new Date());}
    Uri newOriginal() { return insertImage("Originale_"+stamp()+".jpg", "Originali", "image/jpeg"); }
    Uri newProcessed() { return insertImage("Scansione_"+stamp()+".jpg", "Scansioni", "image/jpeg"); }
    private Uri insertImage(String name, String type, String mime){
        ContentValues values=new ContentValues();
        values.put(MediaStore.MediaColumns.DISPLAY_NAME,name);
        values.put(MediaStore.MediaColumns.MIME_TYPE,mime);
        values.put(MediaStore.MediaColumns.RELATIVE_PATH,Environment.DIRECTORY_PICTURES+"/PDFicinaScan/"+type);
        values.put(MediaStore.MediaColumns.IS_PENDING,1);
        Uri u=context.getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,values);
        if(u==null)throw new IllegalStateException("Impossibile creare l'immagine nella memoria condivisa");
        return u;
    }
    Uri newPdf(){
        ContentValues values=new ContentValues();
        values.put(MediaStore.MediaColumns.DISPLAY_NAME,"Documento_"+stamp()+".pdf");
        values.put(MediaStore.MediaColumns.MIME_TYPE,"application/pdf");
        values.put(MediaStore.MediaColumns.RELATIVE_PATH,Environment.DIRECTORY_DOWNLOADS+"/PDFicinaScan");
        values.put(MediaStore.MediaColumns.IS_PENDING,1);
        Uri u=context.getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI,values);
        if(u==null)throw new IllegalStateException("Impossibile creare il PDF nella cartella Download");
        return u;
    }
    void publish(Uri u){
        ContentValues cv=new ContentValues();cv.put(MediaStore.MediaColumns.IS_PENDING,0);
        if(context.getContentResolver().update(u,cv,null,null)<=0)throw new IllegalStateException("Pubblicazione file non riuscita");
    }
    void discard(Uri u){if(u!=null)try{context.getContentResolver().delete(u,null,null);}catch(Exception ignored){}}
    Uri copyOriginal(Uri from) throws IOException {
        String mime=context.getContentResolver().getType(from);
        if(mime==null || !mime.startsWith("image/"))mime="image/jpeg";
        String ext="jpg";
        if(mime.equals("image/png"))ext="png";
        else if(mime.equals("image/webp"))ext="webp";
        else if(mime.equals("image/heic")||mime.equals("image/heif"))ext="heic";
        else if(mime.equals("image/avif"))ext="avif";
        else if(mime.equals("image/gif"))ext="gif";
        Uri to=insertImage("Originale_"+stamp()+"."+ext,"Originali",mime);
        try(InputStream in=context.getContentResolver().openInputStream(from);
            OutputStream out=context.getContentResolver().openOutputStream(to,"w")){
            if(in==null || out==null)throw new IOException("Impossibile leggere o scrivere l'immagine");
            byte[] b=new byte[65536]; int n;while((n=in.read(b))!=-1)out.write(b,0,n);
        }catch(Exception e){discard(to);throw e;}
        publish(to);return to;
    }
    Uri saveProcessed(Bitmap bitmap) throws IOException {
        Uri to=newProcessed();
        try(OutputStream out=context.getContentResolver().openOutputStream(to,"w")){
            if(out==null || !bitmap.compress(Bitmap.CompressFormat.JPEG,92,out))throw new IOException("Scrittura JPEG non riuscita");
        }catch(Exception e){discard(to);throw e;}
        publish(to);return to;
    }
}
