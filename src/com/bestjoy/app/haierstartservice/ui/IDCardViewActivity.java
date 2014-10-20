package com.bestjoy.app.haierstartservice.ui;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;

import org.apache.http.client.ClientProtocolException;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;

import com.bestjoy.app.haierstartservice.HaierServiceObject;
import com.bestjoy.app.haierstartservice.HaierServiceObject.HaierResultObject;
import com.bestjoy.app.haierstartservice.MyApplication;
import com.bestjoy.app.haierstartservice.R;
import com.bestjoy.app.haierstartservice.account.AccountObject;
import com.bestjoy.app.haierstartservice.account.MyAccountManager;
import com.bestjoy.app.haierstartservice.database.BjnoteContent;
import com.bestjoy.app.haierstartservice.database.HaierDBHelper;
import com.bestjoy.app.haierstartservice.service.PhotoManagerUtilsV2;
import com.bestjoy.app.haierstartservice.service.PhotoManagerUtilsV2.TaskType;
import com.shwy.bestjoy.utils.AsyncTaskUtils;
import com.shwy.bestjoy.utils.ComConnectivityManager;
import com.shwy.bestjoy.utils.Contents;
import com.shwy.bestjoy.utils.DebugUtils;
import com.shwy.bestjoy.utils.ImageHelper;
import com.shwy.bestjoy.utils.Intents;
import com.shwy.bestjoy.utils.NetworkUtils;
import com.shwy.bestjoy.utils.QRGenerater;
import com.shwy.bestjoy.utils.QRGenerater.QRGeneratorFinishListener;
/**
 * 用于展示个人名片
 * @author bestjoy
 *
 */
public class IDCardViewActivity extends BaseNoActionBarActivity implements View.OnClickListener{

	private static final String TAG = "IDCardViewActivity";
	private static final int mAvatorWidth = 96, mAvatorHeight = 96;
	private ImageView mQrImage, mEditBtn, mAvator;
	private EditText mName, mTel, mOrg, mWorkplace, mPinpai;
	private EditText mTitle;
	private AccountObject mAccountObject;
	
	private View mButtonsLayout;
	/**是否在编辑模式*/
	private boolean mIsInEditable = false;
	private List<EditText> mCheckList = new LinkedList<EditText>();
	
	private File mAvatorFile;
	private Uri mAvatorUri;
	private boolean mNeedUpdateAvatorFromCamera = false;
	private boolean mNeedUpdateAvatorFromGallery = false;
	
	private Handler mHandler;
	private static final int REQUEST_UPDATE = 1;
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (isFinishing()) {
			return;
		}
		setShowHomeUp(true);
		File tempRootDir = MyApplication.getInstance().getExternalStorageAccountRoot(String.valueOf(mAccountObject.mAccountUid));
		if (tempRootDir != null) {
			mAvatorFile = new File(tempRootDir, ".avatorTemp");
		}
		
		mHandler = new Handler() {

			@Override
			public void handleMessage(Message msg) {
				super.handleMessage(msg);
				DebugUtils.logD(TAG, "handleMessage() loadFapiaoFromCameraAsync");
				updateAvatorAsync();
			}
			
		};
		
		setContentView(R.layout.activity_idcard_view);
		PhotoManagerUtilsV2.getInstance().requestToken(TAG);
		initViews();
		
		if (savedInstanceState != null) {
			mNeedUpdateAvatorFromCamera = savedInstanceState.getBoolean("mNeedUpdateAvatorFromCamera");
			mNeedUpdateAvatorFromGallery = savedInstanceState.getBoolean("mNeedUpdateAvatorFromGallery");
			DebugUtils.logD(TAG, "onCreate savedInstanceState != null, get mNeedUpdateAvatorFromCamera " + mNeedUpdateAvatorFromCamera + ", mNeedUpdateAvatorFromGallery " + mNeedUpdateAvatorFromGallery);
		}
	}
	
	private void initViews() {
		mQrImage = (ImageView) findViewById(R.id.qrImage);
		mEditBtn = (ImageView) findViewById(R.id.button_edit);
		mAvator = (ImageView) findViewById(R.id.avator);
		mAvator.setOnClickListener(this);
		mName = (EditText) findViewById(R.id.name);
		mTitle = (EditText) findViewById(R.id.title);
		mTel = (EditText) findViewById(R.id.tel);
		mOrg = (EditText) findViewById(R.id.org);
		mWorkplace = (EditText) findViewById(R.id.workplace);
		mPinpai = (EditText) findViewById(R.id.pinpai);
		
		mButtonsLayout = findViewById(R.id.buttons);
		
		findViewById(R.id.button_save).setOnClickListener(this);
		findViewById(R.id.button_cancel).setOnClickListener(this);
		
		mEditBtn = (ImageView) findViewById(R.id.button_edit);
		mEditBtn.setOnClickListener(this);
		PhotoManagerUtilsV2.getInstance().loadPhotoAsync(TAG, mAvator, mAccountObject.mAccountMm, null, TaskType.MYPREVIEW);
		
		mCheckList.add(mName);
		mCheckList.add(mTitle);
		mCheckList.add(mTel);
		mCheckList.add(mOrg);
		mCheckList.add(mWorkplace);
		mCheckList.add(mPinpai);
		
		populateViews();
		resetEditMode();
		
		mCheckList.remove(mPinpai);
	}
	
	@Override
	public void onResume() {
		super.onResume();
		DebugUtils.logD(TAG, "onResume() mNeedUpdateAvatorFromCamera=" + mNeedUpdateAvatorFromCamera + ", mNeedUpdateAvatorFromGallery=" + mNeedUpdateAvatorFromGallery);
		if (mNeedUpdateAvatorFromCamera || mNeedUpdateAvatorFromGallery) {
			DebugUtils.logD(TAG, "onResume() removeMessages REQUEST_BILL, sendEmptyMessage REQUEST_BILL");
			mHandler.removeMessages(REQUEST_UPDATE);
			mHandler.sendEmptyMessageDelayed(REQUEST_UPDATE, 500);
		}
		
	}
	
	private void resetEditMode() {
		for (EditText editText:mCheckList) {
			editText.setFocusable(mIsInEditable);
			editText.setFocusableInTouchMode(mIsInEditable);
			editText.setCursorVisible(mIsInEditable);
		}
		
	}
	
	private void populateViews() {
		mName.setText(mAccountObject.mAccountName);
		mTitle.setText(mAccountObject.mAccountTitle);
		mTel.setText(mAccountObject.mAccountTel);
		mOrg.setText(mAccountObject.mAccountOrg);
		mWorkplace.setText(mAccountObject.mAccountWorkaddress);
		mPinpai.setText("Haier");
		
		
		//"http://c.mingdown.com/mm"
//		String format = QRGenerater.getContentFormat(QRGenerater.MECARD_CONTENT_FORMAT);
		String format = "MECARD:URL:%s;;";
		QRGenerater qRGenerater = new QRGenerater(String.format(format, Contents.MingDang.buildDirectCloudUri(mAccountObject.mAccountMm)));
		int size = getResources().getDimensionPixelSize(R.dimen.mypreview_qr_image_view_size);
		qRGenerater.setDimens(size, size);
		qRGenerater.setQRGeneratorFinishListener(new QRGeneratorFinishListener() {

			@Override
			public void onQRGeneratorFinish(final Bitmap bitmap) {
				if (isFinishing()) {
					DebugUtils.logD(TAG, "onQRGeneratorFinish Activity.isFinishing(), so we just return");
					return;
				}
				if (bitmap != null) {
					MyApplication.getInstance().postAsync(new Runnable(){

						@Override
						public void run() {
							mQrImage.setImageBitmap(bitmap);
						}
						
					});
				}
			}
			
		});
		qRGenerater.start();
		
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		PhotoManagerUtilsV2.getInstance().releaseToken(TAG);
	}
	
	@Override
	protected boolean checkIntent(Intent intent) {
		long uid = intent.getLongExtra(Intents.EXTRA_ID, -1);
		mAccountObject = AccountObject.getHaierAccountFromDatabase(this, uid);
		return mAccountObject != null;
	}
	
	public static void startActivity(Context context, long uid) {
		Intent intent = new Intent(context, IDCardViewActivity.class);
		intent.putExtra(Intents.EXTRA_ID, uid);
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		context.startActivity(intent);
	}

	@Override
	public void onClick(View v) {
		switch(v.getId()) {
		case R.id.button_save:
			if (!ComConnectivityManager.getInstance().isConnected()) {
				ComConnectivityManager.getInstance().onCreateNoNetworkDialog(mContext).show();
				return;
			}
			if (checkInput()) {
				saveCardAsync();
			}
			break;
		case R.id.button_edit:
			mIsInEditable = true;
			resetEditMode();
			mButtonsLayout.setVisibility(View.VISIBLE);
			mEditBtn.setVisibility(View.GONE);
			break;
		case R.id.button_cancel:
			mIsInEditable = false;
			resetEditMode();
			mButtonsLayout.setVisibility(View.GONE);
			mEditBtn.setVisibility(View.VISIBLE);
			break;
		case R.id.avator:
			showDialog(DIALOG_PICTURE_CHOOSE_CONFIRM);
			break;
		}
		
	}
	
	private boolean checkInput() {
		mAccountObject.mAccountName  = mName.getText().toString().trim();
		if (TextUtils.isEmpty(mAccountObject.mAccountName)) {
			MyApplication.getInstance().showMessage(R.string.usr_name_hint);
			return false;
		}
		mAccountObject.mAccountTel  = mTel.getText().toString().trim();
		if (TextUtils.isEmpty(mAccountObject.mAccountTel)) {
			MyApplication.getInstance().showMessage(R.string.usr_tel_hint);
			return false;
		}
		mAccountObject.mAccountTitle  = mTitle.getText().toString().trim();
		if (TextUtils.isEmpty(mAccountObject.mAccountTitle)) {
			MyApplication.getInstance().showMessage(R.string.usr_title_hint);
			return false;
		}
		mAccountObject.mAccountOrg  = mOrg.getText().toString().trim();
		if (TextUtils.isEmpty(mAccountObject.mAccountOrg)) {
			MyApplication.getInstance().showMessage(R.string.usr_org_hint);
			return false;
		}
		mAccountObject.mAccountWorkaddress  = mWorkplace.getText().toString().trim();
		if (TextUtils.isEmpty(mAccountObject.mAccountWorkaddress)) {
			MyApplication.getInstance().showMessage(R.string.usr_workplace_hint);
			return false;
		}
		return true;
	}
	
	private SaveCardAsyncTask mSaveCardAsyncTask;
	private void saveCardAsync() {
		AsyncTaskUtils.cancelTask(mSaveCardAsyncTask);
		mSaveCardAsyncTask = new SaveCardAsyncTask();
		mSaveCardAsyncTask.execute();
		showDialog(DIALOG_PROGRESS);
	}
	
	private class SaveCardAsyncTask extends AsyncTask<Void, Void, HaierResultObject> {

		@Override
		protected HaierResultObject doInBackground(Void... params) {
			HaierResultObject haierResultObject = new HaierResultObject();
			InputStream is = null;
			
			try {
				JSONObject queryObject = new JSONObject();
				queryObject.put("username", mAccountObject.mAccountName);
				queryObject.put("cell", mAccountObject.mAccountTel);
				queryObject.put("shopname", mAccountObject.mAccountOrg);
				queryObject.put("address", mAccountObject.mAccountWorkaddress);
				queryObject.put("mm", mAccountObject.mAccountMm);
				queryObject.put("title", mAccountObject.mAccountTitle);
				
				is = NetworkUtils.openContectionLocked(HaierServiceObject.getUpdateVcfUrlByMM("para", queryObject.toString()), MyApplication.getInstance().getSecurityKeyValuesObject());
				if (is != null) {
					haierResultObject = HaierResultObject.parse(NetworkUtils.getContentFromInput(is));
					
					if (haierResultObject.isOpSuccessfully()) {
						ContentValues values = new ContentValues();
						values.put(HaierDBHelper.ACCOUNT_NAME, mAccountObject.mAccountName);
						values.put(HaierDBHelper.ACCOUNT_TEL, mAccountObject.mAccountTel);
						
						values.put(HaierDBHelper.ACCOUNT_TITLE, mAccountObject.mAccountTitle);
						values.put(HaierDBHelper.ACCOUNT_ORG, mAccountObject.mAccountOrg);
						values.put(HaierDBHelper.ACCOUNT_WORKADDRESS, mAccountObject.mAccountWorkaddress);
						int updated = BjnoteContent.update(getContentResolver(), BjnoteContent.Accounts.CONTENT_URI, values, BjnoteContent.ID_SELECTION, new String[]{String.valueOf(mAccountObject.mAccountId)});
						DebugUtils.logD(TAG, "SaveCardAsyncTask updated " + (updated > 0));
						if (updated > 0) {
							//更新账户信息
							MyAccountManager.getInstance().updateAccount();
						}
					}
				}
			} catch (JSONException e) {
				e.printStackTrace();
				haierResultObject.mStatusMessage = e.getMessage();
			} catch (ClientProtocolException e) {
				e.printStackTrace();
				haierResultObject.mStatusMessage = e.getMessage();
			} catch (IOException e) {
				e.printStackTrace();
				haierResultObject.mStatusMessage = e.getMessage();
			}
			
//			HaierResultObject result  = HaierResultObject.parse(content);
			return haierResultObject;
		}

		@Override
		protected void onPostExecute(HaierResultObject result) {
			super.onPostExecute(result);
			dismissDialog(DIALOG_PROGRESS);
			if (result.isOpSuccessfully()) {
				mIsInEditable = false;
				mButtonsLayout.setVisibility(View.GONE);
				mEditBtn.setVisibility(View.VISIBLE);
				resetEditMode();
			} 
			MyApplication.getInstance().showMessage(result.mStatusMessage);
		}

		@Override
		protected void onCancelled() {
			super.onCancelled();
			dismissDialog(DIALOG_PROGRESS);
		}
		
		
		
	}
	
	@Override
	public void onPickFromGalleryStart() {
		mAvatorUri = null;
		mNeedUpdateAvatorFromGallery = false;
		pickFromGallery();
	}
	
	@Override
	public void onPickFromCameraStart() {
		if (mAvatorFile != null && mAvatorFile.exists()) {
			mAvatorFile.delete();
		}
		mNeedUpdateAvatorFromCamera = false;
		pickFromCamera(mAvatorFile);
	}
	
	@Override
	public void onPickFromGalleryFinish(Uri data) {
		DebugUtils.logD(TAG, "onPickFromGalleryFinish() mNeedUpdateAvatorFromGallery " + mNeedUpdateAvatorFromGallery + ", mAvatorUri " + data);
		if (data != null) {
			mAvatorUri = data;
			mNeedUpdateAvatorFromGallery = true;
		}
	}
	
	@Override
	public void onPickFromCameraFinish() {
		DebugUtils.logD(TAG, "onPickFromCameraFinish() mNeedUpdateAvatorFromCamera " + mNeedUpdateAvatorFromCamera + ", mAvatorFile " + mAvatorFile.getAbsolutePath());
		if (mAvatorFile.exists()) {
			mNeedUpdateAvatorFromCamera = true;
		}
	}
	
	
	
	
	private UpdateAvatorTask mUpdateAvatorTask;
	private void updateAvatorAsync() {
		AsyncTaskUtils.cancelTask(mUpdateAvatorTask);
		showDialog(DIALOG_PROGRESS);
		mUpdateAvatorTask = new UpdateAvatorTask();
		mUpdateAvatorTask.execute();
	}
	private class UpdateAvatorTask extends AsyncTask<Void, Void, HaierResultObject> {

		private Bitmap _newBitmap;
		@Override
		protected HaierResultObject doInBackground(Void... arg0) {
			HaierResultObject haierResultObject = new HaierResultObject();
			InputStream is = null;
			try {
				if (mNeedUpdateAvatorFromCamera) {
					_newBitmap = ImageHelper.getSmallBitmap(mAvatorFile.getAbsolutePath(), mAvatorWidth, mAvatorHeight);
					mNeedUpdateAvatorFromCamera = false;
				} else if (mNeedUpdateAvatorFromGallery) {
					mNeedUpdateAvatorFromGallery = false;
					BitmapFactory.Options options = new BitmapFactory.Options();
					options.inJustDecodeBounds = true;
					_newBitmap = BitmapFactory.decodeStream(getContentResolver().openInputStream(mAvatorUri), null, options); 
					
					options.inSampleSize = ImageHelper.calculateInSampleSize(options, mAvatorWidth, mAvatorWidth);
					// Decode bitmap with inSampleSize set
				    options.inJustDecodeBounds = false;
				    
				    _newBitmap = BitmapFactory.decodeStream(getContentResolver().openInputStream(mAvatorUri), null, options); 
				}
				JSONObject queryObject = new JSONObject();
				queryObject.put("mm", mAccountObject.mAccountMm);
				queryObject.put("imgstr", ImageHelper.bitmapToString(_newBitmap, 100));
				is = NetworkUtils.openPostContectionLocked(HaierServiceObject.getPostUpdateAvatorUrlByMM(), "para", queryObject.toString(), MyApplication.getInstance().getSecurityKeyValuesObject());
				if (is != null) {
					haierResultObject = HaierResultObject.parse(NetworkUtils.getContentFromInput(is));
				}
			} catch (JSONException e) {
				e.printStackTrace();
				haierResultObject.mStatusMessage = e.getMessage();
			} catch (FileNotFoundException e) {
				e.printStackTrace();
				haierResultObject.mStatusMessage = e.getMessage();
			} catch (ClientProtocolException e) {
				e.printStackTrace();
				haierResultObject.mStatusMessage = e.getMessage();
			} catch (IOException e) {
				e.printStackTrace();
				haierResultObject.mStatusMessage = e.getMessage();
			}
			return haierResultObject;
		}

		@Override
		protected void onPostExecute(HaierResultObject result) {
			super.onPostExecute(result);
			dismissDialog(DIALOG_PROGRESS);
			if (result.isOpSuccessfully()) {
				mAvator.setImageBitmap(_newBitmap);
				//更新成功，重新下载MM头像
				File old = MyApplication.getInstance().getAccountCardAvatorFile(mAccountObject.mAccountMm);
				if (old.exists()) {
					old.delete();
				}
				PhotoManagerUtilsV2.removeBitmapFromCache(mAccountObject.mAccountMm);
				PhotoManagerUtilsV2.getInstance().loadPhotoAsync(TAG, mAvator, mAccountObject.mAccountMm, null, TaskType.MYPREVIEW);
			}
			MyApplication.getInstance().showMessage(result.mStatusMessage);
		}

		@Override
		protected void onCancelled() {
			super.onCancelled();
			dismissDialog(DIALOG_PROGRESS);
		}
		
	}
	
	
	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putBoolean("mNeedUpdateAvatorFromCamera", mNeedUpdateAvatorFromCamera);
		outState.putBoolean("mNeedUpdateAvatorFromGallery", mNeedUpdateAvatorFromGallery);
		DebugUtils.logD(TAG, "onSaveInstanceState() save mNeedUpdateAvatorFromCamera " + mNeedUpdateAvatorFromCamera + ", mNeedUpdateAvatorFromGallery " + mNeedUpdateAvatorFromGallery);
	}


}
