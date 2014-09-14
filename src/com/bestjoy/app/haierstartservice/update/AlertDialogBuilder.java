package com.bestjoy.app.haierstartservice.update;

import android.app.AlertDialog;
import android.content.Context;
import android.view.View;

public class AlertDialogBuilder extends AlertDialog.Builder{
	
	public View mCustomView;

	public AlertDialogBuilder(Context context) {
		super(context);
	}
	
	@Override
	public AlertDialogBuilder setView(View view) {
		mCustomView = view;
		return (AlertDialogBuilder) super.setView(view);
	}
	
	

}
