package com.bestjoy.app.haierstartservice.ui;

import java.io.File;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.support.v4.app.TaskStackBuilder;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.bestjoy.app.haierstartservice.MyApplication;
import com.bestjoy.app.haierstartservice.R;
import com.bestjoy.app.utils.DebugUtils;
import com.bestjoy.app.utils.MenuHandlerUtils;
import com.jeremyfeinstein.slidingmenu.lib.app.SlidingFragmentActivity;
import com.shwy.bestjoy.utils.ComConnectivityManager;
import com.shwy.bestjoy.utils.ImageHelper;
import com.umeng.analytics.MobclickAgent;
import com.umeng.message.PushAgent;
/**
 * 需要SlidingMenu能力，需要实现继承该类
 * @author chenkai
 *
 */
public abstract class BaseSlidingFragmentActivity extends SlidingFragmentActivity {
	private static final String TAG = "BaseSlidingFragmentActivity";

	private static final int CurrentPictureGalleryRequest = 11000;
	private static final int CurrentPictureCameraRequest = 11001;
	private int mCurrentPictureRequest;
	private Context mContext;
	
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
//		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		//abs__home
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		getSupportActionBar().setDisplayShowHomeEnabled(false);
		
		DebugUtils.logD(TAG, "onCreate()");
		if (!checkIntent(getIntent())) {
			finish();
			DebugUtils.logD(TAG, "checkIntent() failed, finish this activiy");
			return;
		}
		mContext = this;
		PushAgent.getInstance(mContext).onAppStart();
	}
	
	//add by chenkai, 20140726 增加youmeng统计时长 begin
	@Override
	public void onResume() {
		super.onResume();
		MobclickAgent.onResume(this);
	}
	
	@Override
	public void onPause() {
		super.onPause();
		MobclickAgent.onPause(this);
	}
	//add by chenkai, 20140726 增加youmeng统计时长 end
	
    protected abstract boolean checkIntent(Intent intent);
	
	public static final int DIALOG_PICTURE_CHOOSE_CONFIRM = 10002;
	//add by chenkai, 20131208, for updating check
	/**SD不可用*/
	protected static final int DIALOG_MEDIA_UNMOUNTED = 10003;
	
	public static final int DIALOG_DATA_NOT_CONNECTED = 10006;//数据连接不可用
	public static final int DIALOG_MOBILE_TYPE_CONFIRM = 10007;//
	
	
	public static final int DIALOG_PROGRESS = 10008;
	private ProgressDialog mProgressDialog;
	/**
	 * @param uri 选择的图库的图片的Uri
	 * @return
	 */
	protected void onPickFromGalleryFinish(Uri uri) {
	}
    protected void onPickFromCameraFinish() {
	}
    protected void onPickFromGalleryStart() {
	}
    protected void onPickFromCameraStart() {
	}
    protected void onMediaUnmountedConfirmClick() {
   	}
    protected void onDialgClick(int id, DialogInterface dialog, boolean ok, int witch) {
   	}
	/**
	 * pick avator from local gallery app.
	 * @return
	 */
    protected void pickFromGallery() {
    	if (!MyApplication.getInstance().hasExternalStorage()) {
			MyApplication.getInstance().showMessage(R.string.msg_no_sdcard);
			return;
		}
    	Intent intent = ImageHelper.createGalleryIntent();
    	startActivityForResult(intent, CurrentPictureGalleryRequest);
	}
	/**
	 * pick avator by camera
	 * @param savedFile
	 */
    protected void pickFromCamera(File savedFile) {
    	if (!MyApplication.getInstance().hasExternalStorage()) {
			MyApplication.getInstance().showMessage(R.string.msg_no_sdcard);
			return;
		}
		Intent intent = ImageHelper.createCaptureIntent(Uri.fromFile(savedFile));
		startActivityForResult(intent, CurrentPictureCameraRequest);
	}
    
    /**
	 * pick avator from local gallery app.
	 * @return
	 */
    protected void pickFromGallery(int questCode) {
    	if (!MyApplication.getInstance().hasExternalStorage()) {
			MyApplication.getInstance().showMessage(R.string.msg_no_sdcard);
			return;
		}
    	Intent intent = ImageHelper.createGalleryIntent();
    	startActivityForResult(intent, questCode);
	}
	/**
	 * pick avator by camera
	 * @param savedFile
	 */
    protected void pickFromCamera(File savedFile, int questCode) {
    	if (!MyApplication.getInstance().hasExternalStorage()) {
			MyApplication.getInstance().showMessage(R.string.msg_no_sdcard);
			return;
		}
		Intent intent = ImageHelper.createCaptureIntent(Uri.fromFile(savedFile));
		startActivityForResult(intent, questCode);
	}
    
    public int getCurrentPictureRequest() {
    	return mCurrentPictureRequest;
    }
    
    @Override
   	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
   		super.onActivityResult(requestCode, resultCode, data);
   		if (resultCode == Activity.RESULT_OK) {
   			if (CurrentPictureGalleryRequest == requestCode) {
   				onPickFromGalleryFinish(data.getData());
   			} else if (CurrentPictureCameraRequest == requestCode) {
   				onPickFromCameraFinish();
   				
   			}
   		}
   	}
       
       @Override
   	public Dialog onCreateDialog(int id) {
   		switch(id) {
   		case DIALOG_PICTURE_CHOOSE_CONFIRM:
   			return new AlertDialog.Builder(this)
   			.setItems(this.getResources().getStringArray(R.array.picture_op_items), new DialogInterface.OnClickListener() {
   				
   				@Override
   				public void onClick(DialogInterface dialog, int which) {
   					switch(which) {
   					case 0: //Gallery
   						mCurrentPictureRequest = CurrentPictureGalleryRequest;
   						onPickFromGalleryStart();
   						break;
   					case 1: //Camera
   						mCurrentPictureRequest = CurrentPictureCameraRequest;
   						onPickFromCameraStart();
   						break;
   					}
   					
   				}
   			})
   			.setNegativeButton(android.R.string.cancel, null)
   			.create();
   		case DIALOG_MEDIA_UNMOUNTED:
   			return new AlertDialog.Builder(this)
   			.setMessage(R.string.dialog_msg_media_unmounted)
   			.setCancelable(false)
   			.setPositiveButton(R.string.button_close, new DialogInterface.OnClickListener() {
   				
   				@Override
   				public void onClick(DialogInterface dialog, int which) {
   					onMediaUnmountedConfirmClick();
   					
   				}
   			})
   			.create();
   			 //add by chenkai, 20131201, add network check
   	      case DIALOG_DATA_NOT_CONNECTED:
   	    	  return ComConnectivityManager.getInstance().onCreateNoNetworkDialog(mContext);
   	      case DIALOG_PROGRESS:
   	    	  mProgressDialog = new ProgressDialog(this);
   	    	  mProgressDialog.setMessage(getString(R.string.msg_progressdialog_wait));
   	    	  mProgressDialog.setCancelable(false);
   	    	  return mProgressDialog;
   		}
   		return super.onCreateDialog(id);
   	}
       
       protected ProgressDialog getProgressDialog() {
    	   return mProgressDialog;
       }
       
       @Override
       public boolean onCreateOptionsMenu(Menu menu) {
    	   MenuHandlerUtils.onCreateOptionsMenu(menu);
//           SubMenu subMenu1 = menu.addSubMenu(R.string.menu_more);
//           subMenu1.add(0, R.string.menu_login, 0, R.string.menu_login);
//           subMenu1.add(0, R.string.menu_register, 0, R.string.menu_register);
//           subMenu1.add(0, R.string.menu_setting, 0, R.string.menu_setting);
//           subMenu1.add(0, R.string.menu_help, 0, R.string.menu_help);
//           subMenu1.add(0, R.string.menu_about, 0, R.string.menu_about);
//
//           MenuItem subMenu1Item = subMenu1.getItem();
//           subMenu1Item.setIcon(R.drawable.ic_menu_moreoverflow_normal_holo_light);
//           subMenu1Item.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
           return super.onCreateOptionsMenu(menu);
       }
       
       @Override
       public boolean onOptionsItemSelected(MenuItem item) {
           switch (item.getItemId()) {
           // Respond to the action bar's Up/Home button
           case android.R.id.home:
        	   Intent upIntent = NavUtils.getParentActivityIntent(this);
        	   if (upIntent == null) {
        		   // If we has configurated parent Activity in AndroidManifest.xml, we just finish current Activity.
        		   finish();
        		   return true;
        	   }
               if (NavUtils.shouldUpRecreateTask(this, upIntent)) {
                   // This activity is NOT part of this app's task, so create a new task
                   // when navigating up, with a synthesized back stack.
                   TaskStackBuilder.create(this)
                           // Add all of this activity's parents to the back stack
                           .addNextIntentWithParentStack(upIntent)
                           // Navigate up to the closest parent
                           .startActivities();
               } else {
                   // This activity is part of this app's task, so simply
                   // navigate up to the logical parent activity.
                   NavUtils.navigateUpTo(this, upIntent);
               }
               return true;
               default :
            	   return MenuHandlerUtils.onOptionsItemSelected(item, this);
           }

       }

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		boolean result = MenuHandlerUtils.onPrepareOptionsMenu(menu, this);
		return result | super.onPrepareOptionsMenu(menu);
	}
       
}
