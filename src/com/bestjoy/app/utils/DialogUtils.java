package com.bestjoy.app.utils;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;

public class DialogUtils {
	
	public interface DialogCallback extends DialogInterface.OnClickListener, 
		DialogInterface.OnDismissListener, 
		DialogInterface.OnCancelListener {
		@Override
        public void onCancel(DialogInterface dialog); 
		@Override
        public void onDismiss(DialogInterface dialog);
		@Override
        public void onClick(DialogInterface dialog, int which);
		
	}
	
	public static class DialogCallbackSimpleImpl implements DialogCallback {

		@Override
        public void onCancel(DialogInterface dialog) {
	        
        }

		@Override
        public void onDismiss(DialogInterface dialog) {
	        
        }

		@Override
        public void onClick(DialogInterface dialog, int which) {
	        
        }
		
	}
	
	public static DialogCallback getDialogCallbackSimpleImpl() {
		return new DialogCallbackSimpleImpl();
	}
	/**
	 * 创建一个简单的确认对话框
	 * @param context
	 * @param message
	 * @param callback
	 */
	public static void createSimpleConfirmAlertDialog(Context context, String message, DialogCallback callback) {
		new AlertDialog.Builder(context)
		.setMessage(message)
		.setPositiveButton(android.R.string.ok, callback)
		.setOnCancelListener(callback)
		.show();
	}
	
	public static void createSimpleConfirmAlertDialog(Context context, int messageResId, DialogCallback callback) {
		new AlertDialog.Builder(context)
		.setMessage(messageResId)
		.setPositiveButton(android.R.string.ok, callback)
		.setOnCancelListener(callback)
		.show();
	}

}
