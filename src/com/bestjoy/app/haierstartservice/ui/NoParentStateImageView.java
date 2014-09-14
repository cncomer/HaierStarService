package com.bestjoy.app.haierstartservice.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.view.ViewGroup;
import android.widget.ImageView;

public class NoParentStateImageView extends ImageView{

	public NoParentStateImageView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}
	
	@Override
	public void setPressed(boolean pressed) {
		if (((ViewGroup)getParent()).isPressed()) {
			return;
		}
		super.setPressed(pressed);
	}
	
	

}
