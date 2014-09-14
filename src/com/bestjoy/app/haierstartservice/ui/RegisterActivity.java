package com.bestjoy.app.haierstartservice.ui;

import java.io.IOException;
import java.io.InputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.client.ClientProtocolException;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.telephony.SmsMessage;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.actionbarsherlock.view.Menu;
import com.bestjoy.app.haierstartservice.HaierServiceObject;
import com.bestjoy.app.haierstartservice.HaierServiceObject.HaierResultObject;
import com.bestjoy.app.haierstartservice.MyApplication;
import com.bestjoy.app.haierstartservice.R;
import com.bestjoy.app.utils.DebugUtils;
import com.shwy.bestjoy.utils.AsyncTaskUtils;
import com.shwy.bestjoy.utils.ComConnectivityManager;
import com.shwy.bestjoy.utils.Intents;
import com.shwy.bestjoy.utils.NetworkUtils;
import com.shwy.bestjoy.utils.SecurityUtils;

public class RegisterActivity extends BaseActionbarActivity implements View.OnClickListener{
	private static final String TAG = "RegisterActivity";
	
	private static final int TIME_COUNDOWN = 120000;
	
	private Button mNextButton;
	private Button mBtnGetyanzhengma;
	private EditText mTelInput;
	private EditText mCodeInput;
	private String mYanZhengCodeFromServer;

	@Override
	protected boolean checkIntent(Intent intent) {
		return true;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		DebugUtils.logD(TAG, "onCreate()");
		if (isFinishing()) {
			return ;
		}
		setContentView(R.layout.activity_register_confirm);
		this.initViews();
	}

	private void initViews() {
		mNextButton = (Button) findViewById(R.id.button_next);
		mNextButton.setOnClickListener(this);
		
		mBtnGetyanzhengma = (Button) findViewById(R.id.button_getyanzhengma);
		mBtnGetyanzhengma.setOnClickListener(this);
		
		mTelInput = (EditText) findViewById(R.id.usr_tel);
		mCodeInput = (EditText) findViewById(R.id.usr_validate);
	}
	
	 public boolean onCreateOptionsMenu(Menu menu) {
		 return false;
	 }

	public static void startIntent(Context context, Bundle modelBundel) {
		Intent intent = new Intent(context, RegisterActivity.class);
		if (modelBundel == null) {
			modelBundel = new Bundle();
		}
		intent.putExtras(modelBundel);
		context.startActivity(intent);
	}

	@Override
	public void onClick(View v) {
		// add by chenkai, 开始前先检查网络 begin
		if (!ComConnectivityManager.getInstance().isConnected()) {
			ComConnectivityManager.getInstance().onCreateNoNetworkDialog(this);
			return;
		}
		// add by chekai, 开始前先检查网络 end
		//modify by chenkai, 2014.06.04，去掉号码之间的空白符号 begin
		//String tel = mTelInput.getText().toString().trim();
		String tel = mTelInput.getText().toString().trim().replaceAll("[- +]", "");
		//modify by chenkai, 2014.06.04，去掉号码之间的空白符号 end
		String code = mCodeInput.getText().toString().trim();
		switch (v.getId()) {
			case R.id.button_next:
			if(TextUtils.isEmpty(tel)) {
				MyApplication.getInstance().showMessage(R.string.msg_input_usrtel);
				return;
			}
			//add by chenkai, 对手机号码非11位的排除注册, 2014.06.04 begin
			if (tel.length() < 11) {
				MyApplication.getInstance().showMessage(R.string.msg_input_usrtel_invalid);
				return;
			}
			//add by chenkai, 对手机号码非11位的排除注册, 2014.06.04 end
			if(TextUtils.isEmpty(code)) {
				MyApplication.getInstance().showMessage(R.string.msg_input_yanzheng_code);
				return;
			}
			if (mYanZhengCodeFromServer != null
					&& mYanZhengCodeFromServer.equals(SecurityUtils.MD5.md5(code))) {
				Bundle bundle = getIntent().getExtras();
				if (bundle == null) {
					bundle = new Bundle();
				}
				bundle.putString(Intents.EXTRA_TEL, tel);
				RegisterConfirmActivity.startIntent(this, bundle);
			} else {
				MyApplication.getInstance().showMessage(R.string.msg_input_yanzheng_code_error);
			}
				break;
			case R.id.button_getyanzhengma:
				if(!TextUtils.isEmpty(tel)) {
					mYanZhengCodeFromServer = null;
					mBtnGetyanzhengma.setEnabled(false);
					doTimeCountDown();
					loadYanzhengCodeAsync(tel);
				} else {
					MyApplication.getInstance().showMessage(R.string.msg_input_usrtel);
				}
				break;
		}
	}

	private void doTimeCountDown() {
		new TimeCount(TIME_COUNDOWN, 1000).start();
	}
	class TimeCount extends CountDownTimer {

		public TimeCount(long millisInFuture, long countDownInterval) {
			super(millisInFuture, countDownInterval);
			mBtnGetyanzhengma.setEnabled(false);
		}

		@Override
		public void onFinish() {
			mBtnGetyanzhengma.setText(RegisterActivity.this.getResources()
					.getString(R.string.button_re_vali));
			mBtnGetyanzhengma.setEnabled(true);
		}

		@Override
		public void onTick(long millisUntilFinished) {
			mBtnGetyanzhengma.setText(RegisterActivity.this.getResources()
					.getString(R.string.second, millisUntilFinished / 1000));
		}
	}
	private GetYanZhengCodeAsyncTask mGetYanZhengCodeAsyncTask;
	private ProgressDialog mGetYanZhengCodeDialog;
	private void loadYanzhengCodeAsync(String... param) {
		mCodeInput.setHint(R.string.usr_validate);
		AsyncTaskUtils.cancelTask(mGetYanZhengCodeAsyncTask);
		mGetYanZhengCodeDialog = getProgressDialog();
		if (mGetYanZhengCodeDialog == null) {
			showDialog(DIALOG_PROGRESS);
			mGetYanZhengCodeDialog = getProgressDialog();
			mGetYanZhengCodeDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
				
				@Override
				public void onCancel(DialogInterface dialog) {
					AsyncTaskUtils.cancelTask(mGetYanZhengCodeAsyncTask);
				}
			});
		} else {
			if (!mGetYanZhengCodeDialog.isShowing()) {
				mGetYanZhengCodeDialog.show();
			}
		}
		mGetYanZhengCodeAsyncTask = new GetYanZhengCodeAsyncTask();
		mGetYanZhengCodeAsyncTask.execute(param);
	}
	
	private class GetYanZhengCodeAsyncTask extends AsyncTask<String, Void, HaierResultObject> {
		@Override
		protected HaierResultObject doInBackground(String... params) {
			InputStream is = null;
			HaierResultObject result = new HaierResultObject();
			try {
				JSONObject queryJsonObject = new JSONObject();
				queryJsonObject.put("cell", params[0]);
				DebugUtils.logD(TAG, "GetYanZhengCodeAsyncTask run--queryJsonObject " + queryJsonObject.toString(4));
				String desQuery = SecurityUtils.DES.enCrypto(queryJsonObject.toString().getBytes(), HaierServiceObject.DES_PASSWORD);
				DebugUtils.logD(TAG, "GetYanZhengCodeAsyncTask DES QueryJsonObject " + desQuery);
				is = NetworkUtils.openContectionLocked(HaierServiceObject.getYanzhengmaUrl("para", desQuery), MyApplication.getInstance().getSecurityKeyValuesObject());
				if (is == null) {
					DebugUtils.logE(TAG, "openContectionLocked return null");
					result.mStatusMessage = mContext.getString(R.string.msg_get_yanzhengma_gernal_error);
					return result;
				}
				result = HaierResultObject.parse(NetworkUtils.getContentFromInput(is));
			} catch (ClientProtocolException e) {
				e.printStackTrace();
				result.mStatusMessage = e.getMessage();
			} catch (IOException e) {
				e.printStackTrace();
				result.mStatusMessage= MyApplication.getInstance().getGernalNetworkError();
			} catch (JSONException e) {
				e.printStackTrace();
				result.mStatusMessage= e.getMessage();
			} finally {
				NetworkUtils.closeInputStream(is);
			}
			return result;
		}

		@Override
		protected void onPostExecute(HaierResultObject result) {
			super.onPostExecute(result);
			if (mGetYanZhengCodeDialog != null) {
				mGetYanZhengCodeDialog.hide();
			}
			//statuscode 0 已注册  1 获取验证码成功
			if (result.isOpSuccessfully()) {
				//提示用户已注册过了
				mYanZhengCodeFromServer = result.mJsonData.optString("randcode", "");
				if (mYanZhengCodeFromServer.equals("")) {
					MyApplication.getInstance().showMessage(R.string.msg_yanzheng_code_msg_has_registered);
					return;
				} else {
					MyApplication.getInstance().showMessage(R.string.msg_yanzheng_code_msg_send);
					//add by chenkai, 开始监听验证码短信
					if (HaierServiceObject.isSupportReceiveYanZhengMa()) {
						mCodeInput.setHint(R.string.hint_wait_yanzhengma_sms);
						register();
					}
				}
			} else {
				MyApplication.getInstance().showMessage(result.mStatusMessage);
				mYanZhengCodeFromServer = "";
			}
		}

		@Override
		protected void onCancelled() {
			super.onCancelled();
			if (mGetYanZhengCodeDialog != null) {
				mGetYanZhengCodeDialog.hide();
			}
		}
		
	}
	
	//add by chenkai, 增加读取验证码短信，并回填验证码 begin
	private static final String SMS_ACTION = "android.provider.Telephony.SMS_RECEIVED";
	private static final Pattern YANZHENGMA_PATTERN = Pattern.compile(".+(\\d{6})");
	private YanZhengMaReceiver mYanZhengMaReceiver;
	private void register() {
		if (mYanZhengMaReceiver == null) {
			DebugUtils.logD(TAG, "register mYanZhengMaReceiver");
			mYanZhengMaReceiver = new YanZhengMaReceiver();
			IntentFilter filter = new IntentFilter(SMS_ACTION);
			filter.setPriority(Integer.MAX_VALUE);
			registerReceiver(mYanZhengMaReceiver, filter);
		}
		
	}
	
	private void unregister() {
		if (mYanZhengMaReceiver != null) {
			DebugUtils.logD(TAG, "unregister mYanZhengMaReceiver");
			unregisterReceiver(mYanZhengMaReceiver);
			mYanZhengMaReceiver = null;
		}
	}
	
	private class YanZhengMaReceiver extends BroadcastReceiver{

		@Override
		public void onReceive(Context context, Intent intent) {
			DebugUtils.logD(TAG, "onReceive intent " + intent);
			if (SMS_ACTION.equals(intent.getAction())) {
				//回填验证码
				SmsMessage[] smsMessages = Intents.getMessagesFromIntent(intent);
				String message  = smsMessages[0].getMessageBody();
				String address = smsMessages[0].getOriginatingAddress();
				DebugUtils.logD(TAG, "message " + message);
				DebugUtils.logD(TAG, "address " + address);
				if (!TextUtils.isEmpty(address) 
						&& address.length() > 11 
						&& !(address.startsWith("86") || address.startsWith("+86"))
						&& message.contains(context.getString(R.string.haier_yanzhengma_verify2))) {
					Matcher matcher = YANZHENGMA_PATTERN.matcher(message);
					if (matcher.find()) {
						mCodeInput.setText(matcher.group(1));
						DebugUtils.logD(TAG, "find yanzhengma " + matcher.group(1));
						mCodeInput.setHint(R.string.usr_validate);
						//add by chenkai, 移除监听验证码短信
						unregister();
						abortBroadcast();
					}
				}
			}
		}
		
	};
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		//add by chenkai, 移除监听验证码短信
		if (HaierServiceObject.isSupportReceiveYanZhengMa()) unregister();
	}
	//add by chenkai, 增加读取验证码短信，并回填验证码 end
}
