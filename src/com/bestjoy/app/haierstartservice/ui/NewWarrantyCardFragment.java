package com.bestjoy.app.haierstartservice.ui;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;

import org.apache.http.client.ClientProtocolException;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.Dialog;
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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.bestjoy.app.haierstartservice.HaierServiceObject;
import com.bestjoy.app.haierstartservice.HaierServiceObject.HaierResultObject;
import com.bestjoy.app.haierstartservice.MyApplication;
import com.bestjoy.app.haierstartservice.R;
import com.bestjoy.app.haierstartservice.account.AccountObject;
import com.bestjoy.app.haierstartservice.account.BaoxiuCardObject;
import com.bestjoy.app.haierstartservice.account.MyAccountManager;
import com.bestjoy.app.haierstartservice.service.PhotoManagerUtilsV2;
import com.bestjoy.app.haierstartservice.view.HaierPopView;
import com.bestjoy.app.utils.DebugUtils;
import com.bestjoy.app.utils.DialogUtils;
import com.shwy.bestjoy.utils.AsyncTaskUtils;
import com.shwy.bestjoy.utils.ComConnectivityManager;
import com.shwy.bestjoy.utils.DateUtils;
import com.shwy.bestjoy.utils.ImageHelper;
import com.shwy.bestjoy.utils.InfoInterface;
import com.shwy.bestjoy.utils.NetworkUtils;
import com.shwy.bestjoy.utils.SecurityUtils;

public class NewWarrantyCardFragment extends ModleBaseFragment implements View.OnClickListener{
	private static final String TAG = "NewWarrantyCardFragment";
	private static final String TOKEN = NewWarrantyCardFragment.class.getName();
	//按钮
	private Button mSaveBtn;
	private TextView mDatePickBtn;
	private ImageView mBillImageView;
	private EditText mTypeInput, mPinpaiInput, mModelInput, mBianhaoInput, mBaoxiuTelInput, mWyInput, mTagInput;
	private EditText mPriceInput, mYanbaoComponyInput, mYanbaoTelInput;
	private Calendar mCalendar;
	/**购买途径、延保时间*/
	private HaierPopView mTujingPopView, mYanbaoPopView;
	//临时的拍摄照片路径
	private File mBillTempFile, mAvatorTempFile;
	/**请求商品预览图*/
	private static final int REQUEST_AVATOR = 2;
	/**请求发票预览图*/
	private static final int REQUEST_BILL = 3;
	
	/**是否要重新拍摄商品预览图*/
	private static final int DIALOG_PICTURE_AVATOR_CONFIRM = 4;
	private static final int DIALOG_BILL_OP_CONFIRM = 5;
	
	private int mPictureRequest = -1;
	
	private BaoxiuCardObject mBaoxiuCardObject;
	private Handler mHandler;
	private Bundle mBundle;
	
	private boolean mNeedLoadFapiao = false;
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		PhotoManagerUtilsV2.getInstance().requestToken(TOKEN);
		setHasOptionsMenu(true);
		mCalendar = Calendar.getInstance();
		if (savedInstanceState == null) {
			mBundle = getArguments();
			DebugUtils.logD(TAG, "onCreate() savedInstanceState == null, getArguments() mBundle=" + mBundle);
		} else {
			mBundle = savedInstanceState.getBundle(TAG);
			mNeedLoadFapiao = savedInstanceState.getBoolean("mNeedLoadFapiao", false);
			DebugUtils.logD(TAG, "onCreate() savedInstanceState != null, restore mBundle=" + mBundle + ", mNeedLoadFapiao=" + mNeedLoadFapiao);
		}
		mHandler = new Handler() {

			@Override
			public void handleMessage(Message msg) {
				super.handleMessage(msg);
				DebugUtils.logD(TAG, "handleMessage() loadFapiaoFromCameraAsync");
				loadFapiaoFromCameraAsync();
			}
			
		};
		initTempFile();
		
		mBaoxiuCardObject = BaoxiuCardObject.getBaoxiuCardObject(mBundle);
	}
	
	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		DebugUtils.logD(TAG, "onSaveInstanceState() save mBundle=" + mBundle + ", mNeedLoadFapiao=" + mNeedLoadFapiao);
		outState.putBundle(TAG, mBundle);
		outState.putBoolean("mNeedLoadFapiao", mNeedLoadFapiao);
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
	

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.activity_new_card, container, false);
		mBillImageView = (ImageView) view.findViewById(R.id.button_scan_bill);
		mBillImageView.setOnClickListener(this);

		mTypeInput = (EditText) view.findViewById(R.id.product_type_input);
		mPinpaiInput = (EditText) view.findViewById(R.id.product_brand_input);
		mModelInput = (EditText) view.findViewById(R.id.product_model_input);
		mBianhaoInput = (EditText) view.findViewById(R.id.product_sn_input);
		mBaoxiuTelInput = (EditText) view.findViewById(R.id.product_tel_input);
		// 保修期
		mWyInput = (EditText) view.findViewById(R.id.product_wy_input);
		// 购买日期
		mDatePickBtn = (TextView) view.findViewById(R.id.product_buy_date);
		mDatePickBtn.setOnClickListener(this);

		mDatePickBtn.setText(DateUtils.TOPIC_DATE_TIME_FORMAT.format(mCalendar.getTime()));

		mPriceInput = (EditText) view.findViewById(R.id.product_buy_cost);
		// mTujingInput = (EditText) view.findViewById(R.id.product_buy_entry);
		mTujingPopView = new HaierPopView(getActivity(), view, R.id.edit_product_buy_tujing, R.id.menu_choose_tujing);
		mYanbaoPopView = new HaierPopView(getActivity(), view, R.id.product_buy_delay_time, R.id.menu_choose_yanbao);
		// mYanbaoTimeInput = (EditText)
		// view.findViewById(R.id.product_buy_delay_time);
		mYanbaoComponyInput = (EditText) view.findViewById(R.id.product_buy_delay_componey);
		mYanbaoTelInput = (EditText) view.findViewById(R.id.product_buy_delay_componey_tel);
		// 增加标签
		mTagInput = (EditText) view.findViewById(R.id.product_beizhu_tag);

		mSaveBtn = (Button) view.findViewById(R.id.button_save);
		mSaveBtn.setOnClickListener(this);

		mTujingPopView.setDataSource(getResources().getStringArray(R.array.buy_places));
		mYanbaoPopView.setDataSource(getResources().getStringArray(R.array.yanbao_times));
		view.findViewById(R.id.button_scan_qrcode).setOnClickListener(this);

		view.findViewById(R.id.menu_choose).setOnClickListener(this);
		populateBaoxiuInfoView();
		return view;
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
	}
	
	
	@Override
	public void onDestroyView() {
		super.onDestroyView();
		DebugUtils.logD(TAG, "onDestroyView() mNeedLoadFapiao=" + mNeedLoadFapiao + ", mBillTempFile=" + mBillTempFile);
		PhotoManagerUtilsV2.getInstance().releaseToken(TOKEN);
		BaoxiuCardObject.showBill(getActivity(), null);
		AsyncTaskUtils.cancelTask(mLoadFapiaoTask);
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		DebugUtils.logD(TAG, "onDestroy() mNeedLoadFapiao=" + mNeedLoadFapiao + ", mBillTempFile=" + mBillTempFile);
		mBillImageView.setImageBitmap(null);
		mBundle.putBundle("BaoxiuCardObject", null);
	}
	
	@Override
	public Dialog onCreateDialog(int id) {
		switch(id) {
		case DIALOG_BILL_OP_CONFIRM:
			return new AlertDialog.Builder(getActivity())
			.setItems(this.getResources().getStringArray(R.array.bill_op_items), new DialogInterface.OnClickListener() {
				
				@Override
				public void onClick(DialogInterface dialog, int which) {
					switch(which) {
					case 0:
						BaoxiuCardObject.showBill(getActivity(), mBaoxiuCardObject);
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
	
	public boolean isEditable() {
		return mBaoxiuCardObject.mBID > 0;
	}
	
	private void populateBaoxiuInfoView() {
		//init layouts
		if (!isEditable()) {
			mTypeInput.setText(mBaoxiuCardObject.mLeiXin);
			mPinpaiInput.setText(mBaoxiuCardObject.mPinPai);
			mModelInput.setText(mBaoxiuCardObject.mXingHao);
			mBianhaoInput.setText(mBaoxiuCardObject.mSHBianHao);
			mBaoxiuTelInput.setText(mBaoxiuCardObject.mBXPhone);
			mWyInput.setText(mBaoxiuCardObject.mWY);
			mPriceInput.getText().clear();
			//mTujingInput.getText().clear();
			mTujingPopView.getText().clear();
			mYanbaoPopView.getText().clear();
			//mYanbaoTimeInput.getText().clear();
			mYanbaoComponyInput.getText().clear();
			mYanbaoTelInput.getText().clear();
			mTagInput.getText().clear();
		} else {
			mTypeInput.setText(mBaoxiuCardObject.mLeiXin);
			mPinpaiInput.setText(mBaoxiuCardObject.mPinPai);
			mModelInput.setText(mBaoxiuCardObject.mXingHao);
			mBianhaoInput.setText(mBaoxiuCardObject.mSHBianHao);
			
			mBaoxiuTelInput.setText(mBaoxiuCardObject.mBXPhone);
			mWyInput.setText(mBaoxiuCardObject.mWY);
			mPriceInput.setText(mBaoxiuCardObject.mBuyPrice);
			mTujingPopView.setText(mBaoxiuCardObject.mBuyTuJing);
			mYanbaoPopView.setText(mBaoxiuCardObject.mYanBaoTime);
			mYanbaoComponyInput.setText(mBaoxiuCardObject.mYanBaoDanWei);
			mYanbaoTelInput.setText(mBaoxiuCardObject.mYBPhone);
			mTagInput.setText(mBaoxiuCardObject.mCardName);
			
			try {
				if(mBaoxiuCardObject.mBuyDate != null) {
					Date date = BaoxiuCardObject.BUY_DATE_TIME_FORMAT.parse(mBaoxiuCardObject.mBuyDate);
					mCalendar.setTime(date);
					mDatePickBtn.setText(DateUtils.TOPIC_DATE_TIME_FORMAT.format(date));
				}
			} catch (ParseException e) {
				e.printStackTrace();
			}
			
			//如果有发票，我们显示出来
			if (mBaoxiuCardObject.hasBillAvator()) {
				//modify by chenkai, 20140701, 将发票地址存进数据库（不再拼接），增加海尔奖励延保时间 begin 
				//为了传值給发票下载
				BaoxiuCardObject.setBaoxiuCardObject(mBaoxiuCardObject);
				PhotoManagerUtilsV2.getInstance().loadPhotoAsync(TOKEN, mBillImageView, mBaoxiuCardObject.getFapiaoPhotoId(), null, PhotoManagerUtilsV2.TaskType.FaPiao);
				//modify by chenkai, 20140701, 将发票地址存进数据库（不再拼接），增加海尔奖励延保时间 end 
			}
			
			//设置标题为编辑保修卡
			getActivity().setTitle(R.string.button_edit_card);
			mSaveBtn.setText(R.string.button_update);
		}
		
	}
	
	public void setBaoxiuObjectAfterSlideMenu(InfoInterface slideManuObject) {
		if (slideManuObject instanceof BaoxiuCardObject) {
			BaoxiuCardObject object = (BaoxiuCardObject) slideManuObject;
			mTypeInput.setText(object.mLeiXin);
			mPinpaiInput.setText(object.mPinPai);
			mModelInput.setText(object.mXingHao);
			mBianhaoInput.setText(object.mSHBianHao);
			mBaoxiuTelInput.setText(object.mBXPhone);
			mWyInput.setText(object.mWY);
			mBaoxiuCardObject.mKY = object.mKY;
//			if (!TextUtils.isEmpty(object.mLeiXin)) {
//				mTypeInput.setText(object.mLeiXin);
//			}
//			if (!TextUtils.isEmpty(object.mPinPai)) {
//				mPinpaiInput.setText(object.mPinPai);
//			}
//			
//			if (!TextUtils.isEmpty(object.mXingHao)) {
//				mModelInput.setText(object.mXingHao);
//			}
//			
//			if (!TextUtils.isEmpty(object.mSHBianHao)) {
//				mBianhaoInput.setText(object.mSHBianHao);
//			}
//			
//			if (!TextUtils.isEmpty(object.mBXPhone)) {
//				mBaoxiuTelInput.setText(object.mBXPhone);
//			}
//			
//			if (!TextUtils.isEmpty(object.mWY)) {
//				mWyInput.setText(object.mWY);
//			}
		}
	}
	
	private BaoxiuCardObject getmBaoxiuCardObject() {
		mBaoxiuCardObject.mLeiXin = mTypeInput.getText().toString().trim();
		mBaoxiuCardObject.mPinPai = mPinpaiInput.getText().toString().trim();
		mBaoxiuCardObject.mXingHao = mModelInput.getText().toString().trim();
		mBaoxiuCardObject.mSHBianHao = mBianhaoInput.getText().toString().trim();
		mBaoxiuCardObject.mBXPhone = mBaoxiuTelInput.getText().toString().trim();
		mBaoxiuCardObject.mWY = mWyInput.getText().toString().trim();
		
		mBaoxiuCardObject.mBuyDate = BaoxiuCardObject.BUY_DATE_TIME_FORMAT.format(mCalendar.getTime());
		mBaoxiuCardObject.mBuyPrice = mPriceInput.getText().toString().trim();
		//mBaoxiuCardObject.mBuyTuJing = mTujingInput.getText().toString().trim();
		mBaoxiuCardObject.mBuyTuJing = mTujingPopView.getText().toString().trim();
		String yanbaotime = mYanbaoPopView.getText().toString().trim();
		if (yanbaotime != null && yanbaotime.contains(getActivity().getString(R.string.year))) yanbaotime = yanbaotime.substring(0, yanbaotime.length() - 1);
		mBaoxiuCardObject.mYanBaoTime = yanbaotime;
		mBaoxiuCardObject.mYanBaoDanWei = mYanbaoComponyInput.getText().toString().trim();
		mBaoxiuCardObject.mYBPhone = mYanbaoTelInput.getText().toString().trim();
		
		mBaoxiuCardObject.mCardName = mTagInput.getText().toString().trim();
		
		return mBaoxiuCardObject;
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
		case R.id.button_scan_qrcode:
//			Intent scanIntent = new Intent(getActivity(), CaptureActivity.class);
//			scanIntent.putExtra(Intents.EXTRA_SCAN_TASK, true);
//			startActivityForResult(scanIntent, REQUEST_SCAN);
			startScan();
			break;
		case R.id.product_buy_date:
			showDatePickerDialog();
			break;
		case R.id.button_save:
//			if (mBaoxiuCardObject.hasBill()) {
//				saveNewWarrantyCardAndSync();
//			} else {
//				MyApplication.getInstance().showMessage(R.string.msg_cant_show_bill);
//			}
			 if (ComConnectivityManager.getInstance().isConnected()) {
				//如果没有注册，我们前往登陆界面
				 if (!checkInput()) {
					 return;
				 }
				if (isEditable()) {
					updateWarrantyCardAsync();
				} else {
					saveNewWarrantyCardAndSync();
				}
			 } else {
				 showDialog(DIALOG_DATA_NOT_CONNECTED);
			 }
			break;
		case R.id.menu_choose:
			//如果内容为空，我们显示侧边栏
			((NewCardActivity) getActivity()).getSlidingMenu().showMenu(true);
			break;
		}
		
	}
	
	private void saveNewWarrantyCardAndSync() {
		if(MyAccountManager.getInstance().hasLoginned()) {
			mSaveBtn.setEnabled(false);
			requestNewWarrantyCardAndSync();
		} else {
			//如果没有注册，我们前往登陆/注册界面，这里传递ModelBundle对象过去，以便做合适的跳转
			MyApplication.getInstance().showMessage(R.string.login_tip);
			LoginActivity.startIntent(this.getActivity(), getArguments());
		}
	}
	

	private CreateNewWarrantyCardAsyncTask mCreateNewWarrantyCardAsyncTask;
	private void requestNewWarrantyCardAndSync(String... param) {
		AsyncTaskUtils.cancelTask(mCreateNewWarrantyCardAsyncTask);
		showDialog(DIALOG_PROGRESS);
		mCreateNewWarrantyCardAsyncTask = new CreateNewWarrantyCardAsyncTask();
		mCreateNewWarrantyCardAsyncTask.execute(param);
	}

	private class CreateNewWarrantyCardAsyncTask extends AsyncTask<String, Void, HaierResultObject> {
		/*{
		    "StatusCode": "1", 
		    "StatusMessage": "成功返回数据", 
		    "Data": "Bid:4"
		}*/
		@Override
		protected HaierResultObject doInBackground(String... params) {
			//更新保修卡信息
			BaoxiuCardObject baoxiuCardObject = getmBaoxiuCardObject();
			DebugUtils.logD(TAG, "CreateNewWarrantyCardAsyncTask for AID " + baoxiuCardObject.mAID);
			HaierResultObject haierResultObject = new HaierResultObject();
			InputStream is = null;
			//mdofify by chenkai, 修改发票后台同步修改新建更新和登录后台, 20140622 begin
			//StringBuilder paramValue = new StringBuilder();
			//paramValue.append(baoxiuCardObject.mLeiXin)
			//.append("|").append(baoxiuCardObject.mBuyDate)
			//.append("|").append(baoxiuCardObject.mBuyPrice)
			//.append("|").append(baoxiuCardObject.mBuyTuJing)
			//.append("|").append(baoxiuCardObject.mBXPhone)
			//.append("|").append(baoxiuCardObject.mPinPai)
			//.append("|").append(String.valueOf(baoxiuCardObject.mUID))
			//.append("|").append(baoxiuCardObject.mXingHao)
			//.append("|").append(baoxiuCardObject.mYanBaoDanWei)
			//.append("|").append(baoxiuCardObject.mYanBaoTime)
			//.append("|").append(String.valueOf(baoxiuCardObject.mAID))	
			//.append("|").append(baoxiuCardObject.mSHBianHao)
			//.append("|").append(baoxiuCardObject.mCardName)
			//.append("|").append(baoxiuCardObject.mYBPhone);
			//paramValue.append("|").append(baoxiuCardObject.getBase64StringFromBillAvator());
			//paramValue.append("|").append(baoxiuCardObject.mWY);
			//DebugUtils.logD(TAG, "param " + paramValue.toString());
			AccountObject accountObject = MyAccountManager.getInstance().getAccountObject();
			try {
				String imgstr = baoxiuCardObject.getBase64StringFromBillAvator();
				DebugUtils.logD(TAG, "imgstr=" + imgstr);
				JSONObject jsonObject = new JSONObject();
				jsonObject.put("AID", baoxiuCardObject.mAID)
				.put("BuyDate", baoxiuCardObject.mBuyDate)
				.put("BuyPrice", baoxiuCardObject.mBuyPrice)
				.put("BuyTuJing", baoxiuCardObject.mBuyTuJing)
				.put("BXPhone", baoxiuCardObject.mBXPhone)
				.put("SHBianHao", baoxiuCardObject.mSHBianHao)
				.put("token", SecurityUtils.MD5.md5(accountObject.mAccountTel + accountObject.mAccountPwd)) //md5(cell+pwd)
				.put("Tag", baoxiuCardObject.mCardName)
				.put("UID", baoxiuCardObject.mUID)
				.put("WY", baoxiuCardObject.mWY)
				.put("XingHao", baoxiuCardObject.mXingHao)
				.put("YanBaoDanWei", baoxiuCardObject.mYanBaoDanWei)
				.put("YanBaoTime", baoxiuCardObject.mYanBaoTime)
				.put("YBPhone", baoxiuCardObject.mYBPhone)
				.put("LeiXin", baoxiuCardObject.mLeiXin)
				.put("PinPai", baoxiuCardObject.mPinPai)
				.put("imgstr", imgstr);
				
				DebugUtils.logD(TAG, "bjson=" + jsonObject.toString());
				is = NetworkUtils.openPostContectionLocked(HaierServiceObject.getCreateBaoxiucardUri(), "bjson", jsonObject.toString(), MyApplication.getInstance().getSecurityKeyValuesObject());
				try {
					String content = NetworkUtils.getContentFromInput(is);
					if (TextUtils.isEmpty(content)) {
						haierResultObject.mStatusCode = -99;
						return haierResultObject;
					}
					haierResultObject = HaierResultObject.parse(content);
					if (haierResultObject.isOpSuccessfully()) {
						//如果后台返回了保修卡数据,我们解析它保存在本地
						if (haierResultObject.mJsonData != null) {
							baoxiuCardObject = BaoxiuCardObject.parseBaoxiuCards(haierResultObject.mJsonData, accountObject);
							DebugUtils.logE(TAG, "new BID=" + baoxiuCardObject.mBID);
							if (baoxiuCardObject.mBID > 0) {
//								//将临时图片存成发票,如果没有改变，则什么操作也不做
//								baoxiuCardObject.saveBillAvatorTempFileLocked();
								//正常情况
								boolean savedOk = baoxiuCardObject.saveInDatebase(getActivity().getContentResolver(), null);
								if (!savedOk) {
									//通常不会发生
									haierResultObject.mStatusMessage = getActivity().getString(R.string.msg_local_save_card_failed);
								} else {
									MyAccountManager.getInstance().updateHomeObject(baoxiuCardObject.mAID);
								}
								return haierResultObject;
							}
						}
						haierResultObject.mStatusMessage = getActivity().getString(R.string.msg_local_save_card_failed);;
					}
				} catch (JSONException e) {
					e.printStackTrace();
					haierResultObject.mStatusMessage = getActivity().getString(R.string.msg_local_save_card_failed_due_to_parse_exception);
				}
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
			mSaveBtn.setEnabled(true);
			dissmissDialog(DIALOG_PROGRESS);
			if (result.mStatusCode == -99) {
				DialogUtils.createSimpleConfirmAlertDialog(getActivity(), getActivity().getString(R.string.msg_get_no_content_from_server), null);
				return;
			}
			if (result.isOpSuccessfully()) {
				//添加成功
				//add by chenkai, 锁定认证字段 20140701 begin
				 /**
				  *  2.3 rewardStatus=1锁定保修卡的饿时候，提醒用户:
				  */
				 if (mBaoxiuCardObject.isLocked()) {
					 MyApplication.getInstance().showLockedEditMode(getActivity(), result.mStatusMessage, new DialogInterface.OnClickListener() {
						
						@Override
						public void onClick(DialogInterface dialog, int which) {
//							MyApplication.getInstance().showMessage(result.mStatusMessage);
							getActivity().finish();
							mBaoxiuCardObject.clear();
							mBundle.putBundle("BaoxiuCardObject", null);
							MyChooseDevicesActivity.startIntent(getActivity(), getArguments());
						}
					});
				 } else {
					 MyApplication.getInstance().showMessage(result.mStatusMessage);
					getActivity().finish();
					mBaoxiuCardObject.clear();
					mBundle.putBundle("BaoxiuCardObject", null);
					MyChooseDevicesActivity.startIntent(getActivity(), getArguments());
				 }
				//add by chenkai, 锁定认证字段 20140701 end
				
			} else {
				MyApplication.getInstance().showMessage(result.mStatusMessage);
			}
		}

		@Override
		protected void onCancelled() {
			super.onCancelled();
			dissmissDialog(DIALOG_PROGRESS);
			mSaveBtn.setEnabled(true);
		}
	}
	
	//########################更新操作 开始################################################
	/*
	 ServerIP/Haier/UpdateBaoXiu.ashx
	参数：
	LeiXin 
	XingHao 
	SHBianHao 
	BXPhone 
	BuyTuJing 
	YanBaoDanWei 
	Tag 
	YanBaoTime 
	YBPhone 
	BID 
	BuyDate 
	BuyPrice 
	FPaddr 
	AID 
	说明
	跟添加保修数据的参数唯一不同的事一个有UID(用户ID)没有BID 
	这个有BID(保修ID) 没有用户ID 

	 */
	private UpdateWarrantyCardAsyncTask mUpdateWarrantyCardAsyncTask;
	private void updateWarrantyCardAsync() {
		mSaveBtn.setEnabled(false);
		AsyncTaskUtils.cancelTask(mUpdateWarrantyCardAsyncTask);
		showDialog(DIALOG_PROGRESS);
		mUpdateWarrantyCardAsyncTask = new UpdateWarrantyCardAsyncTask();
		mUpdateWarrantyCardAsyncTask.execute();
	}

	private class UpdateWarrantyCardAsyncTask extends AsyncTask<Void, Void, HaierResultObject> {
		/*{
		    "StatusCode": "1", 
		    "StatusMessage": "成功返回数据", 
		    "Data": "Bid:4"
		}*/
		@Override
		protected HaierResultObject doInBackground(Void... params) {
			//更新保修卡信息
			BaoxiuCardObject baoxiuCardObject = getmBaoxiuCardObject();
			DebugUtils.logD(TAG, "UpdateWarrantyCardAsyncTask BID " + baoxiuCardObject.mBID);
			HaierResultObject haierResultObject = new HaierResultObject();
			InputStream is = null;
			//mdofify by chenkai, 修改发票后台同步修改新建更新和登录后台, 20140622 begin
			//StringBuilder paramValue = new StringBuilder();
			//paramValue.append(baoxiuCardObject.mLeiXin)
			//.append("|").append(baoxiuCardObject.mBuyDate)
			//.append("|").append(baoxiuCardObject.mBuyPrice)
			//.append("|").append(baoxiuCardObject.mBuyTuJing)
			//.append("|").append(baoxiuCardObject.mBXPhone)
			//.append("|").append(baoxiuCardObject.mPinPai)
			//.append("|").append(String.valueOf(baoxiuCardObject.mBID))
			//.append("|").append(baoxiuCardObject.mXingHao)
			//.append("|").append(baoxiuCardObject.mYanBaoDanWei)
			//.append("|").append(baoxiuCardObject.mYanBaoTime)
			//.append("|").append(String.valueOf(baoxiuCardObject.mAID))	
			//.append("|").append(baoxiuCardObject.mSHBianHao)
			//.append("|").append(baoxiuCardObject.mCardName)
			//.append("|").append(baoxiuCardObject.mYBPhone);
			//paramValue.append("|").append(baoxiuCardObject.getBase64StringFromBillAvator());
			//paramValue.append("|").append(baoxiuCardObject.mWY);
			//DebugUtils.logD(TAG, "param " + paramValue.toString());
			AccountObject accountObject = MyAccountManager.getInstance().getAccountObject();
			try {
				String imgstr = baoxiuCardObject.getBase64StringFromBillAvator();
				DebugUtils.logD(TAG, "imgstr=" + imgstr);
				JSONObject jsonObject = new JSONObject();
				jsonObject.put("AID", baoxiuCardObject.mAID)
				.put("BuyDate", baoxiuCardObject.mBuyDate)
				.put("BuyPrice", baoxiuCardObject.mBuyPrice)
				.put("BuyTuJing", baoxiuCardObject.mBuyTuJing)
				.put("BXPhone", baoxiuCardObject.mBXPhone)
				.put("SHBianHao", baoxiuCardObject.mSHBianHao)
				.put("token", SecurityUtils.MD5.md5(accountObject.mAccountTel + accountObject.mAccountPwd)) //md5(cell+pwd)
				.put("Tag", baoxiuCardObject.mCardName)
				.put("UID", baoxiuCardObject.mUID)
				.put("WY", baoxiuCardObject.mWY)
				.put("XingHao", baoxiuCardObject.mXingHao)
				.put("YanBaoDanWei", baoxiuCardObject.mYanBaoDanWei)
				.put("YanBaoTime", baoxiuCardObject.mYanBaoTime)
				.put("YBPhone", baoxiuCardObject.mYBPhone)
				.put("LeiXin", baoxiuCardObject.mLeiXin)
				.put("PinPai", baoxiuCardObject.mPinPai)
				.put("BID", baoxiuCardObject.mBID)
				.put("imgstr", imgstr);
				DebugUtils.logD(TAG, "bjson=" + jsonObject.toString(4));
				is = NetworkUtils.openPostContectionLocked(HaierServiceObject.getUpdateBaoxiucardUri(), "bjson", jsonObject.toString(), MyApplication.getInstance().getSecurityKeyValuesObject());
				
				haierResultObject = HaierResultObject.parse(NetworkUtils.getContentFromInput(is));
				if (haierResultObject.isOpSuccessfully()) {
					
					if (haierResultObject.mJsonData != null) {
						baoxiuCardObject = BaoxiuCardObject.parseBaoxiuCards(haierResultObject.mJsonData, accountObject);
						DebugUtils.logE(TAG, "updated BID=" + baoxiuCardObject.mBID);
						if (baoxiuCardObject.mBID > 0) {
							//将临时图片存成发票,如果没有改变，则什么操作也不做
							//baoxiuCardObject.saveBillAvatorTempFileLocked();
							//如果后台返回了保修卡数据,我们解析它保存在本地
							//正常情况
							boolean updated = baoxiuCardObject.saveInDatebase(getActivity().getContentResolver(), null);
							if (!updated) {
								//通常不会发生
								//通常不会发生
								DebugUtils.logD(TAG, "UpdateWarrantyCardAsyncTask " + getActivity().getString(R.string.msg_local_save_card_failed));
								haierResultObject.mStatusMessage = getActivity().getString(R.string.msg_local_save_card_failed);
							} else {
								//更新家以便设备列表能够看到
								MyAccountManager.getInstance().updateHomeObject(baoxiuCardObject.mAID);
							}
							return haierResultObject;
						}
					}
				}
						
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
			mSaveBtn.setEnabled(true);
			dissmissDialog(DIALOG_PROGRESS);
			if (result.isOpSuccessfully()) {
				//添加成功
				//add by chenkai, 锁定认证字段 20140701 begin
				 /**
				  *  2.3 rewardStatus=1锁定保修卡的饿时候，提醒用户:
				  */
				 if (mBaoxiuCardObject.isLocked()) {
					 MyApplication.getInstance().showLockedEditMode(getActivity(), result.mStatusMessage, new DialogInterface.OnClickListener() {
						
						@Override
						public void onClick(DialogInterface dialog, int which) {
//							MyApplication.getInstance().showMessage(result.mStatusMessage);
							getActivity().finish();
							mBaoxiuCardObject.clear();
							MyChooseDevicesActivity.startIntent(getActivity(), getArguments());
						}
					});
				 } else {
					 MyApplication.getInstance().showMessage(result.mStatusMessage);
					getActivity().finish();
					mBaoxiuCardObject.clear();
					mBundle.putBundle("BaoxiuCardObject", null);
					MyChooseDevicesActivity.startIntent(getActivity(), getArguments());
				 }
				//add by chenkai, 锁定认证字段 20140701 end
			} else {
				MyApplication.getInstance().showMessage(result.mStatusMessage);
			}
		}

		@Override
		protected void onCancelled() {
			super.onCancelled();
			dissmissDialog(DIALOG_PROGRESS);
			mSaveBtn.setEnabled(true);
		}
	}
	
	
	//########################更新操作 结束#################################################

	private boolean checkInput() {
		if(TextUtils.isEmpty(mTypeInput.getText().toString().trim())){
			showEmptyInputToast(R.string.product_type);
			return false;
		}
		if(TextUtils.isEmpty(mPinpaiInput.getText().toString().trim())){
			showEmptyInputToast(R.string.product_brand);
			return false;
		}
		if(TextUtils.isEmpty(mModelInput.getText().toString().trim())){
			showEmptyInputToast(R.string.product_model);
			return false;
		}
//		if(TextUtils.isEmpty(mBianhaoInput.getText().toString().trim())){
//			showEmptyInputToast(R.string.product_sn);
//			return false;
//		}
		if(TextUtils.isEmpty(mBaoxiuTelInput.getText().toString().trim())){
			showEmptyInputToast(R.string.product_tel);
			return false;
		}
		/*if(TextUtils.isEmpty(mDatePickBtn.getText().toString().trim())){
			showEmptyInputToast(R.string.product_buy_date);
			return false;
		}
		if(TextUtils.isEmpty(mPriceInput.getText().toString().trim())){
			showEmptyInputToast(R.string.product_buy_cost);
			return false;
		}
		if(TextUtils.isEmpty(mTujingInput.getText().toString().trim())){
			showEmptyInputToast(R.string.product_buy_entry);
			return false;
		}
		if(TextUtils.isEmpty(mYanbaoTimeInput.getText().toString().trim())){
			showEmptyInputToast(R.string.product_buy_delay_time);
			return false;
		}
		if(TextUtils.isEmpty(mYanbaoComponyInput.getText().toString().trim())){
			showEmptyInputToast(R.string.product_buy_delay_componey);
			return false;
		}
		if(TextUtils.isEmpty(mYanbaoTelInput.getText().toString().trim())){
			showEmptyInputToast(R.string.product_buy_delay_componey_tel);
			return false;
		}*/
		return true;
	}

	private void showEmptyInputToast(int resId) {
		String msg = getResources().getString(resId);
		MyApplication.getInstance().showMessage(getResources().getString(R.string.input_type_please_input) + msg);
	}

	private void showDatePickerDialog() {
        new DatePickerDialog(getActivity(), new DatePickerDialog.OnDateSetListener() {
			@Override
			public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
				mCalendar.set(year, monthOfYear, dayOfMonth);
				//更新UI
				mDatePickBtn.setText(DateUtils.TOPIC_DATE_TIME_FORMAT.format(mCalendar.getTime()));
			}
				
		}, mCalendar.get(Calendar.YEAR), mCalendar.get(Calendar.MONTH), mCalendar.get(Calendar.DAY_OF_MONTH))
		.show();
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
			}
		}
		super.onActivityResult(requestCode, resultCode, data);
	}
	
	@Override
    public void setScanObjectAfterScan(InfoInterface barCodeObject) {
		 BaoxiuCardObject object = (BaoxiuCardObject) barCodeObject;
		 mTypeInput.setText(object.mLeiXin);
		 mPinpaiInput.setText(object.mPinPai);
		 mBianhaoInput.setText(object.mSHBianHao);
		 mModelInput.setText(object.mXingHao);
		 mBaoxiuTelInput.setText(object.mBXPhone);
		 mWyInput.setText(object.mWY);
		//这里一般我们只设置品牌、型号、编号和名称
//		if (!TextUtils.isEmpty(object.mLeiXin)) {
//			mTypeInput.setText(object.mLeiXin);
//		}
//		if (!TextUtils.isEmpty(object.mPinPai)) {
//			mPinpaiInput.setText(object.mPinPai);
//		}
//		if (!TextUtils.isEmpty(object.mSHBianHao)) {
//			mBianhaoInput.setText(object.mSHBianHao);
//		}
//		if (!TextUtils.isEmpty(object.mXingHao)) {
//			mModelInput.setText(object.mXingHao);
//		}
//		if (!TextUtils.isEmpty(object.mBXPhone)) {
//			mBaoxiuTelInput.setText(object.mBXPhone);
//		}
//		if (!TextUtils.isEmpty(object.mWY)) {
//			mWyInput.setText(object.mWY);
//		}
	}
	
	@Override
	public InfoInterface getScanObjectAfterScan() {
		return BaoxiuCardObject.getBaoxiuCardObject();
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
	
	@Override
	public void updateArguments(Bundle bundle) {
		mBundle = bundle;
		mBaoxiuCardObject.mAID = mBundle.getLong("aid", -1);
		mBaoxiuCardObject.mUID = mBundle.getLong("uid", -1);
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
			} else {
				new AlertDialog.Builder(getActivity())
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
	public void updateInfoInterface(InfoInterface infoInterface) {
		
	}
	
}
