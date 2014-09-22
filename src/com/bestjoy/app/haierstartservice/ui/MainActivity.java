package com.bestjoy.app.haierstartservice.ui;

import java.io.File;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.bestjoy.app.haierstartservice.MyApplication;
import com.bestjoy.app.haierstartservice.R;
import com.bestjoy.app.haierstartservice.account.MyAccountManager;
import com.bestjoy.app.haierstartservice.im.IMHelper;
import com.bestjoy.app.haierstartservice.service.IMService;
import com.bestjoy.app.haierstartservice.update.UpdateService;
import com.bestjoy.app.haierstartservice.view.ModuleViewUtils;
import com.bestjoy.app.utils.MenuHandlerUtils;
import com.bestjoy.app.utils.YouMengMessageHelper;
import com.shwy.bestjoy.utils.AsyncTaskUtils;
import com.shwy.bestjoy.utils.FilesUtils;

public class MainActivity extends BaseNoActionBarActivity {
	
	private Handler mHandler;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mHandler = new Handler();
		setContentView(R.layout.activity_main_20140913);
		ModuleViewUtils.getInstance().initModules(this);
		
		UpdateService.startUpdateServiceOnAppLaunch(mContext);
		YouMengMessageHelper.getInstance().startCheckDeviceTokenAsync();
	}
	
	@Override
	public void onResume() {
		super.onResume();
		invalidateOptionsMenu();
	}
	
	@Override
	public void onStop() {
		super.onStop();
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		YouMengMessageHelper.getInstance().cancelCheckDeviceTokenTask();
	}
	
	 @Override
     public boolean onCreateTitleBarOptionsMenu(Menu menu) {
		 MenuHandlerUtils.onCreateOptionsMenu(menu);
		 return true;
     }
	 
	 @Override
     public boolean onCreateOptionsMenu(Menu menu) {
		 return super.onCreateOptionsMenu(menu);
     }
	 
	 public boolean onPrepareOptionsMenu(Menu menu) {
		 MenuItem item = menu.findItem(R.string.menu_exit);
		if (item != null) {
			item.setVisible(MyAccountManager.getInstance().hasLoginned());
		}
		item = menu.findItem(R.string.menu_refresh);
		if (item != null) {
			item.setVisible(MyAccountManager.getInstance().hasLoginned());
		}
		
		item = menu.findItem(R.string.menu_setting);
		if (item != null) {
			item.setVisible(MyAccountManager.getInstance().hasLoginned());
		}
	     return super.onPrepareOptionsMenu(menu);
	 }
	 
	 @Override
	 public boolean onOptionsItemSelected(MenuItem menuItem) {
		 if (!MenuHandlerUtils.onOptionsItemSelected(menuItem, mContext)) {
			 switch(menuItem.getItemId()) {
			 case R.string.menu_exit:
				 new AlertDialog.Builder(mContext)
					.setMessage(R.string.msg_existing_system_confirm)
					.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
						
						@Override
						public void onClick(DialogInterface dialog, int which) {
							deleteAccountAsync();
						}
					})
					.setNegativeButton(android.R.string.cancel, null)
					.show();
				 return true;
			 case R.string.menu_refresh:
				 if (MyAccountManager.getInstance().hasLoginned()) {
					 //做一次登陆操作
					 //目前只删除本地的所有缓存文件
					 File dir = MyApplication.getInstance().getCachedXinghaoInternalRoot();
					 FilesUtils.deleteFile("Updating ", dir);
					 
					 dir = MyApplication.getInstance().getCachedXinghaoExternalRoot();
					 if (dir != null) {
						 FilesUtils.deleteFile("Updating ", dir);
					 }
				 }
				 break;
			 }
		 }
		 return super.onOptionsItemSelected(menuItem);
	 }
	 
	 private DeleteAccountTask mDeleteAccountTask;
	 private void deleteAccountAsync() {
		 AsyncTaskUtils.cancelTask(mDeleteAccountTask);
		 showDialog(DIALOG_PROGRESS);
		 mDeleteAccountTask = new DeleteAccountTask();
		 mDeleteAccountTask.execute();
	 }
	 private class DeleteAccountTask extends AsyncTask<Void, Void, Void> {

		@Override
		protected Void doInBackground(Void... params) {
			IMService.disconnectIMService(mContext, MyAccountManager.getInstance().getAccountObject());
			//删除所有的账户相关的即时通信信息
			IMHelper.deleteAllMessages(getContentResolver(), MyAccountManager.getInstance().getCurrentAccountId());
			MyAccountManager.getInstance().deleteDefaultAccount();
			MyAccountManager.getInstance().saveLastUsrTel("");
			return null;
		}

		@Override
		protected void onPostExecute(Void result) {
			super.onPostExecute(result);
			dismissDialog(DIALOG_PROGRESS);
			invalidateOptionsMenu();
			MyApplication.getInstance().showMessage(R.string.msg_op_successed);
		}

		@Override
		protected void onCancelled() {
			super.onCancelled();
			invalidateOptionsMenu();
			dismissDialog(DIALOG_PROGRESS);
		}
		
		
		 
	 }

	@Override
	protected boolean checkIntent(Intent intent) {
		return true;
	}
	/**
	 * 回到主界面
	 * @param context
	 */
	public static void startActivityForTop(Context context) {
		Intent intent = new Intent(context, MainActivity.class);
		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
		context.startActivity(intent);
	}
}
