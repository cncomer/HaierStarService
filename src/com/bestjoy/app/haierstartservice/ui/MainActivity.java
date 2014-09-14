package com.bestjoy.app.haierstartservice.ui;

import java.io.File;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.bestjoy.app.haierstartservice.MyApplication;
import com.bestjoy.app.haierstartservice.R;
import com.bestjoy.app.haierstartservice.account.MyAccountManager;
import com.bestjoy.app.haierstartservice.service.IMService;
import com.bestjoy.app.haierstartservice.update.UpdateService;
import com.bestjoy.app.haierstartservice.view.ModuleView;
import com.bestjoy.app.haierstartservice.view.ModuleViewUtils;
import com.bestjoy.app.utils.BitmapUtils;
import com.bestjoy.app.utils.YouMengMessageHelper;
import com.shwy.bestjoy.utils.AsyncTaskUtils;
import com.shwy.bestjoy.utils.FilesUtils;

public class MainActivity extends BaseActionbarActivity {
	
	private Handler mHandler;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mHandler = new Handler();
		getSupportActionBar().setDisplayShowHomeEnabled(false);
		getSupportActionBar().setDisplayShowTitleEnabled(true);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
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
     public boolean onCreateOptionsMenu(Menu menu) {
  	     boolean result = super.onCreateOptionsMenu(menu);
  	     MenuItem subMenu1Item = menu.findItem(R.string.menu_more);
  	     subMenu1Item.getSubMenu().add(1000, R.string.menu_refresh, 1005, R.string.menu_refresh);
  	     subMenu1Item.getSubMenu().add(1000, R.string.menu_exit, 1006, R.string.menu_exit);
  	     subMenu1Item.setIcon(R.drawable.ic_menu_moreoverflow);
         return result;
     }
	 
	 public boolean onPrepareOptionsMenu(Menu menu) {
		 menu.findItem(R.string.menu_exit).setVisible(MyAccountManager.getInstance().hasLoginned());
		 menu.findItem(R.string.menu_refresh).setVisible(MyAccountManager.getInstance().hasLoginned());
	     return super.onPrepareOptionsMenu(menu);
	 }
	 
	 @Override
	 public boolean onOptionsItemSelected(MenuItem menuItem) {
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
