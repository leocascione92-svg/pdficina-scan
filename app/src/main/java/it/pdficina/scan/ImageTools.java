package it.pdficina.scan;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import java.io.InputStream;
import java.io.IOException;
import java.util.Arrays;

final class ImageTools {
    private ImageTools(){}
    static Bitmap decode(Context c, Uri uri, int max) throws IOException {
        BitmapFactory.Options opts=new BitmapFactory.Options();opts.inJustDecodeBounds=true;
        try(InputStream in=c.getContentResolver().openInputStream(uri)){
            if(in==null)throw new IOException("Documento non leggibile");BitmapFactory.decodeStream(in,null,opts);
        }
        if(opts.outWidth<=0||opts.outHeight<=0)throw new IOException("Formato immagine non supportato");
        int sample=1;while(Math.max(opts.outWidth,opts.outHeight)/sample>max*1.4)sample*=2;
        opts.inSampleSize=sample;opts.inJustDecodeBounds=false;opts.inPreferredConfig=Bitmap.Config.ARGB_8888;
        Bitmap bmp;
        try(InputStream in=c.getContentResolver().openInputStream(uri)){
            if(in==null)throw new IOException("Documento non leggibile");bmp=BitmapFactory.decodeStream(in,null,opts);
        }
        if(bmp==null)throw new IOException("Immagine danneggiata");
        // Respect camera EXIF orientation before manual perspective correction.
        int degrees=0;boolean flip=false;
        try(InputStream in=c.getContentResolver().openInputStream(uri)){
            if(in!=null){ExifInterface exif=new ExifInterface(in);int o=exif.getAttributeInt(ExifInterface.TAG_ORIENTATION,1);
                if(o==ExifInterface.ORIENTATION_ROTATE_90||o==ExifInterface.ORIENTATION_TRANSPOSE)degrees=90;
                else if(o==ExifInterface.ORIENTATION_ROTATE_180||o==ExifInterface.ORIENTATION_FLIP_VERTICAL)degrees=180;
                else if(o==ExifInterface.ORIENTATION_ROTATE_270||o==ExifInterface.ORIENTATION_TRANSVERSE)degrees=270;
                flip=(o==ExifInterface.ORIENTATION_FLIP_HORIZONTAL||o==ExifInterface.ORIENTATION_FLIP_VERTICAL||o==ExifInterface.ORIENTATION_TRANSPOSE||o==ExifInterface.ORIENTATION_TRANSVERSE);
            }
        }catch(Exception ignored){}
        if(degrees!=0||flip){Matrix m=new Matrix();if(flip)m.postScale(-1,1);m.postRotate(degrees);Bitmap rotated=Bitmap.createBitmap(bmp,0,0,bmp.getWidth(),bmp.getHeight(),m,true);if(rotated!=bmp)bmp.recycle();bmp=rotated;}
        if(Math.max(bmp.getWidth(),bmp.getHeight())>max){float s=max/(float)Math.max(bmp.getWidth(),bmp.getHeight());Bitmap scaled=Bitmap.createScaledBitmap(bmp,Math.max(1,Math.round(bmp.getWidth()*s)),Math.max(1,Math.round(bmp.getHeight()*s)),true);if(scaled!=bmp)bmp.recycle();bmp=scaled;}
        return bmp;
    }
    static Bitmap rectify(Bitmap source,float[] xy,int filter){
        int sw=source.getWidth(),sh=source.getHeight();
        double[] q=new double[8];for(int i=0;i<4;i++){q[i*2]=xy[i*2]*sw;q[i*2+1]=xy[i*2+1]*sh;}
        double w=(PerspectiveMath.edge(q[0],q[1],q[2],q[3])+PerspectiveMath.edge(q[6],q[7],q[4],q[5]))*.5;
        double h=(PerspectiveMath.edge(q[0],q[1],q[6],q[7])+PerspectiveMath.edge(q[2],q[3],q[4],q[5]))*.5;
        if(w<40||h<40||!Double.isFinite(w)||!Double.isFinite(h))throw new IllegalArgumentException("Area troppo piccola: allarga il ritaglio");
        double factor=Math.min(1.,2600./Math.max(w,h));
        int dw=Math.max(1,(int)Math.round(w*factor)),dh=Math.max(1,(int)Math.round(h*factor));
        double[] matrix=PerspectiveMath.squareToQuad(q);
        int[] src=new int[sw*sh],out=new int[dw*dh];source.getPixels(src,0,sw,0,0,sw,sh);
        for(int y=0;y<dh;y++){
            double v=(y+.5)/dh;
            for(int x=0;x<dw;x++){
                double u=(x+.5)/dw;
                double den=matrix[6]*u+matrix[7]*v+1;
                double px=(matrix[0]*u+matrix[1]*v+matrix[2])/den;
                double py=(matrix[3]*u+matrix[4]*v+matrix[5])/den;
                if(px<0||py<0||px>=sw-1||py>=sh-1){out[y*dw+x]=0xFFFFFFFF;continue;}
                int ix=(int)px,iy=(int)py;double fx=px-ix,fy=py-iy;
                int a=src[iy*sw+ix],b=src[iy*sw+ix+1],c=src[(iy+1)*sw+ix],d=src[(iy+1)*sw+ix+1];
                int r=interp((a>>16)&255,(b>>16)&255,(c>>16)&255,(d>>16)&255,fx,fy);
                int g=interp((a>>8)&255,(b>>8)&255,(c>>8)&255,(d>>8)&255,fx,fy);
                int bb=interp(a&255,b&255,c&255,d&255,fx,fy);
                out[y*dw+x]=0xFF000000|(r<<16)|(g<<8)|bb;
            }
        }
        filter(out,filter);
        Bitmap result=Bitmap.createBitmap(dw,dh,Bitmap.Config.ARGB_8888);
        result.setPixels(out,0,dw,0,0,dw,dh);return result;
    }
    static int interp(int a,int b,int c,int d,double fx,double fy){return Math.max(0,Math.min(255,(int)Math.round((a*(1-fx)+b*fx)*(1-fy)+(c*(1-fx)+d*fx)*fy)));}
    static void filter(int[] p,int mode){
        if(mode==0)return;
        int[] hist=new int[256];
        for(int color:p){int l=((color>>16&255)*299+(color>>8&255)*587+(color&255)*114)/1000;hist[l]++;}
        int low=0,high=255,sum=0,total=p.length;
        for(int i=0;i<256;i++){sum+=hist[i];if(sum>=total*.02){low=i;break;}}
        sum=0;for(int i=255;i>=0;i--){sum+=hist[i];if(sum>=total*.02){high=i;break;}}
        if(high<=low+10){low=0;high=255;}
        int otsu=128;
        if(mode==3){double cum=0,all=0,back=0,best=-1;int count=0;
            for(int i=0;i<256;i++)all+=i*(double)hist[i];
            for(int i=0;i<256;i++){count+=hist[i];if(count==0)continue;int fg=total-count;if(fg<=0)break;cum+=i*(double)hist[i];double mean1=cum/count,mean2=(all-cum)/fg,score=(double)count*fg*(mean1-mean2)*(mean1-mean2);if(score>best){best=score;otsu=i;}}
        }
        for(int i=0;i<p.length;i++){
            int a=p[i],r=(a>>16)&255,g=(a>>8)&255,b=a&255;
            int lum=(r*299+g*587+b*114)/1000;
            if(mode==1){r=g=b=lum;}
            else if(mode==2){r=clamp((r-low)*255/(high-low));g=clamp((g-low)*255/(high-low));b=clamp((b-low)*255/(high-low));}
            else if(mode==3){r=g=b=lum>=otsu?255:0;}
            p[i]=0xFF000000|(r<<16)|(g<<8)|b;
        }
    }
    private static int clamp(int x){return Math.max(0,Math.min(255,x));}
    /** Conservative heuristic: detects an axis-aligned rectangle. User must confirm and adjust corners. */
    static float[] suggestBounds(Bitmap b){
        int w=b.getWidth(),h=b.getHeight();int[] p=new int[w*h];b.getPixels(p,0,w,0,0,w,h);
        float lx=findEdgeX(p,w,h,true),rx=findEdgeX(p,w,h,false);
        float ty=findEdgeY(p,w,h,true),by=findEdgeY(p,w,h,false);
        if(rx-lx<.35f||by-ty<.35f)return new float[]{.05f,.05f,.95f,.05f,.95f,.95f,.05f,.95f};
        return new float[]{lx,ty,rx,ty,rx,by,lx,by};
    }
    private static float findEdgeX(int[] p,int w,int h,boolean left){
        int first=(int)(w*(left?.035:.70)),last=(int)(w*(left?.30:.965)),step=Math.max(1,w/550);
        double best=-1;int bestX=left?(int)(w*.05):(int)(w*.95);
        for(int x=first;x<last;x+=step){double score=0;int n=0;
            for(int y=h/6;y<5*h/6;y+=Math.max(1,h/220)){score+=Math.abs(lum(p[y*w+x])-lum(p[y*w+Math.min(x+step,w-1)]));n++;}
            score/=Math.max(1,n);if(score>best){best=score;bestX=x;}}
        return bestX/(float)w;
    }
    private static float findEdgeY(int[] p,int w,int h,boolean top){
        int first=(int)(h*(top?.035:.70)),last=(int)(h*(top?.30:.965)),step=Math.max(1,h/550);
        double best=-1;int bestY=top?(int)(h*.05):(int)(h*.95);
        for(int y=first;y<last;y+=step){double score=0;int n=0;
            for(int x=w/6;x<5*w/6;x+=Math.max(1,w/220)){score+=Math.abs(lum(p[y*w+x])-lum(p[Math.min(y+step,h-1)*w+x]));n++;}
            score/=Math.max(1,n);if(score>best){best=score;bestY=y;}}
        return bestY/(float)h;
    }
    private static int lum(int c){return ((c>>16&255)*299+(c>>8&255)*587+(c&255)*114)/1000;}
}
