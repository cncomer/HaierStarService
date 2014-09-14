package com.bestjoy.app.haierstartservice.view;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.LinearLayout;

public class ResizeLayout extends LinearLayout{  
    private OnResizeListener mListener; 
     
    public interface OnResizeListener { 
        void onResize(int w, int h, int oldw, int oldh); 
    } 
     
    public void setOnResizeListener(OnResizeListener l) { 
        mListener = l; 
    } 
     
    public ResizeLayout(Context context, AttributeSet attrs) { 
        super(context, attrs); 
    } 
     
    @Override 
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {     
        super.onSizeChanged(w, h, oldw, oldh); 
         
        if (mListener != null) { 
            mListener.onResize(w, h, oldw, oldh); 
        } 
    } 
} 
