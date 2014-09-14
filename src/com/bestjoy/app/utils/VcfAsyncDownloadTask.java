package com.bestjoy.app.utils;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;

import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;

import com.bestjoy.app.haierstartservice.MyApplication;
import com.bestjoy.app.haierstartservice.R;
import com.bestjoy.app.haierstartservice.account.MyAccountManager;
import com.bestjoy.app.haierstartservice.service.PhotoManagerUtilsV2.TaskType;
import com.shwy.bestjoy.utils.Contents;
import com.shwy.bestjoy.utils.DebugUtils;
import com.shwy.bestjoy.utils.NetworkUtils;
import com.shwy.bestjoy.utils.NotifyRegistrant;

public class VcfAsyncDownloadTask extends AsyncTask<Void, Void, Boolean>{
	private static final String TAG = "VcfAsyncDownloadTask";
	/**下载失败*/
	public static final int EVENT_DOWNLOAD_FAILED =1000;
	/**下载成功*/
	public static final int EVENT_DOWNLOAD_SUCCESS =1001;
	/**保存失败*/
	public static final int EVENT_SAVE_FAILED =1003;
	/**该名号没有对应的名片*/
	public static final int EVENT_NOBCID =1004;
	/**下载开始, arg1表示当前已下载的字节， arg2表示下载文件的总字节数*/
	public static final int EVENT_PROGRESS_START =1005;
	
	/**下载取消*/
	public static final int EVENT_DOWNLOAD_CANCELED =1006;
	/**要下载的vcf文件已经存在了*/
	public static final int EVENT_EXIST_VCF = 1007;
	/**解析vcf失败失败*/
	public static final int EVENT_PARSE_ERROR =1008;
	/**下载开始*/
	public static final int EVENT_DOWNLOAD_START =1009;
	/**保存成功*/
	public static final int EVENT_SAVE_CONTACT_FINISH =1010;
	/**保存商家成功*/
	
	private boolean mIsCanceled = false;
	private boolean mIsNotifyProgress = false;
	private File mSavedFile;
	private static final int DOWNLOAD_CACHE_SIZE = 4096;// 下载缓存，大小为1024B=1KB
	
	private Handler mHandler;
	private String mMM, mUrl;
	/**当这个值为true的时候，即使本地已经存在vcf文件了，仍然会进行下载*/
	private boolean mIsForceUpdate = false;
	private boolean mIsNeedToSave = true;
	
	private TaskType mTaskType;
	/**是否记录某人下载了他人名片，默认是false*/
	private boolean mRecordDownload = false;
	
	public static final long OLD_FILE_TIMESTAMP = 1000 * 60 * 60 * 24 * 7; //10天，超过10天我们认为数据是旧的，需要更新了。
	
	/**
	 * @deprecated 1.62版本请使用{@link #VcfAsyncDownloadTask(String, boolean, Handler, TaskType, boolean)}
	 * @param mm
	 * @param notifyProgress
	 * @param handler
	 * @param taskType
	 */
	public VcfAsyncDownloadTask(String mm, boolean notifyProgress, Handler handler, TaskType taskType) {
		this(mm, notifyProgress, handler, taskType, false);
	}
	
	public VcfAsyncDownloadTask(String mm, boolean notifyProgress, Handler handler, TaskType taskType, boolean recordDownload) {
		mMM = mm;
		mIsNotifyProgress = notifyProgress;
		mSavedFile = getCachedCardFile(mm, taskType);
		mUrl = Contents.MingDang.buildDownloadUri(mm);
		mHandler = handler;
		mTaskType = taskType;
		mRecordDownload = recordDownload;
	}
	
	public void setForceDownload(boolean forceDownload) {
		mIsForceUpdate = forceDownload;
	}
	
//	public VcfAsyncDownloadTask(String mm, boolean notifyProgress, Handler handler, boolean save) {
//		mMM = mm;
//		mIsNotifyProgress = notifyProgress;
//		mUrl = Contents.MingDang.buildDownloadUri(mm);
//		mHandler = handler;
//		mIsNeedToSave = save;
//		if (mIsNeedToSave) {
//			mSavedFile = BJfileApp.getInstance().getCachedContactFile(mm);
//		} else {
//			mSavedFile = BJfileApp.getInstance().getCachedPreviewContactFile(mm);
//		}
//	}
	
	public static File getCachedCardFile(String mm, TaskType taskType) {
		switch(taskType) {
		case PREVIEW:
			return MyApplication.getInstance().getCachedPreviewContactFile(mm);
		}
		return null;
	}
	/**是否是预览他人名片*/
	public static boolean isPreview(TaskType taskType) {
		return taskType == TaskType.PREVIEW;
	}
	
	public VcfAsyncDownloadTask register(Handler handler) {
		DebugUtils.logContactAsyncDownload(TAG, "register handler");
		mHandler = handler;
		return this;
	}
	
	public void unregister(Handler handler) {
		if (mHandler == handler) {
			mHandler = null;
			DebugUtils.logContactAsyncDownload(TAG, "unregister handler");
		}
	}
	
	public boolean cancelTask(boolean mayInterruptIfRunning) {
		mIsCanceled = true;
		return super.cancel(true);
	}
	
	public static boolean isOldFile(File file) {
		
		return System.currentTimeMillis() - file.lastModified() >= OLD_FILE_TIMESTAMP;
	}
	
	@Override
	protected Boolean doInBackground(Void... param) {
		if (mHandler == null) {
			DebugUtils.logE(TAG, "download canceled, you must apply a Handler by calling register(Handler handler)");
			throw new RuntimeException("download canceled, you must apply a Handler by calling register(Handler handler)");
		}
		//通知下载开始
		notify(EVENT_DOWNLOAD_START,null);
		InputStream is = null;
		OutputStream out = null;
		try {
			DebugUtils.logContactAsyncDownload(TAG, "begin doInBackground()");
			
			if (mSavedFile.exists()) {
				DebugUtils.logContactAsyncDownload(TAG, "VCF exsited " + mSavedFile.getAbsolutePath());
				if (!mIsForceUpdate && !isPreview(mTaskType) || !mIsForceUpdate && !isOldFile(mSavedFile)) {
					//add by chenkai 20130603, 保存下载记录 begin
					if (mRecordDownload) {
						//普通名片
						RecordDownloadUtils.recordDownloadLocked(mMM, MyAccountManager.getInstance().getDefaultPhoneNumber());
					}
					//如果不是预览他人名片，那么我们总是直接使用存在的文件；或者是预览他人名片，我们判断是不是需要更新，如果需要，那么删除缓存重新下载
					notify(EVENT_EXIST_VCF, getDownloadedFile().getAbsolutePath());
					return false;
				} else {
					mSavedFile.delete();
					DebugUtils.logContactAsyncDownload(TAG, "force downloading, and delete exsied " + mSavedFile.getAbsolutePath());
				}
			}
			//add by chenkai 20130603, 保存下载记录 end
//			DebugUtils.logD(TAG, "HttpGet uri=" + mUrl);
//			HttpGet httpRequest = new HttpGet(mUrl);
//			HttpClient httpClient = AndroidHttpClient.newInstance("android");
//			HttpResponse response = httpClient.execute(httpRequest);
			HttpResponse response = NetworkUtils.openContectionLockedV2(mUrl, MyApplication.getInstance().getSecurityKeyValuesObject());
			int statusCode = response.getStatusLine().getStatusCode();
			DebugUtils.logD(TAG, "return HttpStatus is " + statusCode);
			if(!NetworkUtils.httpStatusOk(statusCode)) {
				notifyError(EVENT_DOWNLOAD_FAILED, MyApplication.getInstance().getString(R.string.msg_no_mm));
				return false;
			}
			if (mIsCanceled) {
				return false;
			}
			
			long totalSize = response.getEntity().getContentLength();
           is = response.getEntity().getContent();
			byte[] buffer = new byte[DOWNLOAD_CACHE_SIZE];
			int size = is.read(buffer);
			int currentSize = size;
			DebugUtils.logContactAsyncDownload(TAG, "need to download " + totalSize);
			DebugUtils.logContactAsyncDownload(TAG, "downloaded size " + currentSize);
			//add by chenki, 20131225, 没有登录，下载vcf文件bug修复 begin
			if (size == -1) {
				notifyError(EVENT_DOWNLOAD_FAILED, MyApplication.getInstance().getString(R.string.msg_read_no_data_when_download_vcf));
				return false;
			}
			//add by chenki, 20131225, 没有登录，下载vcf文件bug修复 end
			out = new BufferedOutputStream(new FileOutputStream(getDownloadedFile()));
			notifyProgress(currentSize, (int) totalSize);
			while (size >= 0) {
				if (mIsCanceled) {
					return false;
				}
				out.write(buffer, 0, size);
				size = is.read(buffer);
				currentSize +=size;
				notifyProgress(currentSize, (int) totalSize);
				DebugUtils.logContactAsyncDownload(TAG, "downloaded size " + currentSize);
				if (currentSize % 1024 == 0) {
					out.flush();
				}
			}
			out.flush();
			//add by chenkai 20130603, 保存下载记录 begin
			if (mRecordDownload) {
				RecordDownloadUtils.recordDownloadLocked(mMM, MyAccountManager.getInstance().getDefaultPhoneNumber());
			}
			return true;
		} catch (ClientProtocolException e) {
			e.printStackTrace();
			notifyError(EVENT_DOWNLOAD_FAILED, e.getMessage());
		} catch (IOException e) {
			e.printStackTrace();
			notifyError(EVENT_DOWNLOAD_FAILED, e.getMessage());
		} finally {
			try {
				if (out != null) out.close();
				if (is != null) is.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return false;
	}
	
	
	@Override
	protected void onPostExecute(Boolean result) {
		super.onPostExecute(result);
		if(result != null && result) {
			notify(EVENT_DOWNLOAD_SUCCESS, getDownloadedFile().getAbsolutePath());
		}
	}

	public File getDownloadedFile() {
		return mSavedFile;
	}
	
	public static boolean deleteDownloadedFile(String savedPath) {
		File file = new File(savedPath);
		DebugUtils.logContactAsyncDownload(TAG, "begin deleteDownloadedFile() " + file.getAbsolutePath());
		if (file.exists()) {
			DebugUtils.logContactAsyncDownload(TAG, "existed, delete " + file.getAbsolutePath());
			return file.delete();
		} else {
			DebugUtils.logContactAsyncDownload(TAG, "ignore delete, not existed " + file.getAbsolutePath());
		}
		return false;
	}
	
	public static String readDownloadedFile(String path) {
		DebugUtils.logContactAsyncDownload(TAG, "begin readDownloadedFile()");
		byte[] buffer = new byte[DOWNLOAD_CACHE_SIZE];
		FileInputStream fis;
		try {
			fis = new FileInputStream(path);
			int size = fis.read(buffer);
			
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			while (size >= 0) {
				out.write(buffer, 0, size);
				size = fis.read(buffer);
			}
			out.flush();
			buffer = out.toByteArray();
			out.close();

			return new String(buffer, "UTF-8");
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
		
	}
	
	private void notifyError(int what, String msg) {
		Message message = Message.obtain();
		message.what = what;
		message.obj = msg;
		NotifyRegistrant.getInstance().notify(mHandler, message);
	}
	
	private void notify(int what, String msg) {
		Message message = Message.obtain();
		message.what = what;
		message.obj = msg;
		NotifyRegistrant.getInstance().notify(mHandler, message);
	}
	
	
	private void notifyProgress(int current, int total) {
		if (!mIsNotifyProgress) return;
		Message message = Message.obtain();
		message.what = EVENT_PROGRESS_START;
		message.arg1 = current;
		message.arg2 = total;
		NotifyRegistrant.getInstance().notify(mHandler, message);
	}

	@Override
	protected void onPreExecute() {
		super.onPreExecute();
	}

	@Override
	protected void onCancelled() {
		super.onCancelled();
		DebugUtils.logContactAsyncDownload(TAG, "download cancel by user");
		File savedFile= getDownloadedFile();
		if (savedFile.exists()) {
			savedFile.delete();
			DebugUtils.logContactAsyncDownload(TAG, "downloaded file hase existed, delete it " + savedFile.getAbsolutePath());
		}
		NotifyRegistrant.getInstance().notify(mHandler, EVENT_DOWNLOAD_CANCELED);
	}
}
