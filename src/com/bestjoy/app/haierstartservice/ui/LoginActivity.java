package com.bestjoy.app.haierstartservice.ui;

import java.io.IOException;
import java.io.InputStream;

import org.apache.http.client.ClientProtocolException;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.actionbarsherlock.view.Menu;
import com.bestjoy.app.haierstartservice.HaierServiceObject;
import com.bestjoy.app.haierstartservice.HaierServiceObject.HaierResultObject;
import com.bestjoy.app.haierstartservice.MyApplication;
import com.bestjoy.app.haierstartservice.R;
import com.bestjoy.app.haierstartservice.account.AccountObject;
import com.bestjoy.app.haierstartservice.account.MyAccountManager;
import com.bestjoy.app.haierstartservice.view.ModuleViewUtils;
import com.bestjoy.app.utils.DebugUtils;
import com.bestjoy.app.utils.DialogUtils;
import com.shwy.bestjoy.utils.AsyncTaskUtils;
import com.shwy.bestjoy.utils.ComConnectivityManager;
import com.shwy.bestjoy.utils.Intents;
import com.shwy.bestjoy.utils.NetworkUtils;
import com.shwy.bestjoy.utils.SecurityUtils;

public class LoginActivity extends BaseActionbarActivity implements View.OnClickListener{
	private static final String TAG = "NewCardActivity";

	private TextView mRegisterButton, mFindPwdBuuton;
	private static final int REQUEST_LOGIN = 1;
	private Button mLoginBtn;
	private EditText mTelInput, mPasswordInput;
	public static AccountObject mAccountObject;
	/**进入界面请求,如新建我的保修卡进来的*/
	private int mModelId;
	private Bundle mBundles;

	@Override
	protected boolean checkIntent(Intent intent) {
		return true;
	}
	
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		DebugUtils.logD(TAG, "onCreate()");
		if (isFinishing()) {
			return ;
		}
		mBundles = getIntent().getExtras();
		mModelId = ModuleViewUtils.getModelIdFromBundle(mBundles);
		setContentView(R.layout.activity_login_20140415);
		initViews();
	}
	
	public void onResume() {
		super.onResume();
		//每次进来我们都要先清空一下mAccountObject，这个值作为静态变量在各个Activity中传递
		mAccountObject = null;
	}
	
	
	private void initViews() {
		mRegisterButton = (TextView) findViewById(R.id.button_register);
		mRegisterButton.setOnClickListener(this);
		
		mFindPwdBuuton = (TextView) findViewById(R.id.button_find_password);
		mFindPwdBuuton.setOnClickListener(this);
		
		mLoginBtn = (Button) findViewById(R.id.button_login);
		mLoginBtn.setOnClickListener(this);
		
		mTelInput = (EditText) findViewById(R.id.tel);
		//显示上一次输入的用户号码
		mTelInput.setText(MyAccountManager.getInstance().getLastUsrTel());
		Bundle bundle = getIntent().getExtras();
		if (bundle != null) {
			String tel = bundle.getString(Intents.EXTRA_TEL);
			if (!TextUtils.isEmpty(tel)) {
				mTelInput.setText(tel);
			}
			
		}
		mPasswordInput = (EditText) findViewById(R.id.pwd);
	}
	
	 public boolean onCreateOptionsMenu(Menu menu) {
		 return false;
	 }

	@Override
	public void onClick(View v) {
		switch(v.getId()) {
			case R.id.button_register:
				RegisterActivity.startIntent(this, getIntent().getExtras());
				break;
			case R.id.button_find_password:
				//如果电话号码为空，提示用户先输入号码，在找回密码
				if (TextUtils.isEmpty(mTelInput.getText().toString())) {
					MyApplication.getInstance().showMessage(R.string.msg_input_tel_when_find_password);
				} else {
					if (!ComConnectivityManager.getInstance().isConnected()) {
						ComConnectivityManager.getInstance().onCreateNoNetworkDialog(mContext).show();
					} else {
						findPasswordAsync();
					}
				}
				break;
			case R.id.button_login:
				//modify by chenkai, 2014.06.04，去掉号码之间的空白符号 begin
				//String tel = mTelInput.getText().toString().trim();
				String tel = mTelInput.getText().toString().trim().replaceAll("[- +]", "");
				//modify by chenkai, 2014.06.04，去掉号码之间的空白符号 end
				String pwd = mPasswordInput.getText().toString().trim();
				if (!TextUtils.isEmpty(tel) && !TextUtils.isEmpty(pwd)) {
					MyAccountManager.getInstance().saveLastUsrTel(tel);
					startActivityForResult(LoginOrUpdateAccountDialog.createLoginOrUpdate(this, true, tel, pwd), REQUEST_LOGIN);
				} else {
					MyApplication.getInstance().showMessage(R.string.msg_input_usrtel_password);
				}
				break;
		}
		
	}
	
	
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == REQUEST_LOGIN) {
			if (resultCode == Activity.RESULT_OK) {
				// login successfully
				MyApplication.getInstance().showMessage(R.string.msg_login_confirm_success);
				switch(mModelId) {
//				case R.id.model_my_card:
//				case R.id.model_install:
//				case R.id.model_repair:
////					MyChooseDevicesActivity.startIntent(mContext, ModleSettings.createMyCardDefaultBundle(mContext));
//					mBundles.putBoolean(Intents.EXTRA_HAS_REGISTERED, true);
//					NewCardActivity.startIntentClearTop(mContext, mBundles);
//					finish();
//					break;
					default : //其他情况我们回到主界面，海尔要求
						MainActivity.startActivityForTop(mContext);
						finish();
						break;
				}
			}
		} else {
			super.onActivityResult(requestCode, resultCode, data);
		}
	}
	
	private FidnPasswordTask mFidnPasswordTask;
	private void findPasswordAsync() {
		AsyncTaskUtils.cancelTask(mFidnPasswordTask);
		showDialog(DIALOG_PROGRESS);
		mFidnPasswordTask = new FidnPasswordTask();
		mFidnPasswordTask.execute();
	}
	private class FidnPasswordTask extends AsyncTask<Void, Void, HaierResultObject> {

		@Override
		protected HaierResultObject doInBackground(Void... params) {
			HaierResultObject result = new HaierResultObject();
			
			InputStream is = null;
			try {
				JSONObject queryJsonObject = new JSONObject();
				queryJsonObject.put("cell", mTelInput.getText().toString().trim());
				DebugUtils.logD(TAG, "FidnPasswordTask run--queryJsonObject " + queryJsonObject.toString(4));
				String desQuery = SecurityUtils.DES.enCrypto(queryJsonObject.toString().getBytes(), HaierServiceObject.DES_PASSWORD);
				DebugUtils.logD(TAG, "FidnPasswordTask DES QueryJsonObject " + desQuery);
				
				is = NetworkUtils.openContectionLocked(HaierServiceObject.getFindPasswordUrl("para", desQuery), MyApplication.getInstance().getSecurityKeyValuesObject());
			    if (is != null) {
			    	result = HaierResultObject.parse(NetworkUtils.getContentFromInput(is));
			    }
			} catch (ClientProtocolException e) {
				e.printStackTrace();
				result.mStatusMessage = e.getMessage();
			} catch (IOException e) {
				e.printStackTrace();
				result.mStatusMessage = MyApplication.getInstance().getGernalNetworkError();
			} catch (JSONException e) {
				e.printStackTrace();
				result.mStatusMessage = e.getMessage();
			} finally {
				NetworkUtils.closeInputStream(is);
			}
			return result;
		}
		@Override
		protected void onPostExecute(HaierResultObject result) {
			super.onPostExecute(result);
			dismissDialog(DIALOG_PROGRESS);
			if (result.isOpSuccessfully()) {
				MyApplication.getInstance().showMessage(result.mStatusMessage);
			} else {
				DialogUtils.createSimpleConfirmAlertDialog(mContext, mContext.getString(R.string.tel_not_register), null);
			}
			
		}
		@Override
		protected void onCancelled() {
			super.onCancelled();
			dismissDialog(DIALOG_PROGRESS);
		}
		
		
		
	}
	
	
	public static void startIntent(Context context, Bundle modelBundle) {
		Intent intent = new Intent(context, LoginActivity.class);
		if (modelBundle == null) {
			modelBundle = new Bundle();
		}
		intent.putExtras(modelBundle);
		context.startActivity(intent);
	}
	
}
