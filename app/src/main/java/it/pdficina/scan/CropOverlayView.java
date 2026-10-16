package it.pdficina.scan;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.view.MotionEvent;
import android.view.View;

/** Four draggable points in normalized bitmap coordinates; original picture is never changed. */
public final class CropOverlayView extends View {
    private Bitmap bitmap;
    private final Paint imagePaint=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);
    private final Paint border=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint handle=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect=new RectF();
    private final float[] points={.05f,.05f,.95f,.05f,.95f,.95f,.05f,.95f};
    private int active=-1;
    public CropOverlayView(Context c){super(c);setLayerType(View.LAYER_TYPE_SOFTWARE,null);
        border.setStyle(Paint.Style.STROKE);border.setStrokeWidth(Ui.dp(c,2.4f));border.setColor(0xFF3EA6FF);
        handle.setStyle(Paint.Style.FILL);handle.setColor(0xFFFFFFFF);
    }
    public void setBitmap(Bitmap b){bitmap=b;invalidate();}
    public Bitmap getBitmap(){return bitmap;}
    public float[] getCorners(){return points.clone();}
    public void setCorners(float[] p){if(p==null||p.length!=8)return;System.arraycopy(p,0,points,0,8);invalidate();}
    private void measureImage(){
        if(bitmap==null||getWidth()==0||getHeight()==0){rect.setEmpty();return;}
        float pad=Ui.dp(getContext(),12);float ww=getWidth()-2*pad,hh=getHeight()-2*pad;
        float s=Math.min(ww/bitmap.getWidth(),hh/bitmap.getHeight());
        float w=bitmap.getWidth()*s,h=bitmap.getHeight()*s;
        rect.set((getWidth()-w)/2,(getHeight()-h)/2,(getWidth()+w)/2,(getHeight()+h)/2);
    }
    private float tx(float v){return rect.left+v*rect.width();}
    private float ty(float v){return rect.top+v*rect.height();}
    @Override protected void onDraw(Canvas canvas){
        super.onDraw(canvas);canvas.drawColor(0xFF182532);measureImage();if(bitmap==null)return;
        canvas.drawBitmap(bitmap,null,rect,imagePaint);
        Path path=new Path();path.moveTo(tx(points[0]),ty(points[1]));
        for(int i=1;i<4;i++)path.lineTo(tx(points[2*i]),ty(points[2*i+1]));path.close();
        Paint dim=new Paint(Paint.ANTI_ALIAS_FLAG);dim.setColor(0x330078D7);dim.setStyle(Paint.Style.FILL);canvas.drawPath(path,dim);
        canvas.drawPath(path,border);
        Paint shadow=new Paint(Paint.ANTI_ALIAS_FLAG);shadow.setStyle(Paint.Style.FILL);shadow.setColor(0xAA0078D7);
        for(int i=0;i<4;i++){
            float x=tx(points[i*2]),y=ty(points[i*2+1]);
            canvas.drawCircle(x,y,Ui.dp(getContext(),12),shadow);
            canvas.drawCircle(x,y,Ui.dp(getContext(),7),handle);
        }
    }
    @Override public boolean onTouchEvent(MotionEvent event){
        if(bitmap==null||rect.isEmpty())return false;
        switch(event.getActionMasked()){
            case MotionEvent.ACTION_DOWN:
                float min=Float.MAX_VALUE;int best=-1;
                for(int i=0;i<4;i++){
                    float dx=event.getX()-tx(points[2*i]),dy=event.getY()-ty(points[2*i+1]);
                    float distance=(float)Math.hypot(dx,dy);
                    if(distance<min){min=distance;best=i;}
                }
                if(min>Ui.dp(getContext(),48))return false;
                active=best;getParent().requestDisallowInterceptTouchEvent(true);return true;
            case MotionEvent.ACTION_MOVE:
                if(active<0)return false;
                points[2*active]=Math.max(0,Math.min(1,(event.getX()-rect.left)/rect.width()));
                points[2*active+1]=Math.max(0,Math.min(1,(event.getY()-rect.top)/rect.height()));
                invalidate();return true;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                active=-1;getParent().requestDisallowInterceptTouchEvent(false);return true;
        }
        return true;
    }
}
