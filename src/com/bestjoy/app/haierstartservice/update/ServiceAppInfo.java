package com.bestjoy.app.haierstartservice.update;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import org.apache.http.client.ClientProtocolException;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

import com.bestjoy.app.haierstartservice.MyApplication;
import com.bestjoy.app.haierstartservice.R;
import com.shwy.bestjoy.utils.DebugUtils;
import com.shwy.bestjoy.utils.NetworkUtils;

/**
 * version 版本号  从1开始
 * date 日期  
 * importance 重要程度  重要程度分为0和1，0表示强制更新, 1表示普通，其他的暂时不支持
 * size 更新大小 
 * apk 更新apk的地址，如果是default表示的是默认路径http://www.bjnote.com/down4/bjnote.apk
 * note 更新说明 如果是http开头的，会以网页的形式显示
 * @author chenkai
 *
 */
public class ServiceAppInfo implements Parcelable{
	 /**最近一次执行过自动检测的时间*/
    public static final String KEY_SERVICE_APP_INFO_CHECK_TIME = "service_app_info_timestamp";
    public static final String KEY_SERVICE_APP_INFO_VERSION_CODE = "service_app_info_version_code";
    public static final String KEY_SERVICE_APP_INFO_VERSION_NAME = "service_app_info_version_name";
    public static final String KEY_SERVICE_APP_INFO_RELEASENOTE = "service_app_info_releasenote";
    public static final String KEY_SERVICE_APP_INFO_APK_URL = "service_app_info_apk_url";
    public static final String KEY_SERVICE_APP_INFO_APK_SIZE = "service_app_info_apk_sizel";
    public static final String KEY_SERVICE_APP_INFO_RELEASEDATE = "service_app_info_releasedate";
    public static final String KEY_SERVICE_APP_INFO_IMPORTANCE = "service_app_info_importance";
  
    
	public static final String DEFAULT_UPDATE_FILE_URL="http://www.mingdown.com/mobile/getVersion.ashx?app=";
	public static final String KEY_VERSION_CODE = "version";
	public static final String KEY_VERSION_NAME = "versionCodeName";
	public static final String KEY_DATE = "date";
	public static final String KEY_IMPORTANCE = "importance";
	public static final String KEY_SIZE = "size";
	public static final String KEY_APK = "apk";
	public static final String KEY_NOTE = "note";
	
	public static final String KEY_TIME = "check_time";
	
	public static final int IMPORTANCE_MUST = 0;
	public static final int IMPORTANCE_OPTIONAL = 1;
	
	
	public int mVersionCode = 0;
	public String mReleaseDate ;
	/**更新补丁的重要程度，如果是0表示必须更新并安装，否则无法继续使用；默认是1表示可选择安装更新也可以不选择。*/
	public int mImportance = IMPORTANCE_OPTIONAL;
	public String mSizeStr;
	public String mApkUrl;
	public String mReleaseNote;
	
	public String mVersionName = "";
	
	public long mCheckTime;
	
	public String mToken;
	private SharedPreferences mPreferences;
	public ServiceAppInfo(String token) {
		mToken = token;
		init();
	}
	
	public ServiceAppInfo() {
		//默认是app信息
		mToken = MyApplication.PKG_NAME;
		init();
	}

	private void init() {
		mPreferences = MyApplication.getInstance().getSharedPreferences(mToken, Context.MODE_PRIVATE);
		read();
	}
	
	public boolean getServiceAppInfoLocked() {
		InputStream is = null;
		try {
			is = NetworkUtils.openContectionLocked(getServiceUrl(), MyApplication.getInstance().getSecurityKeyValuesObject());
			if (is != null) {
				String content = NetworkUtils.getContentFromInput(is);
				if (!TextUtils.isEmpty(content)) {
					JSONObject json = new JSONObject(content);
					mVersionCode = json.getInt(KEY_VERSION_CODE);
					mReleaseDate = json.getString(KEY_DATE);
					mImportance = json.getInt(KEY_IMPORTANCE);
					mSizeStr = json.getString(KEY_SIZE);
					mApkUrl = json.getString(KEY_APK);
					mReleaseNote = json.getString(KEY_NOTE);
					mVersionName = json.optString(KEY_VERSION_NAME, String.valueOf(mVersionCode));
					return true;
				}
			}
		} catch (ClientProtocolException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (JSONException e) {
			e.printStackTrace();
		} finally {
			NetworkUtils.closeInputStream(is);
			mCheckTime = System.currentTimeMillis();
		}
		return false;
	}
	
	private String getServiceUrl() {
		StringBuilder sb = new StringBuilder(DEFAULT_UPDATE_FILE_URL);
		sb.append(mToken);
		return sb.toString();
	}
	
	public void save() {
		mPreferences.edit()
		.putLong(KEY_SERVICE_APP_INFO_CHECK_TIME, mCheckTime)
		.putInt(KEY_SERVICE_APP_INFO_VERSION_CODE, mVersionCode)
		.putString(KEY_SERVICE_APP_INFO_VERSION_NAME, mVersionName)
		.putString(KEY_SERVICE_APP_INFO_RELEASENOTE, mReleaseNote)
		.putString(KEY_SERVICE_APP_INFO_APK_URL, mApkUrl)
		.putString(KEY_SERVICE_APP_INFO_APK_SIZE, mSizeStr)
		.putString(KEY_SERVICE_APP_INFO_RELEASEDATE, mReleaseDate)
		.putInt(KEY_SERVICE_APP_INFO_IMPORTANCE, mImportance)
		.commit();
		updateLatestCheckTime(mCheckTime);
	}
	
	public void read() {
		
		mCheckTime = mPreferences.getLong(KEY_SERVICE_APP_INFO_CHECK_TIME, -1l);
		if (mCheckTime == -1l) {
			DebugUtils.logD("AppInfo", "read mCheckTime from preferences " + mCheckTime);
		}
		mVersionCode = mPreferences.getInt(KEY_SERVICE_APP_INFO_VERSION_CODE, -1);
		mVersionName = mPreferences.getString(KEY_SERVICE_APP_INFO_VERSION_NAME, "");
		mReleaseNote = mPreferences.getString(KEY_SERVICE_APP_INFO_RELEASENOTE, "");
		mApkUrl = mPreferences.getString(KEY_SERVICE_APP_INFO_APK_URL, "");
		
		mSizeStr = mPreferences.getString(KEY_SERVICE_APP_INFO_APK_SIZE, "");
		mReleaseDate = mPreferences.getString(KEY_SERVICE_APP_INFO_RELEASEDATE, "");
		mImportance = mPreferences.getInt(KEY_SERVICE_APP_INFO_IMPORTANCE, IMPORTANCE_OPTIONAL);
	}
	
	public boolean hasChecked() {
		return mCheckTime > 0;
	}
	
	public String buildReleasenote(Context context) {
		StringBuilder sb = new StringBuilder();
		sb.append(context.getString(R.string.msg_app_release_time, mReleaseDate)).append("\n");
		sb.append(context.getString(R.string.msg_app_release_size, mSizeStr)).append("\n").append("\n");
		sb.append(mReleaseNote);
		return sb.toString();
	}
	
	/**每次都要从配置文件中读取最后检查更新时间*/
	public static long getLatestCheckTime() {
		return MyApplication.getInstance().mPreferManager.getLong(ServiceAppInfo.KEY_SERVICE_APP_INFO_CHECK_TIME, 0);
	}
	
	public static boolean updateLatestCheckTime(long time) {
		return MyApplication.getInstance().mPreferManager.edit().putLong(ServiceAppInfo.KEY_SERVICE_APP_INFO_CHECK_TIME, time).commit();
	}

	public String toString(Context context) {
		StringBuilder sb = new StringBuilder();
		sb.append("Find new version[mVersionCode=").append(mVersionCode).append(", mVersionName=").append(mVersionName)
		.append(", content=").append(buildReleasenote(context)).append("]");
		return sb.toString();
	}

	@Override
	public int describeContents() {
		return 0;
	}


	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeInt(mVersionCode);
		dest.writeString(mReleaseDate);
		dest.writeInt(mImportance);
		dest.writeString(mSizeStr);
		dest.writeString(mApkUrl);
		dest.writeString(mReleaseNote);
		dest.writeString(mVersionName);
		dest.writeString(mToken);
		
	}
	
	
	public static final Parcelable.Creator<ServiceAppInfo> CREATOR = new Parcelable.Creator<ServiceAppInfo>(){

		@Override
		public ServiceAppInfo createFromParcel(Parcel source) {
			ServiceAppInfo appInfo = new ServiceAppInfo();
			appInfo.mVersionCode = source.readInt();
			appInfo.mReleaseDate = source.readString();
			appInfo.mImportance = source.readInt();
			appInfo.mSizeStr = source.readString();
			appInfo.mApkUrl = source.readString();
			appInfo.mReleaseNote = source.readString();
			appInfo.mVersionName = source.readString();
			appInfo.mToken = source.readString();
			return appInfo;
		}

		@Override
		public ServiceAppInfo[] newArray(int size) {
			return new ServiceAppInfo[size];
		}
		
	};
	
	//add by chenkai, 20140618, updating check begin
    public File buildLocalDownloadAppFile() {
    	StringBuilder sb = new StringBuilder("Warranty_");
    	sb.append(String.valueOf(mVersionCode))
    	.append(mToken)
    	.append(".temp");
    	return MyApplication.getInstance().getAppFiles(sb.toString());
    }
    
    public File buildExternalDownloadAppFile() {
    	if (!MyApplication.getInstance().hasExternalStorage()) {
    		return null;
    	}
    	StringBuilder sb = new StringBuilder("Warranty_");
    	sb.append(String.valueOf(mVersionCode))
    	.append(mToken)
    	.append(".temp");
    	return new File(MyApplication.getInstance().getExternalStorageRoot(".download"), sb.toString());
    }
  //add by chenkai, 20140618, updating check end
	
	
}
