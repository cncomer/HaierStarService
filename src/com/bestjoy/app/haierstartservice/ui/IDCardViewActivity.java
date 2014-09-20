package com.bestjoy.app.haierstartservice.ui;

import java.util.LinkedList;
import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.bestjoy.app.haierstartservice.MyApplication;
import com.bestjoy.app.haierstartservice.R;
import com.bestjoy.app.haierstartservice.account.AccountObject;
import com.bestjoy.app.haierstartservice.service.PhotoManagerUtilsV2;
import com.bestjoy.app.haierstartservice.service.PhotoManagerUtilsV2.TaskType;
import com.shwy.bestjoy.utils.Contents;
import com.shwy.bestjoy.utils.DebugUtils;
import com.shwy.bestjoy.utils.Intents;
import com.shwy.bestjoy.utils.QRGenerater;
import com.shwy.bestjoy.utils.QRGenerater.QRGeneratorFinishListener;
/**
 * 用于展示个人名片
 * @author bestjoy
 *
 */
public class IDCardViewActivity extends BaseNoActionBarActivity{

	private static final String TAG = "IDCardViewActivity";
	private ImageView mQrImage, mEditBtn, mAvator;
	private EditText mName, mTel, mOrg, mWorkplace, mPinpai;
	private TextView mTitle;
	private AccountObject mAccountObject;
	/**是否在编辑模式*/
	private boolean mIsInEditable = false;
	private List<EditText> mCheckList = new LinkedList<EditText>();
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (isFinishing()) {
			return;
		}
		setShowHomeUp(true);
		setContentView(R.layout.activity_idcard_view);
		PhotoManagerUtilsV2.getInstance().requestToken(TAG);
		initViews();
	}
	
	private void initViews() {
		mQrImage = (ImageView) findViewById(R.id.qrImage);
		mEditBtn = (ImageView) findViewById(R.id.button_edit);
		mAvator = (ImageView) findViewById(R.id.avator);
		mName = (EditText) findViewById(R.id.name);
		mTitle = (TextView) findViewById(R.id.title);
		mTel = (EditText) findViewById(R.id.tel);
		mOrg = (EditText) findViewById(R.id.org);
		mWorkplace = (EditText) findViewById(R.id.workplace);
		mPinpai = (EditText) findViewById(R.id.pinpai);
		PhotoManagerUtilsV2.getInstance().loadPhotoAsync(TAG, mAvator, mAccountObject.mAccountMm, null, TaskType.MYPREVIEW);
		
		mCheckList.add(mName);
		mCheckList.add(mTel);
		mCheckList.add(mOrg);
		mCheckList.add(mWorkplace);
		mCheckList.add(mPinpai);
		
		populateViews();
		resetEditMode();
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
		QRGenerater qRGenerater = new QRGenerater("http://c.mingdown.com/" + mAccountObject.mAccountMm);
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


}
