package com.bestjoy.app.haierstartservice.ui;

import java.io.IOException;
import java.io.InputStream;

import org.apache.http.client.ClientProtocolException;
import org.json.JSONException;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.widget.TextView;

import com.bestjoy.app.haierstartservice.HaierServiceObject;
import com.bestjoy.app.haierstartservice.HaierServiceObject.HaierResultObject;
import com.bestjoy.app.haierstartservice.MyApplication;
import com.bestjoy.app.haierstartservice.R;
import com.bestjoy.app.haierstartservice.account.AccountObject;
import com.bestjoy.app.haierstartservice.account.AccountParser;
import com.bestjoy.app.haierstartservice.account.MyAccountManager;
import com.bestjoy.app.haierstartservice.service.IMService;
import com.bestjoy.app.haierstartservice.update.UpdateService;
import com.bestjoy.app.utils.DebugUtils;
import com.bestjoy.app.utils.YouMengMessageHelper;
import com.shwy.bestjoy.utils.AsyncTaskUtils;
import com.shwy.bestjoy.utils.Intents;
import com.shwy.bestjoy.utils.NetworkUtils;
/**
 * 这个类用来更新和登录账户使用。
 * @author chenkai
 *
 */
public class LoginOrUpdateAccountDialog extends Activity{

	private static final String TAG = "LoginOrUpdateAccountDialog";
	private AccountObject mAccountObject;
	private String mTel, mPwd;
	private boolean mIsLogin = false;
	private TextView mStatusView;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_login_or_update_layout);
		mStatusView = (TextView) findViewById(R.id.title);
		Intent intent = getIntent();
		mIsLogin = intent.getBooleanExtra(Intents.EXTRA_TYPE, true);
		mTel = intent.getStringExtra(Intents.EXTRA_TEL);
		mPwd = intent.getStringExtra(Intents.EXTRA_PASSWORD);
		loginAsync();
	}

	private LoginAsyncTask mLoginAsyncTask;
	private void loginAsync() {
		mStatusView.setText(mIsLogin?R.string.msg_login_dialog_title_wait:R.string.msg_update_dialog_title_wait);
		AsyncTaskUtils.cancelTask(mLoginAsyncTask);
		mLoginAsyncTask = new LoginAsyncTask();
		mLoginAsyncTask.execute();
	}
	private class LoginAsyncTask extends AsyncTask<Void, Void, HaierResultObject> {

		private InputStream _is;
		@Override
		protected HaierResultObject doInBackground(Void... params) {
			HaierResultObject resultObject = new HaierResultObject();
			mAccountObject = new AccountObject();;
			try {
				MyApplication.getInstance().postAsync(new Runnable() {
					
					@Override
					public void run() {
						mStatusView.setText(R.string.msg_login_download_accountinfo_wait);
					}
				});
				_is = NetworkUtils.openContectionLocked(HaierServiceObject.getLoginOrUpdateUrl(mTel, mPwd), null);
				resultObject = HaierResultObject.parse(NetworkUtils.getContentFromInput(_is));
				if (resultObject.isOpSuccessfully()) {
					//登录成功，我们解析登录返回数据
					AccountParser.parseAccountData(resultObject.mJsonData, mAccountObject);
					if (mAccountObject != null && mAccountObject.hasUid()) {
						boolean saveAccountOk = MyAccountManager.getInstance().saveAccountObject(LoginOrUpdateAccountDialog.this.getContentResolver(), mAccountObject);
						if (!saveAccountOk) {
							//登录成功了，但本地数据保存失败，通常不会走到这里
							resultObject.mStatusMessage = LoginOrUpdateAccountDialog.this.getString(R.string.msg_login_save_success);
						}
					} 
				}
				
			} catch (ClientProtocolException e) {
				e.printStackTrace();
				resultObject.mStatusMessage = e.getMessage();
			} catch (IOException e) {
				e.printStackTrace();
				resultObject.mStatusMessage = MyApplication.getInstance().getGernalNetworkError();
			} catch (JSONException e) {
				e.printStackTrace();
				resultObject.mStatusMessage = e.getMessage();
			} finally {
				NetworkUtils.closeInputStream(_is);
			}
			return resultObject;
		}

		@Override
		protected void onPostExecute(HaierResultObject result) {
			super.onPostExecute(result);
			if (isCancelled()) {
				//通常不走到这里
				onCancelled();
				finish();
				return;
			}
			if (!result.isOpSuccessfully()) {
				MyApplication.getInstance().showMessage(result.mStatusMessage);
				setResult(Activity.RESULT_CANCELED);
			} else if (mAccountObject.hasUid() && mAccountObject.hasId()){
				//如果登陆成功
				IMService.connectIMService(LoginOrUpdateAccountDialog.this);
				setResult(Activity.RESULT_OK);
				//每次登陆，我们都需要注册设备Token
				YouMengMessageHelper.getInstance().saveDeviceTokenStatus(false);
				//登录成功，我们需要检查是否能够上传设备Token到服务器绑定uid和token
				UpdateService.startCheckDeviceTokenToService(LoginOrUpdateAccountDialog.this);
			} else {
				MyApplication.getInstance().showMessage(mAccountObject.mStatusMessage);
				setResult(Activity.RESULT_CANCELED);
			}
			finish();
		}

		@Override
		protected void onCancelled() {
			super.onCancelled();
			MyApplication.getInstance().showMessage(R.string.msg_op_canceled);
			setResult(Activity.RESULT_CANCELED);
			finish();
			
		}
		
		public void cancelTask(boolean cancel) {
			super.cancel(cancel);
			//由于IO操作是不可中断的，所以我们这里关闭IO流来终止任务
			NetworkUtils.closeInputStream(_is);
			
		}
		
	}
	
	@Override
	public void onBackPressed() {
		super.onBackPressed();
		if (mLoginAsyncTask != null) {
			mLoginAsyncTask.cancelTask(true);
			DebugUtils.logD(TAG, "login or update is canceled by user");
		}
	}
	
	public static Intent createLoginOrUpdate(Context context, boolean login, String tel, String pwd) {
		Intent intent = new Intent(context, LoginOrUpdateAccountDialog.class);
		intent.putExtra(Intents.EXTRA_TYPE, login);
		intent.putExtra(Intents.EXTRA_TEL, tel);
		intent.putExtra(Intents.EXTRA_PASSWORD, pwd);
		return intent;
	}
	
}
