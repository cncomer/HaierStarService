package com.shwy.bestjoy.contacts;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.preference.PreferenceManager;
import android.view.WindowManager;

import com.bestjoy.app.haierstartservice.R;
import com.bestjoy.app.haierstartservice.service.PhotoManagerUtilsV2.TaskType;
import com.bestjoy.app.haierstartservice.ui.PreferencesActivity;
import com.bestjoy.app.utils.BeepAndVibrate;
import com.bestjoy.app.utils.VcfAsyncDownloadTask;
import com.bestjoy.app.utils.VcfAsyncDownloadUtils;
import com.bestjoy.app.utils.VcfAsyncDownloadUtils.VcfAsyncDownloadHandler;
import com.google.zxing.client.android.camera.CameraManager;
import com.google.zxing.client.result.AddressBookParsedResult;
import com.shwy.bestjoy.utils.Contents;
import com.shwy.bestjoy.utils.DebugUtils;

public class AddrBookUtils {
	private static final String TAG = "AddrBookUtils";
	private static AddrBookUtils INSTANCE = new AddrBookUtils();
	private Context mContext;
	
	private VcfAsyncDownloadHandler mVcfAsyncDownloadHandler, mVcfAsyncDownloadAndViewHandler;
	private VcfAsyncDownloadTask mVcfAsyncDownloadTask;
	private ProgressDialog mDownloadDialog;
	
	private AddrBookUtils() {
		mVcfAsyncDownloadHandler = new VcfAsyncDownloadHandler() {

			@Override
			public void onDownloadStart() {
				if (mDownloadDialog != null) mDownloadDialog.show();
			}
			@Override
			public boolean onDownloadFinishedInterrupted() {
				return false;
			}
			@Override
			public void onDownloadFinished(
					AddressBookParsedResult addressBookParsedResult,
					String outMsg) {
				super.onDownloadFinished(addressBookParsedResult, outMsg);
				if (mDownloadDialog != null) mDownloadDialog.dismiss();
			}
			
			@Override
			public boolean onSaveFinished(Uri contactUri, String mm) {
				if (mDownloadDialog != null) mDownloadDialog.dismiss();
				if (mm.endsWith(Contents.MingDang.FLAG_MERCHANT)) {
					return false;
				}
				return true;
			}
    	
    	};
    	
    	mVcfAsyncDownloadAndViewHandler = new VcfAsyncDownloadHandler() {
    		
			@Override
			public void onDownloadStart() {
				if (mDownloadDialog != null) mDownloadDialog.show();
			}
			@Override
			public boolean onDownloadFinishedInterrupted() {
				return false;
			}
			@Override
			public void onDownloadFinished(
					AddressBookParsedResult addressBookParsedResult,
					String outMsg) {
				super.onDownloadFinished(addressBookParsedResult, outMsg);
				if (mDownloadDialog != null) mDownloadDialog.dismiss();
			}

			@Override
			public boolean onSaveFinished(Uri contactUri, String mm) {
				if (mDownloadDialog != null) mDownloadDialog.dismiss();
				return false;
			}
			
			
    	};
	}
	
	public static AddrBookUtils getInstance() {
		return INSTANCE;
	}
	
	public void setContext(Context context) {
		mContext = context;
		if (mDownloadDialog == null) {
			mDownloadDialog = new ProgressDialog(context);
			mDownloadDialog.setMessage(mContext.getString(R.string.msg_downloading_text));
			mDownloadDialog.setCancelable(true);
			mDownloadDialog.setIndeterminate(true);
			mDownloadDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
				
				@Override
				public void onCancel(DialogInterface dialog) {
					VcfAsyncDownloadUtils.cancel(mVcfAsyncDownloadTask, true);
				}
			});
			
			mDownloadDialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
		}
		
	}

	public final Uri createContactEntry(AddressBookParsedResult addressResult) {
		
		if (Integer.parseInt(Build.VERSION.SDK) >= CameraManager.VERSION_CODES_LEVEL) {//CameraManager.VERSION_CODES_LEVEL
			AddrBookAddManager addrBookAddManager = new AddrBookAddManager(mContext, BeepAndVibrate.getInstance());
			return addrBookAddManager.createContactEntry(addressResult);
		} else {
			AddrBookAddManagerEarly addrBookAddManager = new AddrBookAddManagerEarly(mContext, BeepAndVibrate.getInstance());
			return addrBookAddManager.createContactEntry(addressResult);
		}
	}
	/**
	 * 创建商家联系人
	 * @param addressResult
	 * @return
	 */
    public final long createMerchantContactEntryLocked(AddressBookParsedResult addressResult) {
		return -1;
		
	}
	
    public final Cursor queryContactEntry(String phoneNumber) {
		
		if (Integer.parseInt(Build.VERSION.SDK) >= CameraManager.VERSION_CODES_LEVEL) {//CameraManager.VERSION_CODES_LEVEL
			return AddrBookAddManager.queryContactEntry(mContext, phoneNumber);
		} else {
			return AddrBookAddManagerEarly.queryContactEntry(mContext, phoneNumber);
		}
	}
	
	/**
	 * 创建单个联系人条目信息，添加到通讯录中 注意：使用新版本的ContactsContractAPIs取代ContactsAPIs
	 * 
	 * @param names
	 *            名字列表 String[]
	 * @param phoneNumbers
	 *            电话列表 String[]
	 * @param emails
	 *            邮件列表 String[]
	 * @param note
	 *            联系人备注
	 * @param address
	 *            联系人地址
	 * @param org
	 *            联系人组织
	 * @param title
	 *            联系人职位
	 * @param photo
	 *            联系人头像，二进制数据
	 */
	public static final AddressBookParsedResult getAddressBookParsedResult(String[] names, String[] phoneNumbers,
			String[] emails, String note, String[] address, String org,
			String title, String[] url, byte[] photo, String bid) {
		return new AddressBookParsedResult(
				names, 
				null, 
				phoneNumbers, 
				emails, 
				note, 
				address, 
				org, 
				null, 
				title, 
				url, 
				photo, 
				bid, 
				null, 
				null);
	}
	
//	public void downloadContactLock(String mm) {
//		VcfAsyncDownloadUtils.cancel(mVcfAsyncDownloadTask, true);
//		mVcfAsyncDownloadTask = VcfAsyncDownloadUtils.getInstance().executeTask(mm, false, mVcfAsyncDownloadHandler, TaskType.PREVIEW, false, true);
//	}
	
	public void downloadAndViewContactLock(String mm, boolean recordDownload) {
		VcfAsyncDownloadUtils.cancel(mVcfAsyncDownloadTask, true);
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
		boolean viewContact = prefs.getBoolean(PreferencesActivity.KEY_AUTO_REDIRECT, true);
		if (!viewContact) {
			mVcfAsyncDownloadTask = VcfAsyncDownloadUtils.getInstance().executeTask(mm, false, mVcfAsyncDownloadHandler, TaskType.PREVIEW, false, recordDownload);
		} else {
			mVcfAsyncDownloadTask = VcfAsyncDownloadUtils.getInstance().executeTask(mm, false, mVcfAsyncDownloadAndViewHandler, TaskType.PREVIEW, false, recordDownload);
		}
	}
	/***
	 * 该方法需要提供VcfAsyncDownloadHandler,并且如果需要Dialog,需要在VcfAsyncDownloadHandler回调中实现，可参考{@link #downloadAndViewContactLock(String)}
	 * @param mm
	 * @param handler
	 */
	public void downloadContactLock(String mm, VcfAsyncDownloadHandler handler, boolean recordDownload) {
		VcfAsyncDownloadUtils.cancel(mVcfAsyncDownloadTask, true);
		mVcfAsyncDownloadTask = VcfAsyncDownloadUtils.getInstance().executeTask(mm, false, handler, TaskType.PREVIEW, false, recordDownload);
		
	}
	
	public void viewContact(Uri uri) {
		if (uri != null) {
//			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
//			if(prefs.getBoolean(PreferencesActivity.KEY_AUTO_REDIRECT, true)) {
//				if (DebugLogger.DEBUG_ADD_CONTACT) Log.v(TAG, "viewContact " + uri);
//	    		Intent intent = new Intent();
//	    		intent.setAction(Intent.ACTION_VIEW);
//	    		//显示当前联系人信息
//	    		intent.setData(uri);
//	    		context.startActivity(intent);
//	    	}
			DebugUtils.logD(TAG, "viewContact " + uri);
    		Intent intent = new Intent();
    		intent.setAction(Intent.ACTION_VIEW);
    		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    		//显示当前联系人信息
    		intent.setData(uri);
    		mContext.startActivity(intent);
		}
	}
}
