package com.bestjoy.app.haierstartservice.ui;

import java.io.IOException;
import java.io.InputStream;

import org.apache.http.client.ClientProtocolException;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.actionbarsherlock.view.Menu;
import com.bestjoy.app.haierstartservice.HaierServiceObject;
import com.bestjoy.app.haierstartservice.HaierServiceObject.HaierResultObject;
import com.bestjoy.app.haierstartservice.MyApplication;
import com.bestjoy.app.haierstartservice.R;
import com.bestjoy.app.haierstartservice.account.AccountObject;
import com.bestjoy.app.haierstartservice.account.HomeObject;
import com.bestjoy.app.haierstartservice.account.MyAccountManager;
import com.bestjoy.app.haierstartservice.view.HaierProCityDisEditPopView;
import com.bestjoy.app.utils.DebugUtils;
import com.shwy.bestjoy.utils.AsyncTaskUtils;
import com.shwy.bestjoy.utils.DevicesUtils;
import com.shwy.bestjoy.utils.Intents;
import com.shwy.bestjoy.utils.NetworkUtils;
import com.shwy.bestjoy.utils.SecurityUtils;


public class RegisterConfirmActivity extends BaseActionbarActivity implements View.OnClickListener{
	private static final String TAG = "RegisterActivity";

	private HaierProCityDisEditPopView mProCityDisEditPopView;
	
	private EditText mUsrNameEditText, usrPwdEditText, usrPwdConfirmEditText, usrHomeNameEditText;
	private String usrPwdConfirm;
	
	private AccountObject mAccountObject;
	
	private HomeObject mHomeObject;
	private Button mConfrimReg;
	private Bundle mBundles;
	
	private static final int REQUEST_LOGIN = 1;

	@Override
	protected boolean checkIntent(Intent intent) {
		mBundles = getIntent().getExtras();
		if (mBundles == null) {
			DebugUtils.logD(TAG, "finish due to checkIntent mBundles is null");
			return false;
		}
		String tel = mBundles.getString(Intents.EXTRA_TEL);
		if (TextUtils.isEmpty(tel)) {
			DebugUtils.logD(TAG, "finish due to checkIntent tel is null");
			return false;
		}
		mAccountObject = new AccountObject();
		mAccountObject.mAccountTel = tel;
		return true;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		DebugUtils.logD(TAG, "onCreate()");
		if (isFinishing()) {
			return;
		}
		removeDialog(DIALOG_PROGRESS);
		setContentView(R.layout.activity_register);
		this.initViews();
	}

	private void initViews() {
		mProCityDisEditPopView = new HaierProCityDisEditPopView(this);
		usrHomeNameEditText = (EditText) findViewById(R.id.tag);
		mUsrNameEditText = (EditText) findViewById(R.id.usr_name);
		usrPwdEditText = (EditText) findViewById(R.id.usr_pwd);
		usrPwdConfirmEditText = (EditText) findViewById(R.id.usr_repwd);
		
		mConfrimReg = (Button) findViewById(R.id.button_save_reg);
		mConfrimReg.setOnClickListener(this);
	}
	
	public static void startIntent(Context context, Bundle bundle) {
		Intent intent = new Intent(context, RegisterConfirmActivity.class);
		if (bundle == null) {
			bundle = new Bundle();
		}
		intent.putExtras(bundle);
		context.startActivity(intent);
	}
	
	private RegisterAsyncTask mRegisterAsyncTask;
	private void registerAsync(String... param) {
		AsyncTaskUtils.cancelTask(mRegisterAsyncTask);
		showDialog(DIALOG_PROGRESS);
		mConfrimReg.setEnabled(false);
		mRegisterAsyncTask = new RegisterAsyncTask();
		mRegisterAsyncTask.execute(param);
	}

	private class RegisterAsyncTask extends AsyncTask<String, Void, HaierResultObject> {
		@Override
		protected HaierResultObject doInBackground(String... params) {
			InputStream is = null;
			HaierResultObject haierResultObject = new HaierResultObject();
			try {
				JSONObject jsonQueryObject = new JSONObject();
				jsonQueryObject.put("cell", mAccountObject.mAccountTel)
				.put("UserName", mAccountObject.mAccountName)
				.put("Shen", mHomeObject.mHomeProvince)
				.put("Shi", mHomeObject.mHomeCity)
				.put("Qu", mHomeObject.mHomeDis)
				.put("detail", mHomeObject.mHomePlaceDetail)
				.put("pwd", mAccountObject.mAccountPwd)
				.put("iemi", DevicesUtils.getInstance().getImei())
				.put("imsi", DevicesUtils.getInstance().getIMSI())
				.put("Tag", usrHomeNameEditText.getText().toString().trim());
				DebugUtils.logD(TAG, "jsonQueryObject=" + jsonQueryObject.toString(4));
				String desQueryObject = SecurityUtils.DES.enCrypto(jsonQueryObject.toString().getBytes(), HaierServiceObject.DES_PASSWORD);
				DebugUtils.logD(TAG, "desQueryObject=" + desQueryObject);
				is = NetworkUtils.openContectionLocked(HaierServiceObject.getRegisterUrl("para", desQueryObject), MyApplication.getInstance().getSecurityKeyValuesObject());
				if (is == null) {
					throw new IOException();
				}
				haierResultObject = HaierResultObject.parse(NetworkUtils.getContentFromInput(is));
				if (haierResultObject.isOpSuccessfully()) {
					String dataStr = haierResultObject.mStrData;
					DebugUtils.logD(TAG, "Data = " + dataStr);
					mAccountObject.mAccountUid = Long.parseLong(dataStr.substring(dataStr.indexOf(":")+1));
					DebugUtils.logD(TAG, "Uid = " + mAccountObject.mAccountUid);
				}
			} catch (ClientProtocolException e) {
				e.printStackTrace();
				haierResultObject.mStatusMessage = e.getMessage();
			} catch (IOException e) {
				e.printStackTrace();
				haierResultObject.mStatusMessage = MyApplication.getInstance().getGernalNetworkError();
			} catch (JSONException e) {
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
			mConfrimReg.setEnabled(true);
			if (result.isOpSuccessfully()) {
				//注册后，我们要做一次登陆
				MyApplication.getInstance().showMessage(result.mStatusMessage);
				MyAccountManager.getInstance().saveLastUsrTel(mAccountObject.mAccountTel);
				startActivityForResult(LoginOrUpdateAccountDialog.createLoginOrUpdate(mContext, true, mAccountObject.mAccountTel, mAccountObject.mAccountPwd), REQUEST_LOGIN);
			} else if (result.mStatusCode == 2) {
				//2当前手机的电话号码已经注册了，我们需要弹框用户提示找回密码
				new AlertDialog.Builder(mContext)
				.setMessage(result.mStatusMessage)
				.setPositiveButton(R.string.button_find_password, new DialogInterface.OnClickListener() {
					
					@Override
					public void onClick(DialogInterface arg0, int arg1) {
						Bundle bundle = new Bundle();
						bundle.putString(Intents.EXTRA_TEL, mAccountObject.mAccountTel);
						LoginActivity.startIntent(mContext, bundle);
						finish();
					}
				})
				.setNegativeButton(R.string.button_cancel, null)
				.show();
			} else {
				MyApplication.getInstance().showMessage(result.mStatusMessage);
			}
			removeDialog(DIALOG_PROGRESS);
		}

		@Override
		protected void onCancelled() {
			super.onCancelled();
			removeDialog(DIALOG_PROGRESS);
			mConfrimReg.setEnabled(true);
		}
	}
	
	 public boolean onCreateOptionsMenu(Menu menu) {
		 return false;
	 }
	 
	@Override
	public void onClick(View view) {
		switch (view.getId()) {
			case R.id.button_save_reg:
				DebugUtils.logD(TAG, "button_save onClick");
				mAccountObject.mAccountName = mUsrNameEditText.getText().toString().trim();
				mAccountObject.mAccountPwd = usrPwdEditText.getText().toString().trim();
				usrPwdConfirm = usrPwdConfirmEditText.getText().toString().trim();

				mHomeObject = mProCityDisEditPopView.getHomeObject();
				
				if(valiInput()) {
					registerAsync();
				}
				break;
		}
	}

	private boolean valiInput() {
		if (mAccountObject != null && TextUtils.isEmpty(mAccountObject.mAccountName)) {
			MyApplication.getInstance().showMessage(R.string.msg_input_usr_name);
			return false;
		}
		if (TextUtils.isEmpty(mAccountObject.mAccountPwd)) {
			MyApplication.getInstance().showMessage(R.string.msg_input_usr_pwd);
			return false;
		}
		if (TextUtils.isEmpty(mHomeObject.mHomeProvince)) {
			MyApplication.getInstance().showMessage(R.string.msg_input_usr_pro);
			return false;
		}
		if (TextUtils.isEmpty(mHomeObject.mHomeCity)) {
			MyApplication.getInstance().showMessage(R.string.msg_input_usr_city);
			return false;
		}
		if (TextUtils.isEmpty(mHomeObject.mHomeDis)) {
			MyApplication.getInstance().showMessage(R.string.msg_input_usr_dis);
			return false;
		}
		if (!usrPwdConfirm.equals(mAccountObject.mAccountPwd)) {
			MyApplication.getInstance().showMessage(R.string.msg_input_pwd_not_match_tips);
			return false;
		}
		if (mAccountObject.mAccountPwd.length() < 6) {
			MyApplication.getInstance().showMessage(R.string.msg_usr_pwd_too_short_tips);
			return false;
		}
		if (TextUtils.isEmpty(mHomeObject.mHomePlaceDetail)) {
			MyApplication.getInstance().showMessage(R.string.msg_input_usr_place_detail);
			return false;
		}
		return true;
	}
	
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == REQUEST_LOGIN) {
			if (resultCode == Activity.RESULT_OK) {
				// login successfully
				MyApplication.getInstance().showMessage(R.string.msg_login_confirm_success);
				//注册成功，如果是先新建后注册，那么回到新建界面
//				int modelId = ModleSettings.getModelIdFromBundle(mBundles);
//				switch(modelId) {
//				case R.id.model_my_card:
//				case R.id.model_install:
//				case R.id.model_repair:
//					mBundles.putBoolean(Intents.EXTRA_HAS_REGISTERED, true);
//					NewCardActivity.startIntentClearTop(mContext, mBundles);
//					finish();
//					break;
//					default ://否则回到主界面
//						MainActivity.startActivityForTop(mContext);
//				}
			}
		} else {
			super.onActivityResult(requestCode, resultCode, data);
		}
	}
}
