package com.bestjoy.app.haierstartservice.ui;

import com.bestjoy.app.haierstartservice.R;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;

public class DemoModelActivity extends BaseActionbarActivity{

	private ImageView mContentView;
	private int mContentResId;
	private int mContentTitleResId;
	
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_demo);
		((ImageView) findViewById(R.id.imageview)).setImageResource(mContentResId);
		setTitle(mContentTitleResId);
	}
	@Override
	protected boolean checkIntent(Intent intent) {
		mContentResId = intent.getIntExtra("imageId", 0);
		mContentTitleResId = intent.getIntExtra("titleId", 0);
		return true;
	}
	
	public static void startActivity(Context context, int imageId, int titleId) {
		Intent intent = new Intent(context, DemoModelActivity.class);
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		intent.putExtra("imageId", imageId);
		intent.putExtra("titleId", titleId);
		context.startActivity(intent);
	}

}
