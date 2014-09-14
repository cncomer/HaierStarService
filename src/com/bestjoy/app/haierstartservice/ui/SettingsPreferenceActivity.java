/*
 * Copyright (C) 2011 Jake Wharton
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.bestjoy.app.haierstartservice.ui;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;

import org.apache.http.client.ClientProtocolException;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceScreen;
import android.support.v4.app.NavUtils;
import android.support.v4.app.TaskStackBuilder;

import com.actionbarsherlock.app.SherlockPreferenceActivity;
import com.actionbarsherlock.view.MenuItem;
import com.bestjoy.app.haierstartservice.HaierServiceObject;
import com.bestjoy.app.haierstartservice.HaierServiceObject.HaierResultObject;
import com.bestjoy.app.haierstartservice.MyApplication;
import com.bestjoy.app.haierstartservice.R;
import com.bestjoy.app.haierstartservice.account.AccountObject;
import com.bestjoy.app.haierstartservice.account.MyAccountManager;
import com.bestjoy.app.haierstartservice.database.HaierDBHelper;
import com.bestjoy.app.utils.DebugUtils;
import com.shwy.bestjoy.utils.AsyncTaskUtils;
import com.shwy.bestjoy.utils.ComConnectivityManager;
import com.shwy.bestjoy.utils.NetworkUtils;
import com.shwy.bestjoy.utils.SecurityUtils;

public class SettingsPreferenceActivity extends SherlockPreferenceActivity implements OnPreferenceChangeListener{

	private static final String TAG = "SettingsPreferenceActivity";
	private static final String KEY_ACCOUNT_NAME = "preference_key_account_name";
	private static final String KEY_ACCOUNT_PASSWORD = "preference_key_account_password";
	public static final int DIALOG_DATA_NOT_CONNECTED = 10006;//数据连接不可用
	public static final int DIALOG_MOBILE_TYPE_CONFIRM = 10007;//
	public static final int DIALOG_PROGRESS = 10008;
	private EditTextPreference mAccountName;
	private Preference mAccountPassword;
	
	private String mOldName, mOldPassword;
	private Context mContext;
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!MyAccountManager.getInstance().hasLoginned()) {
        	DebugUtils.logD(TAG, "finish Actvitiy due to hasLoginned() return false, you must login in firstlly.");
        	finish();
        	return;
        }
        mContext = this;
        addPreferencesFromResource(R.xml.settings_preferences);
        
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		getSupportActionBar().setDisplayShowHomeEnabled(false);
		
		mAccountName = (EditTextPreference) getPreferenceScreen().findPreference(KEY_ACCOUNT_NAME);
		mAccountPassword = (Preference) getPreferenceScreen().findPreference(KEY_ACCOUNT_PASSWORD);
		
		updateAccountName(MyAccountManager.getInstance().getAccountObject().mAccountName);
		mAccountName.setOnPreferenceChangeListener(this);
    }
    
    @Override
	public void onResume() {
		super.onResume();
		//重新获取一次账户密码，有可能之前呗改变了
		mOldPassword = MyAccountManager.getInstance().getAccountObject().mAccountPwd;
		
	}
    
    private void updateAccountName(String name) {
    	mOldName = name;
    	mAccountName.setText(name);
		mAccountName.setSummary(name);
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
        }
        return super.onOptionsItemSelected(item);

    }
    
    @Override
	public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
    	if (preference == mAccountPassword) {
    		ModifyPasswordActivity.startActivity(this, mOldPassword);
    		return true;
    	}
		return super.onPreferenceTreeClick(preferenceScreen, preference);
	}

	@Override
	public boolean onPreferenceChange(Preference preference, Object newValue) {
		if (preference == mAccountName) {
			String newName = (String) newValue;
			if (!mOldName.equals(newName.trim())) {
				//用户名发生变化了，我们需要更新
				updateAccountNameAsync(newName.trim());
			}
			return true;
		}
		return false;
	}
	
	  @Override
	   	public Dialog onCreateDialog(int id) {
	   		switch(id) {
	   			 //add by chenkai, 20131201, add network check
	   	      case DIALOG_DATA_NOT_CONNECTED:
	   	    	  return ComConnectivityManager.getInstance().onCreateNoNetworkDialog(this);
	   	      case DIALOG_PROGRESS:
	   	    	  ProgressDialog progressDialog = new ProgressDialog(this);
	   	    	  progressDialog.setMessage(getString(R.string.msg_progressdialog_wait));
	   	    	  progressDialog.setCancelable(false);
	   	    	  return progressDialog;
	   		}
	   		return super.onCreateDialog(id);
	   	}
	
	private UpdateAccountNameTask mUpdateAccountNameTask;
	private void updateAccountNameAsync(String name) {
		AsyncTaskUtils.cancelTask(mUpdateAccountNameTask);
		showDialog(DIALOG_PROGRESS);
		mUpdateAccountNameTask = new UpdateAccountNameTask(name);
		mUpdateAccountNameTask.execute();
	}
	
	/**
	 *    Url:http://115.29.231.29/Haier/UpdateUserName.ashx
			入参：
			UserName	y	要更新的名称
			key	y	Md5(cell+pwd)
			UID	y	用户ID

	 * @author chenkai
	 *
	 */
	private class UpdateAccountNameTask extends AsyncTask<Void, Void, HaierResultObject> {

		private String _name;
		public UpdateAccountNameTask(String name) {
			_name = name;
		}
		@Override
		protected HaierResultObject doInBackground(Void... params) {
			HaierResultObject haierResultObject = new HaierResultObject();
			StringBuilder sb = new StringBuilder(HaierServiceObject.SERVICE_URL);
			sb.append("UpdateUserName.ashx?");
			String cell = MyAccountManager.getInstance().getAccountObject().mAccountTel;
			String pwd = MyAccountManager.getInstance().getAccountObject().mAccountPwd;
			long uid = MyAccountManager.getInstance().getAccountObject().mAccountUid;
			sb.append("UserName=").append(URLEncoder.encode(_name))
			.append("&key=").append(SecurityUtils.MD5.md5(cell+pwd))
			.append("&UID=").append(uid);
			InputStream is = null;
			try {
				 is = NetworkUtils.openContectionLocked(sb.toString(), MyApplication.getInstance().getSecurityKeyValuesObject());
			     if (is != null) {
			    	 haierResultObject = HaierResultObject.parse(NetworkUtils.getContentFromInput(is));
			    	 if (haierResultObject.isOpSuccessfully()) {
			    		 //如果更新成功，我们需要同步更新本地数据
			    		 AccountObject accountObject = MyAccountManager.getInstance().getAccountObject();
			    		 accountObject.mAccountName = _name;
			    		 ContentValues values = new ContentValues();
			    		 values.put(HaierDBHelper.ACCOUNT_NAME, _name);
			    		 accountObject.updateAccount(mContext.getContentResolver(), values);
			    	 }
			     }
			} catch (ClientProtocolException e) {
				e.printStackTrace();
				haierResultObject.mStatusMessage = e.getMessage();
			} catch (IOException e) {
				e.printStackTrace();
				haierResultObject.mStatusMessage = e.getMessage();
			} finally {
				NetworkUtils.closeInputStream(is);
			}
			return haierResultObject;
		}

		@Override
		protected void onPostExecute(HaierResultObject result) {
			super.onPostExecute(result);
			if (result.isOpSuccessfully()) {
				MyApplication.getInstance().showMessage(R.string.msg_op_successed);
				updateAccountName(_name);
			} else {
				MyApplication.getInstance().showMessage(result.mStatusMessage);
			}
			dismissDialog(DIALOG_PROGRESS);
		}

		@Override
		protected void onCancelled() {
			super.onCancelled();
			dismissDialog(DIALOG_PROGRESS);
			MyApplication.getInstance().showMessage(R.string.msg_op_canceled);
		}
		
	}
	
	public static void startActivity(Context context) {
    	Intent intent = new Intent(context, SettingsPreferenceActivity.class);
    	context.startActivity(intent);
    }
	
}
