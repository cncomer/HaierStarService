package com.bestjoy.app.haierstartservice.ui;

import java.io.IOException;
import java.io.InputStream;

import org.apache.http.client.ClientProtocolException;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.EditText;

import com.actionbarsherlock.view.Menu;
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
import com.shwy.bestjoy.utils.Intents;
import com.shwy.bestjoy.utils.NetworkUtils;

public class ModifyPasswordActivity extends BaseActionbarActivity {

	private static final String TAG = "ModifyPasswordActivity";
	private EditText _oldInput, _newInput, _newReInput;
	private String _oldPassword;
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.setting_preference_password);
		_oldInput = (EditText) findViewById(R.id.title);
		_newInput = (EditText) findViewById(R.id.title1);
		_newReInput = (EditText) findViewById(R.id.title2);
	}
	
	@Override
	protected boolean checkIntent(Intent intent) {
		if (intent == null) {
			DebugUtils.logW(TAG, "checkIntent return false due to intent == null");
			return false;
		}
		_oldPassword = intent.getStringExtra(Intents.EXTRA_PASSWORD);
		if (TextUtils.isEmpty(_oldPassword)) {
			DebugUtils.logW(TAG, "checkIntent return false due to no Password");
			return false;
		}
		return true;
	}
	
	 @Override
     public boolean onCreateOptionsMenu(Menu menu) {
		 MenuItem item = menu.add(0, R.string.button_update, 0, R.string.button_update);
		 item.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
		 return true;
	 }
	 @Override
      public boolean onOptionsItemSelected(MenuItem item) {
         switch (item.getItemId()) {
         case R.string.button_update:
        	 //更新操作
        	 update();
        	 return true;
         }
         return super.onOptionsItemSelected(item);
	 }
	
	private void update() {
		String oldInput = _oldInput.getText().toString().trim();
		String newInput = _newInput.getText().toString().trim();
		String newReInput = _newReInput.getText().toString().trim();
		
		if (!oldInput.equals(_oldPassword)) {
			MyApplication.getInstance().showMessage(R.string.msg_input_old_password_error);
			return;
		}
		
		if (TextUtils.isEmpty(newInput)) {
			MyApplication.getInstance().showMessage(R.string.hint_input_new_password);
			return;
		}
		
		if (TextUtils.isEmpty(newReInput)) {
			MyApplication.getInstance().showMessage(R.string.hint_reinput_new_password);
			return;
		}
		
		if (!newInput.equals(newReInput)) {
			MyApplication.getInstance().showMessage(R.string.msg_input_new_password_error);
			return;
		}
		
		if (newInput.length() < 6) {
			MyApplication.getInstance().showMessage(R.string.msg_usr_pwd_too_short_tips);
			return;
		}
		
		//开始更新密码
		updateAccountPwdAsync(newInput);
	}
	
	private UpdateAccountPwdTask mUpdateAccountPwdTask;
	private void updateAccountPwdAsync(String pwd) {
		AsyncTaskUtils.cancelTask(mUpdateAccountPwdTask);
		showDialog(DIALOG_PROGRESS);
		mUpdateAccountPwdTask = new UpdateAccountPwdTask(pwd);
		mUpdateAccountPwdTask.execute();
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
	private class UpdateAccountPwdTask extends AsyncTask<Void, Void, HaierResultObject> {

		private String _password;
		public UpdateAccountPwdTask(String password) {
			_password = password;
		}
		@Override
		protected HaierResultObject doInBackground(Void... params) {
			long uid = MyAccountManager.getInstance().getAccountObject().mAccountUid;
			HaierResultObject haierResultObject = new HaierResultObject();
			StringBuilder sb = new StringBuilder(HaierServiceObject.SERVICE_URL);
			sb.append("ChangePwd.ashx?")
			.append("oldpwd=").append(_oldPassword)
			.append("&newpwd=").append(_password)
			.append("&UID=").append(uid);
			InputStream is = null;
			try {
				 is = NetworkUtils.openContectionLocked(sb.toString(), MyApplication.getInstance().getSecurityKeyValuesObject());
			     if (is != null) {
			    	 haierResultObject = HaierResultObject.parse(NetworkUtils.getContentFromInput(is));
			    	 if (haierResultObject.isOpSuccessfully()) {
			    		 //如果更新成功，我们需要同步更新本地数据
			    		 AccountObject accountObject = MyAccountManager.getInstance().getAccountObject();
			    		 accountObject.mAccountPwd = _password;
			    		 ContentValues values = new ContentValues();
			    		 values.put(HaierDBHelper.ACCOUNT_PWD, _password);
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
			dismissDialog(DIALOG_PROGRESS);
			if (result.isOpSuccessfully()) {
				MyApplication.getInstance().showMessage(R.string.msg_op_successed);
				finish();
			} else {
				MyApplication.getInstance().showMessage(result.mStatusMessage);
			}
		}

		@Override
		protected void onCancelled() {
			super.onCancelled();
			dismissDialog(DIALOG_PROGRESS);
			MyApplication.getInstance().showMessage(R.string.msg_op_canceled);
		}
		
	}
	
	public static void startActivity(Context context, String pwd) {
		Intent intent = new Intent(context, ModifyPasswordActivity.class);
		intent.putExtra(Intents.EXTRA_PASSWORD, pwd);
		context.startActivity(intent);
	}
}