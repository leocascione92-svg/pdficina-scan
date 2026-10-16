package it.pdficina.scan;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

final class Ui {
    static final int BLUE = 0xFF0078D7;
    static final int INK = 0xFF1F2937;
    static final int MUTED = 0xFF64748B;
    static final int BG = 0xFFF5F7FA;
    static final int BORDER = 0xFFDCE3EC;
    static final int WHITE = 0xFFFFFFFF;
    static int dp(Context c, float x) { return (int)(x * c.getResources().getDisplayMetrics().density + .5f); }
    static GradientDrawable bg(int fill, int stroke, int r, Context c) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(fill); d.setCornerRadius(dp(c,r));
        if(stroke!=0) d.setStroke(dp(c,1),stroke);
        return d;
    }
    static TextView text(Context c, String s, int sp, int color, boolean bold) {
        TextView t=new TextView(c); t.setText(s); t.setTextSize(TypedValue.COMPLEX_UNIT_SP,sp); t.setTextColor(color);
        if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        return t;
    }
    static Button button(Context c,String s, boolean filled){
        Button b=new Button(c); b.setText(s); b.setTextAllCaps(false); b.setTextSize(14);
        b.setTextColor(filled?WHITE:BLUE); b.setElevation(0); b.setPadding(dp(c,10),0,dp(c,10),0);
        b.setBackground(bg(filled?BLUE:WHITE,filled?0:BORDER,14,c)); b.setMinHeight(dp(c,46));
        return b;
    }
    static LinearLayout column(Context c){ LinearLayout l=new LinearLayout(c);l.setOrientation(LinearLayout.VERTICAL);return l; }
    static LinearLayout row(Context c){ LinearLayout l=new LinearLayout(c);l.setGravity(Gravity.CENTER_VERTICAL);return l; }
    static LinearLayout.LayoutParams lp(Context c,int w,int h,int margin){
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(w<0?w:dp(c,w),h<0?h:dp(c,h));
        p.setMargins(dp(c,margin),dp(c,margin),dp(c,margin),dp(c,margin));return p;
    }
    static void space(LinearLayout l,Context c,int d){View v=new View(c);l.addView(v,new LinearLayout.LayoutParams(1,dp(c,d)));}
    private Ui(){}
}
