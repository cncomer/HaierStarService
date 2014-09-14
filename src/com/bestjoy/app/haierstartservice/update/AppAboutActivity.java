package com.bestjoy.app.haierstartservice.update;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.ClipboardManager;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.actionbarsherlock.view.Menu;
import com.bestjoy.app.haierstartservice.MyApplication;
import com.bestjoy.app.haierstartservice.R;
import com.bestjoy.app.haierstartservice.database.DeviceDBHelper;
import com.bestjoy.app.haierstartservice.ui.BaseActionbarActivity;
import com.bestjoy.app.haierstartservice.ui.PreferencesActivity;
import com.bestjoy.app.utils.YouMengMessageHelper;
import com.shwy.bestjoy.utils.DebugUtils;

public class AppAboutActivity extends BaseActionbarActivity implements View.OnClickListener{

	private static final String TAG = "AppAboutActivity";
	private static final int DIALOG_RELEASENOTE = 1;
	private static final int DIALOG_INTRODUCE = 2;
	
	private ServiceAppInfo mServiceAppInfo;
	
	private TextView mVersionName, mUpdateStatus, mDbVersionName, mDeviceToken;
	private LinearLayout mButtonUpdate;
	
	private Button mBtnHelp, mBtnHome, mBtIntroduce;
	private int mCurrentVersion;
	private String mCurrentVersionCodeName; 
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (this.isFinishing()) {
			return;
		}
		setContentView(R.layout.about_app);
		
		SharedPreferences prefs = MyApplication.getInstance().mPreferManager;
		mCurrentVersion = prefs.getInt(PreferencesActivity.KEY_LATEST_VERSION, 0);
		mCurrentVersionCodeName = prefs.getString(PreferencesActivity.KEY_LATEST_VERSION_CODE_NAME, "");
		
		mServiceAppInfo = new ServiceAppInfo();
		initView();
		UpdateService.startUpdateServiceForce(mContext);
	}
	
	public void initView() {
		if (mButtonUpdate == null) {
			mVersionName = (TextView) findViewById(R.id.app_version_name);
			mDbVersionName = (TextView) findViewById(R.id.app_db_version_name);
			mDeviceToken = (TextView) findViewById(R.id.app_device_token);
			mUpdateStatus = (TextView) findViewById(R.id.desc_update);
			
			mButtonUpdate = (LinearLayout) findViewById(R.id.button_update);
			mBtIntroduce = (Button) findViewById(R.id.button_introduce);
			
			mBtnHome = (Button) findViewById(R.id.button_home);
			mBtnHelp = (Button) findViewById(R.id.button_help);
			
			mButtonUpdate.setOnClickListener(this);
			mBtIntroduce.setOnClickListener(this);
			mBtnHome.setOnClickListener(this);
			mBtnHelp.setOnClickListener(this);
			
			mBtIntroduce.setVisibility(View.GONE);
			mBtnHome.setVisibility(View.GONE);
			mBtnHelp.setVisibility(View.GONE);
		}
		mVersionName.setText(getString(R.string.format_current_sw_version, mCurrentVersionCodeName));
		mDbVersionName.setText(getString(R.string.format_current_db_version, DeviceDBHelper.getDeviceDatabaseVersion()));
		String deviceToken = YouMengMessageHelper.getInstance().getDeviceTotke();
		if (TextUtils.isEmpty(deviceToken)) {
			mDeviceToken.setText(R.string.msg_current_device_token_null);
			mDeviceToken.setOnClickListener(null);
		} else {
			mDeviceToken.setText(R.string.msg_current_device_token);
			mDeviceToken.setOnClickListener(this);
		}
		mDeviceToken.setVisibility(View.GONE);
		if (mServiceAppInfo != null && mServiceAppInfo.mVersionCode > mCurrentVersion) {
			//发现新版本
			mButtonUpdate.setEnabled(true);
			mUpdateStatus.setText(getString(R.string.format_latest_version, mServiceAppInfo.mVersionName));
		} else {
			//已经是最新的版本了
			mButtonUpdate.setEnabled(false);
			mUpdateStatus.setText(R.string.msg_app_has_latest);
		}
	}
	
	 @Override
     public boolean onCreateOptionsMenu(Menu menu) {
		 return false;
     }
	
	@Override
	public void onClick(View v) {
		switch(v.getId()) {
		case R.id.button_update:
			if (mServiceAppInfo != null) {
				startActivity(UpdateActivity.createIntent(mContext, mServiceAppInfo));
			} else {
				DebugUtils.logE(TAG, "mServiceAppInfo == null, so we ignore update click");
			}
			break;
		case R.id.button_introduce:
			showDialog(DIALOG_INTRODUCE);
			break;
		case R.id.button_home:
			break;
		case R.id.button_help:
			break;
		case R.id.app_device_token://单击复制device token导剪贴板
			ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
			cm.setText(YouMengMessageHelper.getInstance().getDeviceTotke());
			MyApplication.getInstance().showMessage(getString(R.string.format_current_device_token_copy, YouMengMessageHelper.getInstance().getDeviceTotke()));
			break;
		}
		
	}
	
	@Override
	public Dialog onCreateDialog(int id) {
		switch(id) {
		case DIALOG_INTRODUCE:
		case DIALOG_RELEASENOTE:
		}
		return super.onCreateDialog(id);
	}
	
	@Override
	protected boolean checkIntent(Intent intent) {
		return true;
	}
	
	public static Intent createIntent(Context context) {
		Intent intent = new Intent(context, AppAboutActivity.class);
		return intent;
	}

}
