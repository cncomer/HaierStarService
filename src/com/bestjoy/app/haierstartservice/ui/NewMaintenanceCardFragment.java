package com.bestjoy.app.haierstartservice.ui;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;

import org.apache.http.client.ClientProtocolException;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import com.bestjoy.app.haierstartservice.HaierServiceObject;
import com.bestjoy.app.haierstartservice.MyApplication;
import com.bestjoy.app.haierstartservice.R;
import com.bestjoy.app.haierstartservice.account.AccountObject;
import com.bestjoy.app.haierstartservice.account.BaoxiuCardObject;
import com.bestjoy.app.haierstartservice.account.HomeObject;
import com.bestjoy.app.haierstartservice.account.MyAccountManager;
import com.bestjoy.app.haierstartservice.view.HaierProCityDisEditPopView;
import com.bestjoy.app.utils.DebugUtils;
import com.bestjoy.app.utils.SpeechRecognizerEngine;
import com.shwy.bestjoy.utils.AsyncTaskUtils;
import com.shwy.bestjoy.utils.DateUtils;
import com.shwy.bestjoy.utils.InfoInterface;
import com.shwy.bestjoy.utils.Intents;
import com.shwy.bestjoy.utils.NetworkUtils;

public class NewMaintenanceCardFragment extends ModleBaseFragment implements View.OnClickListener{
	private static final String TAG = "NewRepairCardFragment";
	//按钮
	private Button mSaveBtn;
	//商品信息
	private EditText mTypeInput, mPinpaiInput, mModelInput, mBianhaoInput, mBaoxiuTelInput, mTagInput;
	//联系人信息
	private EditText mContactNameInput, mContactTelInput;
	//private ProCityDisEditView mProCityDisEditView;
	private HaierProCityDisEditPopView mProCityDisEditPopView;
	
	//预约信息
	private TextView mYuyueDate, mYuyueTime;
	private Calendar mCalendar;
	
	private EditText mAskInput;
	//private Handler mHandler;
	private Button mSpeakButton;
	private SpeechRecognizerEngine mSpeechRecognizerEngine;
	
	private BaoxiuCardObject mBaoxiuCardObject;
	private Bundle mBundle;
	
	private ScrollView mScrollView;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setHasOptionsMenu(true);
		getActivity().setTitle(R.string.activity_title_maintenance);
		mCalendar = Calendar.getInstance();
		if (savedInstanceState == null) {
			mBundle = getArguments();
			DebugUtils.logD(TAG, "onCreate() savedInstanceState == null, getArguments() mBundle=" + mBundle);
		} else {
			mBundle = savedInstanceState.getBundle(TAG);
			DebugUtils.logD(TAG, "onCreate() savedInstanceState != null, restore mBundle=" + mBundle);
		}
		mBaoxiuCardObject = BaoxiuCardObject.getBaoxiuCardObject(mBundle);
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		 View view = inflater.inflate(R.layout.activity_repair_20140418, container, false);
		 
		 mScrollView = (ScrollView) view.findViewById(R.id.scrollview);
		 
		 //条码识别
		 view.findViewById(R.id.button_scan_qrcode).setOnClickListener(this);
		 
		 //商品信息
		 mTypeInput = (EditText) view.findViewById(R.id.product_type_input);
		 mPinpaiInput = (EditText) view.findViewById(R.id.product_brand_input);
		 mModelInput = (EditText) view.findViewById(R.id.product_model_input);
		 mBianhaoInput = (EditText) view.findViewById(R.id.product_sn_input);
		 mBianhaoInput.setHint(R.string.hint_optional);
		 mBaoxiuTelInput = (EditText) view.findViewById(R.id.product_tel_input);
		 mTagInput = (EditText) view.findViewById(R.id.product_beizhu_tag);
		 
		 
		 //联系人
		 ((TextView) view.findViewById(R.id.people_info_title)).setTextColor(getResources().getColor(R.color.light_green));
		 view.findViewById(R.id.people_info_divider).setBackgroundResource(R.color.light_green);
		 mContactNameInput = (EditText) view.findViewById(R.id.contact_name_input);
		 mContactTelInput = (EditText) view.findViewById(R.id.contact_tel_input);
		 /*mProCityDisEditView = (ProCityDisEditView) view.findViewById(R.id.home);
		 //不要显示HomeName输入框
		 mProCityDisEditView.setHomeEditVisiable(View.GONE);*/
		 mProCityDisEditPopView = new HaierProCityDisEditPopView(this.getActivity(), view);
//		 mProCityDisEditPopView.setOnClickListener(new OnClickListener() {
//
//			@Override
//			public void onClick(View v) {
//				mScrollView.scrollTo(0, -(int) v.getTop() + 20);
//				
//			}
//
//			 
//		 });
		 //设置问题描述文本
		 TextView issureDesc = (TextView) view.findViewById(R.id.desc);
	     issureDesc.setText(R.string.maintennace_desc);
		 //语音
		 mAskInput = (EditText) view.findViewById(R.id.product_ask_online_input);
		 mSpeakButton =  (Button) view.findViewById(R.id.button_speak);
		 mSpeakButton.setOnClickListener(this);
		 mSpeechRecognizerEngine = SpeechRecognizerEngine.getInstance(getActivity());
		 SpeechRecognizerEngine.setResultText(mAskInput);
		 
		 mSaveBtn = (Button) view.findViewById(R.id.button_save);
		 mSaveBtn.setOnClickListener(this);

		 //预约时间
		 ((TextView) view.findViewById(R.id.yuyue_info_title)).setTextColor(getResources().getColor(R.color.light_green));
		 view.findViewById(R.id.yuyue_info_divider).setBackgroundResource(R.color.light_green);
		 mYuyueDate = (TextView) view.findViewById(R.id.date);
		 mYuyueTime = (TextView) view.findViewById(R.id.time);
		 //不需要自动填写预约时间
		 //mYuyueDate.setText(DateUtils.TOPIC_DATE_TIME_FORMAT.format(mCalendar.getTime()));
		 //mYuyueTime.setText(DateUtils.TOPIC_TIME_FORMAT.format(mCalendar.getTime()));
		 mYuyueDate.setOnClickListener(this);
		 mYuyueTime.setOnClickListener(this);
		 
		 view.findViewById(R.id.menu_choose).setOnClickListener(this);
		 populateBaoxiuInfoView();
		return view;
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
	}
	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		DebugUtils.logD(TAG, "onSaveInstanceState() save mBundle=" + mBundle);
		outState.putBundle(TAG, mBundle);
	}
	public boolean isEditable() {
		return mBaoxiuCardObject.mBID > 0;
	}
	private void populateBaoxiuInfoView() {
		//init layouts
		if (!isEditable()) {
			mTypeInput.getText().clear();
			mPinpaiInput.getText().clear();
			mModelInput.getText().clear();
			mBianhaoInput.getText().clear();
			mBaoxiuTelInput.getText().clear();
			mTagInput.getText().clear();
		} else {
			mTypeInput.setText(mBaoxiuCardObject.mLeiXin);
			mPinpaiInput.setText(mBaoxiuCardObject.mPinPai);
			mModelInput.setText(mBaoxiuCardObject.mXingHao);
			mBianhaoInput.setText(mBaoxiuCardObject.mSHBianHao);
			mBaoxiuTelInput.setText(mBaoxiuCardObject.mBXPhone);
			mTagInput.setText(mBaoxiuCardObject.mCardName);
		}
		populateHomeInfoView(HomeObject.getHomeObject(mBundle));
		populateContactInfoView(MyAccountManager.getInstance().getAccountObject().clone());
	}
	
	public void setBaoxiuObjectAfterSlideMenu(InfoInterface slideManuObject) {
		if (slideManuObject instanceof BaoxiuCardObject) {
			BaoxiuCardObject object = (BaoxiuCardObject) slideManuObject;
			if (!TextUtils.isEmpty(object.mLeiXin)) {
				mTypeInput.setText(object.mLeiXin);
			}
			if (!TextUtils.isEmpty(object.mPinPai)) {
				mPinpaiInput.setText(object.mPinPai);
			}
			
			if (!TextUtils.isEmpty(object.mXingHao)) {
				mModelInput.setText(object.mXingHao);
			}
			
			if (!TextUtils.isEmpty(object.mSHBianHao)) {
				mBianhaoInput.setText(object.mSHBianHao);
			}
			
			if (!TextUtils.isEmpty(object.mBXPhone)) {
				mBaoxiuTelInput.setText(object.mBXPhone);
			}
		}
	}
	
	public void populateHomeInfoView(HomeObject homeObject) {
		mProCityDisEditPopView.setHomeObject(homeObject.clone());
	}
	
    public void populateContactInfoView(AccountObject accountObject) {
    	if(accountObject == null) {
			mContactNameInput.getText().clear();
			mContactTelInput.getText().clear();
		} else {
			mContactNameInput.setText(accountObject.mAccountName);
			mContactTelInput.setText(accountObject.mAccountTel);
			
		}
	}


	
	public BaoxiuCardObject getBaoxiuCardObject() {
		mBaoxiuCardObject.mLeiXin = mTypeInput.getText().toString().trim();
		mBaoxiuCardObject.mPinPai = mPinpaiInput.getText().toString().trim();
		mBaoxiuCardObject.mXingHao = mModelInput.getText().toString().trim();
		mBaoxiuCardObject.mSHBianHao = mBianhaoInput.getText().toString().trim();
		mBaoxiuCardObject.mBXPhone = mBaoxiuTelInput.getText().toString().trim();
		mBaoxiuCardObject.mCardName = mTagInput.getText().toString().trim();
		
		return mBaoxiuCardObject;
	}
	
	public HomeObject getHomeObject() {
		return mProCityDisEditPopView.getHomeObject();
	}
	

	public AccountObject getContactInfoObject() {
		AccountObject contactInfoObject = new AccountObject();
		contactInfoObject.mAccountName = mContactNameInput.getText().toString().trim();
		contactInfoObject.mAccountTel = mContactTelInput.getText().toString().trim();
		return contactInfoObject;
	}

	@Override
	public void onClick(View v) {
		switch(v.getId()) {
		case R.id.button_scan_qrcode:
			startScan();
			break;
		case R.id.date:
			showDatePickerDialog();
			break;
		case R.id.time:
			showTimePickerDialog();
			break;
		case R.id.button_speak:
			mSpeechRecognizerEngine.showIatDialog(getActivity());
			break;
		case R.id.button_save:
			createCard();
			break;
		case R.id.menu_choose:
			//如果内容为空，我们显示侧边栏
			((NewCardActivity) getActivity()).getSlidingMenu().showMenu(true);
			break;
		}
		
	}

	private void createCard() {
		if(MyAccountManager.getInstance().hasLoginned()) {
			//如果没有注册，我们前往登陆界面
			if(checkInput()) {
				createCardAsync();
			}
		} else {
			//如果没有注册，我们前往登陆/注册界面，这里传递ModelBundle对象过去，以便做合适的跳转
			MyApplication.getInstance().showMessage(R.string.login_tip);
			LoginActivity.startIntent(this.getActivity(), getArguments());
		}
		
	}

	private CreateCardAsyncTask mCreateCardAsyncTask;
	private void createCardAsync(String... param) {
		AsyncTaskUtils.cancelTask(mCreateCardAsyncTask);
		showDialog(DIALOG_PROGRESS);
		mCreateCardAsyncTask = new CreateCardAsyncTask();
		mCreateCardAsyncTask.execute(param);
	}

	private class CreateCardAsyncTask extends AsyncTask<String, Void, Boolean> {
		private String mError;
		int mStatusCode = -1;
		String mStatusMessage = null;
		@Override
		protected Boolean doInBackground(String... params) {
			BaoxiuCardObject baoxiuCardObject = getBaoxiuCardObject();
			mError = null;
			InputStream is = null;
			final int LENGTH = 14;
			String[] urls = new String[LENGTH];
			String[] paths = new String[LENGTH];
			getBaoxiuCardObject();
			HomeObject homeObject = mProCityDisEditPopView.getHomeObject();
			urls[0] = HaierServiceObject.SERVICE_URL + "NAddHaierYY.ashx?LeiXin=";//AddHaierYuyue.ashx
			paths[0] = baoxiuCardObject.mLeiXin;
			urls[1] = "&PinPai=";
			paths[1] = baoxiuCardObject.mPinPai;
			urls[2] = "&XingHao=";
			paths[2] = baoxiuCardObject.mXingHao;
			urls[3] = "&SHBianhao=";
			paths[3] = baoxiuCardObject.mSHBianHao;
			urls[4] = "&BxPhone=";
			paths[4] = baoxiuCardObject.mBXPhone;
			urls[5] = "&UserName=";
			paths[5] = mContactNameInput.getText().toString().trim();
			urls[6] = "&Cell=";
			paths[6] = mContactTelInput.getText().toString().trim();
			urls[7] = "&address=";
			paths[7] = homeObject.mHomePlaceDetail;
			urls[8] = "&dstrictid=";
			paths[8] = mProCityDisEditPopView.getDisID();
			urls[9] = "&yytime=";
			paths[9] = BaoxiuCardObject.BUY_DATE_FORMAT_YUYUE_TIME.format(mCalendar.getTime());
			urls[10] = "&Desc=";
			paths[10] = mAskInput.getText().toString().trim();
			urls[11] = "&service_type=";
			paths[11] = "T15";//T15为保养
			
			String timeStr = BaoxiuCardObject.DATE_FORMAT_YUYUE_TIME.format(new Date());
			String tip = BaoxiuCardObject.getYuyueSecurityTip(timeStr);
			urls[12] = "&tip=";
			paths[12] = tip;
			urls[13] = "&key=";
			paths[13] = BaoxiuCardObject.getYuyueSecurityKey(mContactTelInput.getText().toString().trim(), timeStr);
			DebugUtils.logD(TAG, "urls = " + Arrays.toString(urls));
			DebugUtils.logD(TAG, "paths = " + Arrays.toString(paths));
			try {
				is = NetworkUtils.openContectionLocked(urls, paths, MyApplication.getInstance().getSecurityKeyValuesObject());
				try {
					JSONObject jsonObject = new JSONObject(NetworkUtils.getContentFromInput(is));
					mStatusCode = Integer.parseInt(jsonObject.getString("StatusCode"));
					mStatusMessage = jsonObject.getString("StatusMessage");
					DebugUtils.logD(TAG, "StatusCode = " + mStatusCode);
					DebugUtils.logD(TAG, "StatusMessage = " + mStatusMessage);
					if (mStatusCode == 1) {
						//操作成功
						return true;
					}
				} catch (JSONException e) {
					e.printStackTrace();
					mError = e.getMessage();
				}
			} catch (ClientProtocolException e) {
				e.printStackTrace();
				mError = e.getMessage();
			} catch (IOException e) {
				e.printStackTrace();
				mError = e.getMessage();
			} finally {
				NetworkUtils.closeInputStream(is);
			}
			return false;
		}

		@Override
		protected void onPostExecute(Boolean result) {
			super.onPostExecute(result);
			dissmissDialog(DIALOG_PROGRESS);
			if (mError != null) {
//				if (result) {
//					//服务器上传信息成功，但本地保存失败，请重新登录同步数据
//					new AlertDialog.Builder(getActivity())
//					.setTitle(R.string.msg_tip_title)
//		   			.setMessage(mError)
//		   			.setCancelable(false)
//		   			.setPositiveButton(R.string.button_ok, new DialogInterface.OnClickListener() {
//		   				@Override
//		   				public void onClick(DialogInterface dialog, int which) {
//		   					LoginActivity.startIntent(getActivity(), null);
//		   				}
//		   			})
//		   			.create()
//		   			.show();
//				} else {
//					MyApplication.getInstance().showMessage(mError);
//				}
				MyApplication.getInstance().showMessage(mError);
			} else if (result) {
				//预约成功
				getActivity().finish();
				MyApplication.getInstance().showMessage(R.string.msg_yuyue_sucess);
				if (MyAccountManager.getInstance().hasBaoxiuCards()) {
					MyChooseDevicesActivity.startIntent(getActivity(), getArguments());
				}
				
			} else {
				MyApplication.getInstance().showMessage(mStatusMessage);
			}
			
		}

		@Override
		protected void onCancelled() {
			super.onCancelled();
			dissmissDialog(DIALOG_PROGRESS);
		}
	}
	
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
		/*if(TextUtils.isEmpty(mBianhaoInput.getText().toString().trim())){
			showEmptyInputToast(R.string.product_sn);
			return false;
		}*/
		if(TextUtils.isEmpty(mBaoxiuTelInput.getText().toString().trim())){
			showEmptyInputToast(R.string.product_tel);
			return false;
		}
		

		if(TextUtils.isEmpty(mContactNameInput.getText().toString().trim())){
			showEmptyInputToast(R.string.name);
			return false;
		}
		if(TextUtils.isEmpty(mContactTelInput.getText().toString().trim())){
			showEmptyInputToast(R.string.usr_tel);
			return false;
		}
		
		if(TextUtils.isEmpty(mYuyueDate.getText().toString().trim())){
			showEmptyInputToast(R.string.date);
			return false;
		}
		if(TextUtils.isEmpty(mYuyueTime.getText().toString().trim())){
			showEmptyInputToast(R.string.time);
			return false;
		}
		if(TextUtils.isEmpty(mAskInput.getText().toString().trim())){
			showEmptyInputToast(R.string.error_des);
			return false;
		}

		if(!timeEscapeEnough()) {
			MyApplication.getInstance().showMessage(R.string.yuyue_time_too_early_tips);
			return false;
		}
		String pinpai = mPinpaiInput.getText().toString().trim();
		final String bxPhone = mBaoxiuTelInput.getText().toString().trim();
		//目前只有海尔支持预约安装和预约维修，如果不是，我们需要提示用户
    	if (!HaierServiceObject.isHaierPinpai(pinpai)) {
    		new AlertDialog.Builder(getActivity())
	    	.setMessage(R.string.must_haier_confirm_yuyue)
	    	.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
				
				@Override
				public void onClick(DialogInterface dialog, int which) {
					if (!TextUtils.isEmpty(bxPhone)) {
						Intents.callPhone(getActivity(), bxPhone);
					} else {
						MyApplication.getInstance().showMessage(R.string.msg_no_bxphone);
					}
					
				}
			})
			.setNegativeButton(android.R.string.cancel, null)
			.show();
		    return false;
    	} 
		return true;
	}

	private boolean timeEscapeEnough() {
		if((mCalendar.getTimeInMillis() - System.currentTimeMillis()) > 3 * 60 * 60 * 1000)
			return true;
		return false;
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
				//更新日期数据
//				mGoodsObject.mDate = mCalendar.getTimeInMillis();
				//更新UI
				mYuyueDate.setText(DateUtils.TOPIC_DATE_TIME_FORMAT.format(mCalendar.getTime()));
			}
				
		}, mCalendar.get(Calendar.YEAR), mCalendar.get(Calendar.MONTH), mCalendar.get(Calendar.DAY_OF_MONTH))
		.show();
	}

	Toast mToast;
	MyTimePickerDialog mMyTimePickerDialog;
	private void showTimePickerDialog() {
		mMyTimePickerDialog = new MyTimePickerDialog(getActivity(), new TimePickerDialog.OnTimeSetListener() {
			@Override
			public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
				mCalendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
				mCalendar.set(Calendar.MINUTE, minute);
				mYuyueTime.setText(DateUtils.TOPIC_TIME_FORMAT.format(mCalendar.getTime())
						+ "-" + DateUtils.TOPIC_TIME_FORMAT.format(new Date(mCalendar.getTimeInMillis() + 60 * 60 * 1000)));
			}
        	
        }, mCalendar.get(Calendar.HOUR_OF_DAY), 0, true);
		mMyTimePickerDialog.show();

		if(mCalendar.get(Calendar.HOUR_OF_DAY) < 8 || mCalendar.get(Calendar.HOUR_OF_DAY) > 19)
			mMyTimePickerDialog.getButton(TimePickerDialog.BUTTON_POSITIVE).setEnabled(false);

	}

	class MyTimePickerDialog extends TimePickerDialog {
		public MyTimePickerDialog(Context context,
				OnTimeSetListener callBack, int hourOfDay, int minute,
				boolean is24HourView) {
			super(context, callBack, hourOfDay, minute, is24HourView);
			if(hourOfDay < 8 || hourOfDay > 19) {
				if(mToast != null) {
					mToast.setText(R.string.select_time_out_of_service_tips);
				} else {					
					mToast = Toast.makeText(this.getContext(), R.string.select_time_out_of_service_tips, Toast.LENGTH_LONG);
				}
				mToast.show();
				//mMyTimePickerDialog.getButton(BUTTON_POSITIVE).setEnabled(false);
			}
		}

		@Override
		public void onTimeChanged(TimePicker view, int hourOfDay, int minute) {
			if(minute != 0) {
				if(mToast != null) {
					mToast.setText(R.string.select_clock_tips);
				} else {					
					mToast = Toast.makeText(this.getContext(), R.string.select_clock_tips, Toast.LENGTH_LONG);
				}
				mToast.show();
				mMyTimePickerDialog.getButton(BUTTON_POSITIVE).setEnabled(false);
				return;
			} else {
				mMyTimePickerDialog.getButton(BUTTON_POSITIVE).setEnabled(true);
			}
			if(hourOfDay < 8 || hourOfDay > 19) {
				if(mToast != null) {
					mToast.setText(R.string.select_time_out_of_service_tips);
				} else {					
					mToast = Toast.makeText(this.getContext(), R.string.select_time_out_of_service_tips, Toast.LENGTH_LONG);
				}
				mToast.show();
				mMyTimePickerDialog.getButton(BUTTON_POSITIVE).setEnabled(false);
			} else {
				mMyTimePickerDialog.getButton(BUTTON_POSITIVE).setEnabled(true);
			}
		}
	}

	@Override
    public void setScanObjectAfterScan(InfoInterface barCodeObject) {
		 BaoxiuCardObject object = (BaoxiuCardObject) barCodeObject;
		//这里一般我们只设置品牌、型号、编号和名称
		if (!TextUtils.isEmpty(object.mLeiXin)) {
			mTypeInput.setText(object.mLeiXin);
		}
		if (!TextUtils.isEmpty(object.mPinPai)) {
			mPinpaiInput.setText(object.mPinPai);
		}
		if (!TextUtils.isEmpty(object.mSHBianHao)) {
			mBianhaoInput.setText(object.mSHBianHao);
		}
		if (!TextUtils.isEmpty(object.mXingHao)) {
			mModelInput.setText(object.mXingHao);
		}
		if (!TextUtils.isEmpty(object.mBXPhone)) {
			mBaoxiuTelInput.setText(object.mBXPhone);
		}
	}
	
	@Override
	public InfoInterface getScanObjectAfterScan() {
		return BaoxiuCardObject.getBaoxiuCardObject();
	}
	@Override
	public void updateInfoInterface(InfoInterface infoInterface) {
	}

	@Override
	public void updateArguments(Bundle args) {
		mBundle = args;
		mBaoxiuCardObject.mAID = mBundle.getLong("aid", -1);
		mBaoxiuCardObject.mUID = mBundle.getLong("uid", -1);
	}
}
