package com.bestjoy.app.haierstartservice.update;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;

import android.app.PendingIntent.CanceledException;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.util.Log;

import com.bestjoy.app.haierstartservice.MyApplication;
import com.bestjoy.app.haierstartservice.database.BjnoteContent;
import com.bestjoy.app.haierstartservice.database.DeviceDBHelper;
import com.bestjoy.app.haierstartservice.ui.PreferencesActivity;
import com.bestjoy.app.utils.YouMengMessageHelper;
import com.shwy.bestjoy.utils.ComConnectivityManager;
import com.shwy.bestjoy.utils.DateUtils;
import com.shwy.bestjoy.utils.DebugUtils;
import com.shwy.bestjoy.utils.Intents;
import com.shwy.bestjoy.utils.NetworkUtils;

/**
 * 
 * @author chenkai
 *
 */
public class UpdateService extends Service implements ComConnectivityManager.ConnCallback{
	private static String TAG = "UpdateService";
	private static final boolean DEBUG = false;

	/**强制检查更新*/
	public static final String ACTION_UPDATE_CHECK_FORCE = MyApplication.PKG_NAME + ".intent.ACTION_UPDATE_CHECK_FORCE";
	/**开始检查更新*/
	public static final String ACTION_UPDATE_CHECK = MyApplication.PKG_NAME + ".intent.ACTION_UPDATE_CHECK";
	/**用户强制立即检查更新*/
	public static final String ACTION_UPDATE_CHECK_FORCE_BY_USER = MyApplication.PKG_NAME + ".intent.ACTION_UPDATE_CHECK_FORCE_BY_USER";
	/**自动检查开始了*/
	public static final String ACTION_UPDATE_CHECK_AUTO = MyApplication.PKG_NAME + ".intent.ACTION_UPDATE_CHECK_AUTO";
	/**开始下载*/
	public static final String ACTION_DOWNLOAD_START = MyApplication.PKG_NAME + ".intent.ACTION_DOWNLOAD_START";
	/**结束下载*/
	public static final String ACTION_DOWNLOAD_END = MyApplication.PKG_NAME + ".intent.ACTION_DOWNLOAD_END";
	/**下载进度*/
	public static final String ACTION_DOWNLOAD_PROGRESS = MyApplication.PKG_NAME + ".intent.ACTION_DOWNLOAD_PROGRESS";
	/**没有网络*/
	public static final String ACTION_UNAVAILABLE_NETWORK = MyApplication.PKG_NAME + ".intent.ACTION_UNAVAILABLE_NETWORK";
	private Handler mWorkServiceHandler, mHandler;
	private static final int MSG_CHECK_UPDATE = 1000;
	/**开始下载*/
	private static final int MSG_DOWNLOAD_START = 1001;
	/**结束下载*/
	private static final int MSG_DOWNLOAD_END = 1002;
	
	private static final long UPDATE_DURATION_PER_HOUR = 1 * 60 * 60 * 1000; //1小时检查一次
	private static final long UPDATE_DURATION_PER_DAY = 24 * 60 * 60 * 1000; //1天检查一次
	private static final long UPDATE_DURATION_PER_WEEK = 7 * 24 * 60 * 60 * 1000; //7天检查一次
	
	//表示自动更新检查是否正在运行
	private boolean mIsCheckUpdateRuinning = false;
	/**表示服务是否正在运行*/
	private boolean mIsServiceRuinning = false;
	
	public static final String ACTION_CHECK_DEVICE_TOKEN = MyApplication.PKG_NAME + ".intent.ACTION_CHECK_DEVICE_TOKEN";
	private static final int MSG_CHECK_DEVICE_TOKEN = 1003;
	
	
	/**下载结束广播。当接到该广播的时候，我们解析字段Intents.EXTRA_RESULT， false表示下载取消了，true表示下载完成*/
	private Intent mDownloadEndIntent;
	private Intent mNoNetworkIntent, mDownloadStartIntent, mDownloadProgressIntent;
	
	private static UpdateService mInstance;
	
	public static enum TYPE {
		IDLE,
		DOWNLOADING,
		SUCCESS
	};
	private TYPE mCurrentType;
	
	private ServiceAppInfo mServiceAppInfo, mDatabaseServiceAppInfo;
	
	@Override
	public void onCreate() {
		super.onCreate();
		Log.v(TAG, "onCreate");
		mNoNetworkIntent = new Intent(ACTION_UNAVAILABLE_NETWORK);
		mDownloadStartIntent = new Intent(ACTION_DOWNLOAD_START);
		mDownloadEndIntent = new Intent(ACTION_DOWNLOAD_END);
		mDownloadProgressIntent = new Intent(ACTION_DOWNLOAD_PROGRESS);
		
		mIsServiceRuinning = true;
		
		mServiceAppInfo = new ServiceAppInfo(MyApplication.PKG_NAME);
		mDatabaseServiceAppInfo = new ServiceAppInfo(MyApplication.PKG_NAME + ".db");
		
		mHandler = new Handler() {
			@Override
			public void handleMessage(Message msg) {
				super.handleMessage(msg);
			}
		};
		HandlerThread workThread = new HandlerThread("UpdateWorkService", Process.THREAD_PRIORITY_BACKGROUND);
		workThread.start();
		Looper looper = workThread.getLooper();
		mWorkServiceHandler = new Handler(looper) {

			@Override
			public void handleMessage(Message msg) {
				switch(msg.what) {
				case MSG_CHECK_UPDATE:
					if(checkUpdate()){
						//检查app
						DebugUtils.logD(TAG, "checkUpdate and start UpdateActivity for app");
						Intent intent = UpdateActivity.createIntent(UpdateService.this, mServiceAppInfo);
						intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
						startActivity(intent);
					} else if (checkDeviceDatabaseUpdate()) {
						//检查数据库
						updateDeviceDatabase();
					}
					break;
				case MSG_DOWNLOAD_START:
					downloadLocked(mServiceAppInfo.buildExternalDownloadAppFile());
					break;
				case MSG_CHECK_DEVICE_TOKEN:
					if (!YouMengMessageHelper.getInstance().getDeviceTotkeStatus()) {
						YouMengMessageHelper.getInstance().postDeviceTokenToServiceLocked();
						DebugUtils.logD(TAG, "sendEmptyMessageDelayed(MSG_CHECK_DEVICE_TOKEN, 30000");
						mWorkServiceHandler.sendEmptyMessageDelayed(MSG_CHECK_DEVICE_TOKEN, 30000);
						
					}
					break;
				}
				super.handleMessage(msg);
			}
		};
		//当网络状态改变的时候我们需要判断今天是否已经进行过版本检查，如果进行过，那么wifi可用的时候再次进行一次检查
		ComConnectivityManager.getInstance().addConnCallback(this);
		mInstance = this;
	}
	
	public static UpdateService getUpdateService() {
		return mInstance;
	}
	
	
	@Override
	public void onStart(Intent intent, int startId) {
		super.onStart(intent, startId);
		Log.v(TAG, "onStart from intent " + intent);
		if (intent != null) {
			if (ComConnectivityManager.getInstance().isConnected()) {
				String action = intent.getAction();
				onServiceIntent(action);
			} else {
				sendBroadcast(mNoNetworkIntent);
			}
			
		}
	}
	
	private void onServiceIntent(String action) {
		if (ACTION_UPDATE_CHECK.equals(action)
				|| Intent.ACTION_BOOT_COMPLETED.equals(action)
				|| Intent.ACTION_USER_PRESENT.equals(action)
				|| ACTION_UPDATE_CHECK_FORCE.equals(action)) {
			long currentTime = System.currentTimeMillis();
			DebugUtils.logD(TAG, "onServiceIntent currentTime" + DateUtils.TOPIC_SUBJECT_DATE_TIME_FORMAT.format(new Date(currentTime)));
			long lastUpdateCheckTime = ServiceAppInfo.getLatestCheckTime();
			DebugUtils.logD(TAG, "onServiceIntent lastUpdateCheckTime" + DateUtils.TOPIC_SUBJECT_DATE_TIME_FORMAT.format(new Date(lastUpdateCheckTime)));
			boolean needCheckUpdate = false;
			//正常Wifi情况下，我们每一天都会检查一次是否有更新
			if (ComConnectivityManager.getInstance().isWifiConnected()) {
				if (Intent.ACTION_USER_PRESENT.equals(action)) {
					needCheckUpdate = currentTime - lastUpdateCheckTime > UPDATE_DURATION_PER_HOUR;
				} else {
					needCheckUpdate = currentTime - lastUpdateCheckTime > UPDATE_DURATION_PER_DAY;
				}
			} else if (ComConnectivityManager.getInstance().isMobileConnected()) {
				needCheckUpdate = currentTime - lastUpdateCheckTime > UPDATE_DURATION_PER_WEEK;
			} else {
				DebugUtils.logD(TAG, "connectivity is not connected.");
				return;
			}
			
			if (ACTION_UPDATE_CHECK_FORCE.equals(action)) {
				DebugUtils.logD(TAG, "force updating....");
				needCheckUpdate = true;
			}
			
			if (!DEBUG && !needCheckUpdate) {
				DebugUtils.logD(TAG, "need not updating check, time is not enough long");
				return;
			}
			if (mWorkServiceHandler.hasMessages(MSG_CHECK_UPDATE)) {
				DebugUtils.logD(TAG, "mWorkServiceHandler is running checkupdate, so just ignore");
				return;
			}
			mWorkServiceHandler.sendEmptyMessage(MSG_CHECK_UPDATE);
		} else if (ACTION_DOWNLOAD_START.equals(action)) {
			//开始下载，如果下载任务正在进行了，那么
			synchronized(mDownloadTaskLocked) {
				if (!mIsDownloadTaskRunning) {
					mWorkServiceHandler.sendEmptyMessage(MSG_DOWNLOAD_START);
				} else {
					DebugUtils.logD(TAG, "Download task is running, so we just ignore");
				}
			}
		} else if (ACTION_DOWNLOAD_END.equals(action)) {
			synchronized(mDownloadTaskLocked) {
				if (mIsDownloadTaskRunning) {
					mIsDownloadTaskRunning = false;
				}
			}
		} else if (ACTION_CHECK_DEVICE_TOKEN.equals(action)) {
			DebugUtils.logD(TAG, "sendEmptyMessage(MSG_CHECK_DEVICE_TOKEN)");
			mWorkServiceHandler.sendEmptyMessage(MSG_CHECK_DEVICE_TOKEN);
		}
	}


	//判断是否需要更新
	private boolean checkUpdate(){
		DebugUtils.logD(TAG, "start update checking......." + mServiceAppInfo.mToken);
		mIsCheckUpdateRuinning = true;
		boolean needUpdate = false;
		if (mServiceAppInfo.getServiceAppInfoLocked()) {
			SharedPreferences prefs = MyApplication.getInstance().mPreferManager;
			int currentVersion = prefs.getInt(PreferencesActivity.KEY_LATEST_VERSION, 0);
			DebugUtils.logD(TAG, "updateCheckTime = " + DateUtils.TOPIC_SUBJECT_DATE_TIME_FORMAT.format(new Date(mServiceAppInfo.mCheckTime)));
			DebugUtils.logD(TAG, "currentVersionCode = " + currentVersion);
			DebugUtils.logD(TAG, "newVersionCode = " + mServiceAppInfo.mVersionCode);
			mIsCheckUpdateRuinning = false;
			needUpdate = mServiceAppInfo.mVersionCode > currentVersion;
		}
		mServiceAppInfo.save();
		DebugUtils.logD(TAG, "end update app checking......." + mServiceAppInfo.mToken);
		return needUpdate;
	}
	
	//判断是否需要更新
		private boolean checkDeviceDatabaseUpdate(){
			DebugUtils.logD(TAG, "start update checking......." + mDatabaseServiceAppInfo.mToken);
			mIsCheckUpdateRuinning = true;
			boolean needUpdate = false;
			if (mDatabaseServiceAppInfo.getServiceAppInfoLocked()) {
				int currentVersion = DeviceDBHelper.getDeviceDatabaseVersion();
				DebugUtils.logD(TAG, "updateCheckTime = " + DateUtils.TOPIC_SUBJECT_DATE_TIME_FORMAT.format(new Date(mServiceAppInfo.mCheckTime)));
				DebugUtils.logD(TAG, "currentVersionCode = " + currentVersion);
				DebugUtils.logD(TAG, "newVersionCode = " + mDatabaseServiceAppInfo.mVersionCode);
				mIsCheckUpdateRuinning = false;
				needUpdate = mDatabaseServiceAppInfo.mVersionCode > currentVersion;
			}
			DebugUtils.logD(TAG, "end update app checking......." + mDatabaseServiceAppInfo.mToken);
			return needUpdate;
		}

	@Override
	public void onDestroy() {
		super.onDestroy();
		mIsServiceRuinning = false;
		ComConnectivityManager.getInstance().removeConnCallback(this);
		Log.v(TAG, "onDestroy");
	}


	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}
	
	
	@Override
	public void onConnChanged(ComConnectivityManager cm) {
		 onServiceIntent(ACTION_UPDATE_CHECK);
	}
	
	
	private boolean mIsDownloadTaskRunning = false;
	private Object mDownloadTaskLocked = new Object();
	
	private void downloadLocked(File file) {
		synchronized(mDownloadTaskLocked) {
			mIsDownloadTaskRunning =  true;
		}
		InputStream is = null;
		long count = 0;
		long total = 0;
		try {
			HttpResponse response = NetworkUtils.openContectionLockedV2(mServiceAppInfo.mApkUrl, MyApplication.getInstance().getSecurityKeyValuesObject());
			if(response.getStatusLine().getStatusCode() != 200) {
				throw new IOException("StatusCode!=200");
			}
            HttpEntity entity = response.getEntity();  
            total = entity.getContentLength();
            is = entity.getContent();
			if (is != null) {
				 FileOutputStream fileOutputStream = new FileOutputStream(file);  
                 byte[] buf = new byte[4096];  
                 int ch = -1;
                 while ((ch = is.read(buf)) != -1) {
                	 if (!mIsDownloadTaskRunning) {
                		 fileOutputStream.flush();
                		 fileOutputStream.close();
                		 mDownloadEndIntent.putExtra(Intents.EXTRA_RESULT, false);
                		 DebugUtils.logD(TAG, "sendBroadcast for downloadTask is cancelde");
                		 sendBroadcast(mDownloadEndIntent);
                		 throw new CanceledException("Download task is canceled");
                	 }
                 	count += ch;
                    fileOutputStream.write(buf, 0, ch);
                    if (count % 4096 == 0) {
                    	fileOutputStream.flush();
                    }
                    publishProgress((int) (count * 100 / total), total);
                 } 
                 if(count == total) {
                	 publishProgress(100, total);
                 }
                 fileOutputStream.flush();
                 fileOutputStream.close();
                 mDownloadEndIntent.putExtra(Intents.EXTRA_RESULT, true);
        		 DebugUtils.logD(TAG, "sendBroadcast for downloadTask is finished");
        		 sendBroadcast(mDownloadEndIntent);
             } 
		} catch (ClientProtocolException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (CanceledException e) {
			e.printStackTrace();
		} finally {
			NetworkUtils.closeInputStream(is);
			synchronized(mDownloadTaskLocked) {
				mIsDownloadTaskRunning =  false;
			}
		}
	}
	
	private void publishProgress(int progress, long size) {
		DebugUtils.logD(TAG, "sendBroadcast for downloadTask is updating progress " + progress + ", size is " + size);
		mDownloadProgressIntent.putExtra(Intents.EXTRA_PROGRESS, progress);
		mDownloadProgressIntent.putExtra(Intents.EXTRA_PROGRESS_MAX, size);
		sendBroadcast(mDownloadProgressIntent);
	}
	
	private void updateDeviceDatabase() {
		DebugUtils.logD(TAG, "enter updateDeviceDatabase()");
		InputStream is = null;
		OutputStream out = null;
		try {
			DebugUtils.logD(TAG, "start download " + mDatabaseServiceAppInfo.mApkUrl);
	        is = NetworkUtils.openContectionLocked(mDatabaseServiceAppInfo.mApkUrl, MyApplication.getInstance().getSecurityKeyValuesObject());
	        if (is != null) {
	        	out = new FileOutputStream(mDatabaseServiceAppInfo.buildLocalDownloadAppFile());
	        	byte[] buf = new byte[4096];
                int ch = -1;
                while ((ch = is.read(buf)) != -1) {
                	out.write(buf, 0, ch);
               	 }
                out.flush();
	        }
	        NetworkUtils.closeOutStream(out);
	        NetworkUtils.closeInputStream(is);
	        DebugUtils.logD(TAG, "save to " + mDatabaseServiceAppInfo.buildLocalDownloadAppFile().getAbsolutePath());
	        mDatabaseServiceAppInfo.save();
	        BjnoteContent.CloseDeviceDatabase.closeDeviceDatabase(getContentResolver());
	        DebugUtils.logD(TAG, "restart " + getPackageName());
	        Intent i = getPackageManager().getLaunchIntentForPackage(getPackageName());  
	        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
	        startActivity(i);
        } catch (ClientProtocolException e) {
	        e.printStackTrace();
        } catch (IOException e) {
	        e.printStackTrace();
        }
	}
	
	/**
	 * 开始下载任务，需要提供要下载的apk的版本号，如果已经有正在下载的任务，
	 * @param context
	 * @param downloadedVersion
	 */
	public static void startDownloadTask(Context context, String downloadedVersionCode) {
		Intent intent = new Intent(context, UpdateService.class);
		intent.setAction(ACTION_DOWNLOAD_START);
		intent.putExtra(Intents.EXTRA_ID, downloadedVersionCode);
		context.startService(intent);
	}
	
	public static void startUpdateServiceOnAppLaunch(Context context) {
		Intent service = new Intent(context, UpdateService.class);
		service.setAction(ACTION_UPDATE_CHECK);
		context.startService(service);
	}
	public static void startUpdateServiceOnBootCompleted(Context context) {
		Intent service = new Intent(context, UpdateService.class);
		service.setAction(Intent.ACTION_BOOT_COMPLETED);
		context.startService(service);
	}
	public static void startUpdateServiceOnUserPresent(Context context) {
		Intent service = new Intent(context, UpdateService.class);
		service.setAction(Intent.ACTION_USER_PRESENT);
		context.startService(service);
	}
	public static void startUpdateServiceForce(Context context) {
		Intent service = new Intent(context, UpdateService.class);
		service.setAction(ACTION_UPDATE_CHECK_FORCE);
		context.startService(service);
	}
	public static void startCheckDeviceTokenToService(Context context) {
		Intent service = new Intent(context, UpdateService.class);
		service.setAction(ACTION_CHECK_DEVICE_TOKEN);
		context.startService(service);
	}
}
