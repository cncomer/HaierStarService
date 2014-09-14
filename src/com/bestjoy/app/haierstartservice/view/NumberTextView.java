package com.bestjoy.app.haierstartservice.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Path;
import android.util.AttributeSet;
import android.widget.TextView;

public class NumberTextView extends TextView{

	public NumberTextView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}
	
	@Override
	public void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		drawUnreadNum(canvas);
	}
	
	private void drawUnreadNum(Canvas canvas) {
		Path path = new Path();
		path.reset();
		path.moveTo(60, 330);// 开始坐标 也就是三角形的顶点
		// path.lineTo(60, 330);
		path.lineTo(0, 390);
		path.lineTo(120, 390);
		path.close();
	}

}
