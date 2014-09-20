package com.bestjoy.app.haierstartservice.ui;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.http.client.ClientProtocolException;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.bestjoy.app.haierstartservice.HaierServiceObject;
import com.bestjoy.app.haierstartservice.HaierServiceObject.HaierResultObject;
import com.bestjoy.app.haierstartservice.MyApplication;
import com.bestjoy.app.haierstartservice.R;
import com.bestjoy.app.haierstartservice.account.AccountObject;
import com.bestjoy.app.haierstartservice.account.BaoxiuCardObject;
import com.bestjoy.app.haierstartservice.account.HomeObject;
import com.bestjoy.app.haierstartservice.account.MyAccountManager;
import com.bestjoy.app.haierstartservice.view.HaierProCityDisEditPopView;
import com.bestjoy.app.utils.DebugUtils;
import com.bestjoy.app.utils.DialogUtils;
import com.shwy.bestjoy.utils.AsyncTaskUtils;
import com.shwy.bestjoy.utils.ImageHelper;
import com.shwy.bestjoy.utils.InfoInterface;
import com.shwy.bestjoy.utils.Intents;
import com.shwy.bestjoy.utils.NetworkUtils;
import com.shwy.bestjoy.utils.SecurityUtils;

public class CreateUserBaoxiucardActivity extends BaseNoActionBarActivity implements View.OnClickListener{

	private static final String TAG = "CreateUserBaoxiucardActivity";
	public static  DateFormat FAPIAO_DATE_FORMAT = new SimpleDateFormat("yyyy/MM/dd");
	public static  DateFormat FAPIAO_DATE_FORMAT_TO_SERVER = new SimpleDateFormat("yyyyMMdd");
	private HaierProCityDisEditPopView mProCityDisEditPopView;
	private EditText mUsrNameEditText, mUsrTelEditText;
	
	private TextView mBillDateText;
	private EditText mSnInput;
	private ImageView mBillImageView;
	
	private AccountObject mAccountObject;
	
	private HomeObject mUserHomeObject;
	private Button mConfrimBtn;
	//临时的拍摄照片路径
	private File mBillTempFile, mAvatorTempFile;
	/**请求商品预览图*/
	private static final int REQUEST_AVATOR = 2;
	/**请求发票预览图*/
	private static final int REQUEST_BILL = 3;
	/**请求扫描条码*/
	public static final int REQUEST_SCAN = 10000;
	
	/**是否要重新拍摄商品预览图*/
	private static final int DIALOG_PICTURE_AVATOR_CONFIRM = 4;
	private static final int DIALOG_BILL_OP_CONFIRM = 5;
	
	private int mPictureRequest = -1;
	
	private BaoxiuCardObject mBaoxiuCardObject;
	private Handler mHandler;
	
	private boolean mNeedLoadFapiao = false;
	
	private Date mFapiaoDate;
	
	private CustomerInfo mCustomerInfo = new CustomerInfo();;
	class CustomerInfo {
		private String _name, _tel;
		private HomeObject _homeObject;
		
	}
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (isFinishing()) {
			return;
		}
		setShowHomeUp(true);
		mHandler = new Handler() {

			@Override
			public void handleMessage(Message msg) {
				super.handleMessage(msg);
				DebugUtils.logD(TAG, "handleMessage() loadFapiaoFromCameraAsync");
				loadFapiaoFromCameraAsync();
			}
			
		};
		initTempFile();
		setContentView(R.layout.activity_create_user_baoxiucard);
		mBaoxiuCardObject = new BaoxiuCardObject();
		mBaoxiuCardObject.clear();
		initUserInfo();
		initCustomerView();
		mBillImageView = (ImageView) findViewById(R.id.button_scan_bill);
		mBillImageView.setOnClickListener(this);
		mBillDateText = (TextView) findViewById(R.id.date);
		
		mBillDateText.setVisibility(View.INVISIBLE);
		
		mSnInput = (EditText) findViewById(R.id.sn);
		
		findViewById(R.id.button_scan_sn).setOnClickListener(this);
		
		findViewById(R.id.button_save).setOnClickListener(this);
	}
	
	@Override
	public void onResume() {
		super.onResume();
		DebugUtils.logD(TAG, "onResume() mNeedLoadFapiao=" + mNeedLoadFapiao + ", mBillTempFile=" + mBillTempFile);
		if (mNeedLoadFapiao) {
			DebugUtils.logD(TAG, "onResume() removeMessages REQUEST_BILL, sendEmptyMessage REQUEST_BILL");
			mHandler.removeMessages(REQUEST_BILL);
			mHandler.sendEmptyMessageDelayed(REQUEST_BILL, 500);
		}
		
	}
	private void initTempFile() {
		File tempRootDir = Environment.getExternalStorageDirectory();
		mBillTempFile = new File(tempRootDir, ".billTemp");
		mAvatorTempFile = new File(tempRootDir, ".avatorTemp");
	}
	private void initUserInfo() {
		TextView infoView = (TextView) findViewById(R.id.name);
		infoView.setText(mAccountObject.mAccountName);
		
		infoView = (TextView) findViewById(R.id.tel);
		infoView.setText(mAccountObject.mAccountTel);
		
		infoView = (TextView) findViewById(R.id.org);
		infoView.setText(mAccountObject.mAccountOrg);
		
		infoView = (TextView) findViewById(R.id.workplace);
		infoView.setText(mAccountObject.mAccountWorkaddress);
		
	}
	
	private void initCustomerView() {
		mProCityDisEditPopView = new HaierProCityDisEditPopView(this);
		
		mUsrNameEditText = (EditText) findViewById(R.id.usr_name);
		mUsrTelEditText = (EditText) findViewById(R.id.usr_tel);
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		DebugUtils.logD(TAG, "onDestroy() mNeedLoadFapiao=" + mNeedLoadFapiao + ", mBillTempFile=" + mBillTempFile);
		mBillImageView.setImageBitmap(null);
	}
	
    public void setScanObjectAfterScan(InfoInterface barCodeObject) {
		 BaoxiuCardObject object = (BaoxiuCardObject) barCodeObject;
		 mBaoxiuCardObject.mLeiXin = object.mLeiXin;
		 mBaoxiuCardObject.mPinPai = object.mPinPai;
		 mBaoxiuCardObject.mSHBianHao = object.mSHBianHao;
		 mBaoxiuCardObject.mXingHao = object.mXingHao;
		 mBaoxiuCardObject.mBXPhone = object.mBXPhone;
		 mBaoxiuCardObject.mWY = object.mWY;
		 
		 mSnInput.getText().clear();
		 mSnInput.setText(object.mXingHao);
	}
    
    @Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		DebugUtils.logD(TAG, "onActivityResult() requestCode=" + requestCode + ", resultCode=" + resultCode);
		if (resultCode == Activity.RESULT_OK) {
			if (REQUEST_BILL == requestCode) {
                if (mBillTempFile.exists()) {
                	mNeedLoadFapiao = true;
				}
                return;
			} else if (requestCode == REQUEST_SCAN) {
	   			   //识别到了信息
				   setScanObjectAfterScan(BaoxiuCardObject.getBaoxiuCardObject());
	   		}
		}
		super.onActivityResult(requestCode, resultCode, data);
	}
	
	/**
	 * 调用相机拍摄图片
	 */
	private void onCapturePhoto() {
		if (!MyApplication.getInstance().hasExternalStorage()) {
			showDialog(DIALOG_MEDIA_UNMOUNTED);
			return;
		}
		Intent intent = null;
		if (mPictureRequest == REQUEST_AVATOR) {
			intent = ImageHelper.createCaptureIntent(Uri.fromFile(mAvatorTempFile));
		} else if (mPictureRequest == REQUEST_BILL) {
			intent = ImageHelper.createCaptureIntent(Uri.fromFile(mBillTempFile));
		}
		startActivityForResult(intent, mPictureRequest);
	}
	
	private LoadFapiaoTask mLoadFapiaoTask;
	private void loadFapiaoFromCameraAsync() {
		AsyncTaskUtils.cancelTask(mLoadFapiaoTask);
		showDialog(DIALOG_PROGRESS);
		mLoadFapiaoTask = new LoadFapiaoTask();
		mLoadFapiaoTask.execute();
	}
	private class LoadFapiaoTask extends AsyncTask<Void, Void, Boolean> {

		private Bitmap last = null;
		@Override
        protected void onPreExecute() {
	        super.onPreExecute();
	        last = mBaoxiuCardObject.mBillTempBitmap;
        }
		@Override
		protected Boolean doInBackground(Void... arg0) {
			DebugUtils.logW(TAG, "LoadFapiaoTask doInBackground()");
			int tryTime = 0;
			while (tryTime < 2) {
				try {
					mBaoxiuCardObject.updateBillAvatorTempLocked(mBillTempFile);
					return true;
				} catch(OutOfMemoryError e) {
					e.printStackTrace();
					DebugUtils.logW(TAG, "updateBillAvatorTempLocked oom " + e.getMessage());
					tryTime ++;
				}
			}
			return false;
		}

		@Override
		protected void onPostExecute(Boolean result) {
			super.onPostExecute(result);
			DebugUtils.logW(TAG, "LoadFapiaoTask onPostExecute()");
			dismissDialog(DIALOG_PROGRESS);
			if (result) {
				mBillImageView.setImageBitmap(mBaoxiuCardObject.mBillTempBitmap);
				if (last != null) {
					last.recycle();
				}
				mFapiaoDate = new Date();
				mBillDateText.setText(getString(R.string.format_fapiao_date, FAPIAO_DATE_FORMAT.format(mFapiaoDate)));
				mBillDateText.setVisibility(View.VISIBLE);
			} else {
				new AlertDialog.Builder(mContext)
				.setMessage(R.string.error_oom_for_fapiao)
				.setPositiveButton(R.string.button_ok, null)
				.show();
			}
			mNeedLoadFapiao = false;
		}

		@Override
		protected void onCancelled() {
			super.onCancelled();
			DebugUtils.logW(TAG, "LoadFapiaoTask onCancelled()");
			dismissDialog(DIALOG_PROGRESS);
			mNeedLoadFapiao = false;
		}
		
	}
	@Override
	public Dialog onCreateDialog(int id) {
		switch(id) {
		case DIALOG_BILL_OP_CONFIRM:
			return new AlertDialog.Builder(mContext)
			.setItems(this.getResources().getStringArray(R.array.bill_op_items), new DialogInterface.OnClickListener() {
				
				@Override
				public void onClick(DialogInterface dialog, int which) {
					switch(which) {
					case 0:
						BaoxiuCardObject.showBill(mContext, mBaoxiuCardObject);
						break;
					case 1:
						mPictureRequest = REQUEST_BILL;
						onCapturePhoto();
						break;
					}
					
				}
			})
			.setNegativeButton(android.R.string.cancel, null)
			.create();
		}
		
		return super.onCreateDialog(id);
	}
	@Override
	protected boolean checkIntent(Intent intent) {
		mAccountObject = MyAccountManager.getInstance().getAccountObject();
		DebugUtils.logD(TAG, "checkIntent() mAccountObject.isLogined() " + mAccountObject.hasUid());
		return mAccountObject.hasUid();
	}
	
	public static void startActivity(Context context) {
		Intent intent = new Intent(context, CreateUserBaoxiucardActivity.class);
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		context.startActivity(intent);
	}
	
	@Override
	public void onClick(View v) {
		switch(v.getId()) {
		case R.id.button_scan_bill:
			if (mBaoxiuCardObject != null && !mBaoxiuCardObject.hasLocalBill()) {
				//如果没有发票，我们直接调用相机
				mPictureRequest = REQUEST_BILL;
				onCapturePhoto();
			} else {
				//如果有，我们显示操作选项，查看或是拍摄发票
				onCreateDialog(DIALOG_BILL_OP_CONFIRM).show();
			}
			break;
		case R.id.button_scan_sn:
			Intent scanIntent = new Intent(mContext, CaptureActivity.class);
			scanIntent.putExtra(Intents.EXTRA_SCAN_TASK, true);
			startActivityForResult(scanIntent, REQUEST_SCAN);
			break;
		case R.id.button_save:
			if (checkInput()) {
				requestNewWarrantyCardAndSync();
			}
			break;
		}
		
	}
	
	private boolean checkInput() {
		mCustomerInfo._name = mUsrNameEditText.getText().toString().trim();
		if (TextUtils.isEmpty(mCustomerInfo._name)) {
			MyApplication.getInstance().showMessage(R.string.msg_empty_card_name);
			return false;
		}
		
		mCustomerInfo._tel = mUsrTelEditText.getText().toString().trim();
		if (TextUtils.isEmpty(mCustomerInfo._tel)) {
			MyApplication.getInstance().showMessage(R.string.msg_empty_card_name);
			return false;
		}
		mCustomerInfo._homeObject = mProCityDisEditPopView.getHomeObject();
		if (TextUtils.isEmpty(mCustomerInfo._homeObject.mHomeDis)) {
			MyApplication.getInstance().showMessage(R.string.msg_empty_card_address);
			return false;
		}
		mBaoxiuCardObject.mXingHao = mSnInput.getText().toString().trim();
		//新建保修卡的时候，发票的拍摄日期就是购买日期
		if (mFapiaoDate != null) {
			mBaoxiuCardObject.mBuyDate = FAPIAO_DATE_FORMAT_TO_SERVER.format(mFapiaoDate);
		}
		if (TextUtils.isEmpty(mBaoxiuCardObject.mXingHao)) {
			MyApplication.getInstance().showMessage(R.string.msg_empty_card_xinghao);
			return false;
		}
		return true;
	}
	
	
	
	//新建保修卡
	private CreateNewWarrantyCardAsyncTask mCreateNewWarrantyCardAsyncTask;
	private void requestNewWarrantyCardAndSync() {
		AsyncTaskUtils.cancelTask(mCreateNewWarrantyCardAsyncTask);
		showDialog(DIALOG_PROGRESS);
		mCreateNewWarrantyCardAsyncTask = new CreateNewWarrantyCardAsyncTask();
		mCreateNewWarrantyCardAsyncTask.execute();
	}

	private class CreateNewWarrantyCardAsyncTask extends AsyncTask<Void, Void, HaierResultObject> {
		/*{
		    "StatusCode": "1", 
		    "StatusMessage": "成功返回数据", 
		    "Data": "Bid:4"
		}*/
		@Override
		protected HaierResultObject doInBackground(Void... params) {
			//更新保修卡信息
			DebugUtils.logD(TAG, "CreateNewWarrantyCardAsyncTask for AID " + mBaoxiuCardObject.mAID);
			HaierResultObject haierResultObject = new HaierResultObject();
			InputStream is = null;
			//mdofify by chenkai, 修改发票后台同步修改新建更新和登录后台, 20140622 begin
			AccountObject accountObject = MyAccountManager.getInstance().getAccountObject();
			try {
				String imgstr = mBaoxiuCardObject.getBase64StringFromBillAvator();
				DebugUtils.logD(TAG, "imgstr=" + imgstr);
				JSONObject jsonObject = new JSONObject();
				jsonObject.put("AID", mBaoxiuCardObject.mAID)
				.put("BuyDate", mBaoxiuCardObject.mBuyDate)
				.put("BuyPrice", mBaoxiuCardObject.mBuyPrice)
				.put("BuyTuJing", mBaoxiuCardObject.mBuyTuJing)
				.put("BXPhone", mBaoxiuCardObject.mBXPhone)
				.put("SHBianHao", mBaoxiuCardObject.mSHBianHao)
				.put("token", SecurityUtils.MD5.md5(accountObject.mAccountTel + accountObject.mAccountPwd)) //md5(cell+pwd)
				.put("Tag", mBaoxiuCardObject.mCardName)
				.put("UID", mBaoxiuCardObject.mUID)
				.put("WY", mBaoxiuCardObject.mWY)
				.put("XingHao", mBaoxiuCardObject.mXingHao)
				.put("YanBaoDanWei", mBaoxiuCardObject.mYanBaoDanWei)
				.put("YanBaoTime", mBaoxiuCardObject.mYanBaoTime)
				.put("YBPhone", mBaoxiuCardObject.mYBPhone)
				.put("LeiXin", mBaoxiuCardObject.mLeiXin)
				.put("PinPai", mBaoxiuCardObject.mPinPai)
				.put("imgstr", imgstr);
				
				
				//添加客户资料信息
				JSONObject user = new JSONObject();
				user.put("username", mCustomerInfo._name)
				.put("cell", mCustomerInfo._tel)
				.put("regionid", mProCityDisEditPopView.getDisID())
				.put("detailaddr", mCustomerInfo._homeObject.mHomePlaceDetail);
				
				jsonObject.put("suid", mAccountObject.mAccountUid)
				.put("AddUser", user);
				
				
				DebugUtils.logD(TAG, "bjson=" + jsonObject.toString());
				is = NetworkUtils.openPostContectionLocked(HaierServiceObject.createUserBaoxiucardUrl(), "bjson", jsonObject.toString(), MyApplication.getInstance().getSecurityKeyValuesObject());
				String content = NetworkUtils.getContentFromInput(is);
				if (TextUtils.isEmpty(content)) {
					haierResultObject.mStatusCode = -99;
					return haierResultObject;
				}
				return HaierResultObject.parse(content);
			} catch (ClientProtocolException e) {
				e.printStackTrace();
				haierResultObject.mStatusMessage = e.getMessage();
			} catch (IOException e) {
				e.printStackTrace();
				haierResultObject.mStatusMessage = e.getMessage();
			} catch (JSONException e) {
				e.printStackTrace();
				haierResultObject.mStatusMessage = e.getMessage();
			} finally {
				NetworkUtils.closeInputStream(is);
			}
			//mdofify by chenkai, 修改发票后台同步修改新建更新和登录后台, 20140622 end
			return haierResultObject;
		}

		@Override
		protected void onPostExecute(final HaierResultObject result) {
			super.onPostExecute(result);
			dismissDialog(DIALOG_PROGRESS);
			if (result.mStatusCode == -99) {
				DialogUtils.createSimpleConfirmAlertDialog(mContext, getString(R.string.msg_get_no_content_from_server), null);
				return;
			}
			if (result.isOpSuccessfully()) {
				//添加成功
				//add by chenkai, 锁定认证字段 20140701 begin
				 /**
				  *  2.3 rewardStatus=1锁定保修卡的饿时候，提醒用户:
				  */
				 if (mBaoxiuCardObject.isLocked()) {
					 MyApplication.getInstance().showLockedEditMode(mContext, result.mStatusMessage, new DialogInterface.OnClickListener() {
						
						@Override
						public void onClick(DialogInterface dialog, int which) {
//							MyApplication.getInstance().showMessage(result.mStatusMessage);
							finish();
							mBaoxiuCardObject.clear();
						}
					});
				 } else {
					MyApplication.getInstance().showMessage(result.mStatusMessage);
					finish();
					mBaoxiuCardObject.clear();
				 }
				//add by chenkai, 锁定认证字段 20140701 end
				
			} else {
				MyApplication.getInstance().showMessage(result.mStatusMessage);
			}
		}

		@Override
		protected void onCancelled() {
			super.onCancelled();
			dismissDialog(DIALOG_PROGRESS);
		}
	}


}
