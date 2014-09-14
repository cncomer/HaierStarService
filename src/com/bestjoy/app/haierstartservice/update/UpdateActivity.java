package com.bestjoy.app.haierstartservice.update;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.bestjoy.app.haierstartservice.MyApplication;
import com.bestjoy.app.haierstartservice.R;
import com.bestjoy.app.haierstartservice.ui.BaseActionbarActivity;
import com.shwy.bestjoy.utils.AsyncTaskUtils;
import com.shwy.bestjoy.utils.ComConnectivityManager;
import com.shwy.bestjoy.utils.DebugUtils;
import com.shwy.bestjoy.utils.Intents;
import com.shwy.bestjoy.utils.NetworkUtils;

public class UpdateActivity extends BaseActionbarActivity{
	private static final String TAG = "UpdateActivity";
	private static final String EXTRA_APPINFO = "extra_appinfo";
	
	private File mApkFile;
	private ServiceAppInfo mServiceAppInfo;
	
	private enum TYPE {
		IDLE,
		DOWNLOADING,
		SUCCESS
	};
	private TYPE mCurrentType;
	private TextView mReleaseNote, mProgressStatus;
	private ProgressBar mProgressBar;
	private View mProgressLayout;
	
    private DownloadAsynTask mDownloadAsynTask;
    
    private static final int DIALOG_DOWNLOAD_UNFINISH_ON_EXIT = 1;
    private static final int DIALOG_DOWNLOAD_UNFINISH = 2;
    
    private WakeLock mWakeLock;
    
    private boolean mDownloadCancelWaitForUser = false;
    private Object mWaitObject = new Object();
	    
	@Override
	public void onCreate(Bundle bundle) {
		super.onCreate(bundle);
		DebugUtils.logD(TAG, "onCreate");
		if (isFinishing()) {
			DebugUtils.logD(TAG, "has been finishing");
			return;
		}
		setContentView(R.layout.update_activity);
		initView();
		
		mCurrentType = TYPE.IDLE;
		
		PowerManager pm = (PowerManager) this.getSystemService(Context.POWER_SERVICE);
		mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK | PowerManager.ON_AFTER_RELEASE, TAG);
	} 
	
	@Override
	public void onResume() {
		super.onResume();
		//如果SD不能使用，弹出对话框
		if (!MyApplication.getInstance().hasExternalStorage()) {
			showDialog(DIALOG_MEDIA_UNMOUNTED);
		} else {
			mApkFile = mServiceAppInfo.buildExternalDownloadAppFile();
			if (mApkFile.exists()) {
				mCurrentType = TYPE.SUCCESS;
			}
			checkCurrentType();
		}
		
	}
	
	@Override
	public void onNewIntent(Intent newIntent) {
		DebugUtils.logLife(TAG, "onNewIntent " + newIntent);
		if (newIntent != null) {
			setIntent(newIntent);
			mServiceAppInfo = newIntent.getParcelableExtra(EXTRA_APPINFO);
			DebugUtils.logLife(TAG, "onNewIntent mServiceAppInfo " + mServiceAppInfo.toString(this));
			initView();
		}
	}
	
	private void initView() {
		if (mReleaseNote == null) {
			mReleaseNote = (TextView) findViewById(R.id.releasenote);
			
			mProgressLayout = findViewById(R.id.progress_layout);
			mProgressBar = (ProgressBar) findViewById(R.id.progressBar);
			mProgressStatus = (TextView) findViewById(R.id.status_view);
		}
		mReleaseNote.setText(mServiceAppInfo.buildReleasenote(mContext));
		setTitle(getString(R.string.format_latest_version, mServiceAppInfo.mVersionName));
	}
	
	/**
	 * 根据当前的按钮类型，更新相应的UI
	 */
	private void checkCurrentType() {
		switch(mCurrentType) {
		case IDLE:
			mProgressLayout.setVisibility(View.GONE);
			break;
		case DOWNLOADING:
			mProgressLayout.setVisibility(View.VISIBLE);
			updateProgress(0);
			break;
		case SUCCESS:
			mProgressLayout.setVisibility(View.GONE);
			break;
		}
		invalidateOptionsMenu();
	}
	
	 @Override
     public boolean onCreateOptionsMenu(Menu menu) {
		 getSupportMenuInflater().inflate(R.menu.update_activity_menu, menu);
		 return true;
     }

	 @Override
     public boolean onPrepareOptionsMenu(Menu menu) {
		 switch(mCurrentType) {
			case IDLE:
				menu.findItem(R.string.button_update).setVisible(true);
				menu.findItem(R.string.button_cancel).setVisible(false);
				menu.findItem(R.string.button_install).setVisible(false);
				break;
			case DOWNLOADING:
				menu.findItem(R.string.button_update).setVisible(false);
				menu.findItem(R.string.button_cancel).setVisible(true);
				menu.findItem(R.string.button_install).setVisible(false);
				break;
			case SUCCESS:
				menu.findItem(R.string.button_update).setVisible(false);
				menu.findItem(R.string.button_cancel).setVisible(false);
				menu.findItem(R.string.button_install).setVisible(true);
				break;
			}
		 
		 return true;
     }
	 
	 @Override
     public boolean onOptionsItemSelected(MenuItem item) {
		 switch(item.getItemId()) {
		 case R.string.button_update:
			//check network to download
			if (!ComConnectivityManager.getInstance().isConnected()) {
				showDialog(DIALOG_DATA_NOT_CONNECTED);
			} else {
				mCurrentType = TYPE.DOWNLOADING;
				checkCurrentType();
				downloadAsync();
			}
			 break;
		 case R.string.button_cancel:
			//取消正下下载
			mDownloadCancelWaitForUser = true;
			showDialog(DIALOG_DOWNLOAD_UNFINISH);
			 break;
		 case R.string.button_install:
			 Intents.install(mContext, mApkFile);
			 finish();
			 break;
		 case android.R.id.home:
			 onBackPressed();
			 break;
		 }
		 return true;
	 }
	 
	 
	 
	/***
	 * 更新进度条区域的显示
	 * @param progress
	 */
	private void updateProgress(int progress) {
		mProgressBar.setProgress(progress);
		mProgressStatus.setText(getString(R.string.status_downloading, progress));
	}
	
	@Override
	public void onBackPressed() {
		if (mCurrentType == TYPE.DOWNLOADING) {
			mDownloadCancelWaitForUser = true;
			showDialog(DIALOG_DOWNLOAD_UNFINISH_ON_EXIT);
			return;
		}
		super.onBackPressed();
	}
	
	private void downloadAsync() {
		AsyncTaskUtils.cancelTask(mDownloadAsynTask);
		mDownloadAsynTask = new DownloadAsynTask();
		mDownloadAsynTask.execute();
	}
	
	private class DownloadAsynTask extends AsyncTask<Void, Integer, Boolean> {

		@Override
		protected Boolean doInBackground(Void... params) {
			InputStream is = null;
			long count = 0;
			long total = 0;
			mWakeLock.acquire();
			try {
				HttpResponse response = NetworkUtils.openContectionLockedV2(mServiceAppInfo.mApkUrl, MyApplication.getInstance().getSecurityKeyValuesObject());
				if(response.getStatusLine().getStatusCode() != 200) {
					throw new IOException("StatusCode!=200");
				}
                HttpEntity entity = response.getEntity();  
                total = entity.getContentLength();
                is = entity.getContent();
				if (is != null) {
					 FileOutputStream fileOutputStream = new FileOutputStream(mApkFile);  
	                 byte[] buf = new byte[4096];  
	                 int ch = -1;
	                 while ((ch = is.read(buf)) != -1) {
	                	 while (mDownloadCancelWaitForUser) {
	                		 synchronized(mWaitObject) {
	                			 try {
									mWaitObject.wait();
								} catch (InterruptedException e) {
									e.printStackTrace();
								}
	                		 }
	                	 }
	                	 if (isCancelled()) {
	                		 fileOutputStream.flush();
	                		 fileOutputStream.close();
	                		 return false;
	                	 }
	                 	count += ch;
	                    fileOutputStream.write(buf, 0, ch);
	                    if (count % 4096 == 0) {
	                    	fileOutputStream.flush();
	                    }
	                    publishProgress((int) (count * 100 / total));
	                 } 
	                 if(count == total) {
	                	 publishProgress(100);
	                 }
	                 fileOutputStream.flush();
	                 fileOutputStream.close();
	                 return true;
	             } 
			} catch (ClientProtocolException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				NetworkUtils.closeInputStream(is);
			}
			return false;
		}

		@Override
		protected void onProgressUpdate(Integer... values) {
			super.onProgressUpdate(values);
			if (isCancelled()) {
				return;
       	    }
			updateProgress(values[0]);
		}

		@Override
		protected void onPostExecute(Boolean result) {
			super.onPostExecute(result);
			if (isCancelled()) {
				return;
       	    }
			DebugUtils.logD(TAG, "DownloadAsynTask onPostExecute return " + result);
			if (result) {
				mCurrentType = TYPE.SUCCESS;
				Intents.install(mContext, mApkFile);
			} else {
				MyApplication.getInstance().showMessage(R.string.app_update_error);
				mCurrentType = TYPE.IDLE;
				if (mApkFile.exists()) {
					DebugUtils.logD(TAG, "DownloadAsynTask onPostExecute delete existed apk " + mApkFile.getAbsolutePath());
					mApkFile.delete();
				}
			}
			checkCurrentType();
			mWakeLock.release();
		}

		@Override
		protected void onCancelled() {
			super.onCancelled();
			mCurrentType = TYPE.IDLE;
			DebugUtils.logD(TAG, "DownloadAsynTask onCancelled");
			if (mApkFile.exists()) {
				DebugUtils.logD(TAG, "DownloadAsynTask onCancelled delete existed apk " + mApkFile.getAbsolutePath());
				mApkFile.delete();
			}
			checkCurrentType();
			mWakeLock.release();
		}
		
		
    }
	
	@Override
	public Dialog onCreateDialog(int id) {
		switch(id) {
		case DIALOG_DOWNLOAD_UNFINISH_ON_EXIT:
			return new AlertDialog.Builder(mContext)
			.setMessage(R.string.dialog_msg_download_unfinish_on_exit)
			.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
				
				@Override
				public void onClick(DialogInterface dialog, int which) {
					AsyncTaskUtils.cancelTask(mDownloadAsynTask);
					mDownloadCancelWaitForUser = false;
					synchronized(mWaitObject) {
						mWaitObject.notify();
					}
					finish();
				}
			})
			.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
				
				@Override
				public void onClick(DialogInterface dialog, int which) {
					mDownloadCancelWaitForUser = false;
					synchronized(mWaitObject) {
						mWaitObject.notify();
					}
				}
			})
			.create();
		case DIALOG_DOWNLOAD_UNFINISH:
			return new AlertDialog.Builder(mContext)
			.setMessage(R.string.dialog_msg_download_unfinish)
			.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
				
				@Override
				public void onClick(DialogInterface dialog, int which) {
					AsyncTaskUtils.cancelTask(mDownloadAsynTask);
					mDownloadCancelWaitForUser = false;
					synchronized(mWaitObject) {
						mWaitObject.notify();
					}
				}
			})
			.setNegativeButton(android.R.string.cancel,new DialogInterface.OnClickListener() {
				
				@Override
				public void onClick(DialogInterface dialog, int which) {
					mDownloadCancelWaitForUser = false;
					synchronized(mWaitObject) {
						mWaitObject.notify();
					}
				}
			})
			.create();
		}
		return super.onCreateDialog(id);
	}
	
	protected void onMediaUnmountedConfirmClick() {
		DebugUtils.logD(TAG, "onMediaUnmountedConfirmClick()");
		finish();
	}
	@Override
	public void onDestroy() {
		super.onDestroy();
		DebugUtils.logD(TAG, "onDestroy");
		AsyncTaskUtils.cancelTask(mDownloadAsynTask);
	}
	
	public static Intent createIntent(Context context, ServiceAppInfo appInfo) {
		Intent intent = new Intent(context, UpdateActivity.class);
		intent.putExtra(EXTRA_APPINFO, appInfo);
		return intent;
	}

	@Override
	protected boolean checkIntent(Intent intent) {
		if (intent != null) {
			mServiceAppInfo = intent.getParcelableExtra(EXTRA_APPINFO);
		}
		return mServiceAppInfo != null;
	}


}
