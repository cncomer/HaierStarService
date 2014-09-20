package com.bestjoy.app.haierstartservice;

import java.io.File;

import org.json.JSONObject;

import android.app.AlertDialog;
import android.app.Application;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.net.wifi.WifiManager;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

import com.bestjoy.app.haierstartservice.account.MyAccountManager;
import com.bestjoy.app.haierstartservice.service.PhotoManagerUtilsV2;
import com.bestjoy.app.haierstartservice.view.ModuleViewUtils;
import com.bestjoy.app.utils.BeepAndVibrate;
import com.bestjoy.app.utils.BitmapUtils;
import com.bestjoy.app.utils.VcfAsyncDownloadUtils;
import com.bestjoy.app.utils.YouMengMessageHelper;
import com.shwy.bestjoy.contacts.AddrBookUtils;
import com.shwy.bestjoy.utils.ComConnectivityManager;
import com.shwy.bestjoy.utils.ComPreferencesManager;
import com.shwy.bestjoy.utils.DevicesUtils;
import com.shwy.bestjoy.utils.SecurityUtils.SecurityKeyValuesObject;
import com.umeng.analytics.MobclickAgent;

public class MyApplication extends Application{
	
	private static final String TAG ="MyApplication";
	/**对于不同的保修卡，我们只要确保该变量为正确的应用包名即可*/
	public static final String PKG_NAME = "com.bestjoy.app.haierstartservice";
	private Handler mHandler;
	private static MyApplication mInstance;
	public SharedPreferences mPreferManager;
	
	private InputMethodManager mImMgr;
	
	@Override
	public void onCreate() {
		super.onCreate();
		MobclickAgent.setDebugMode(false);
		Log.d(TAG, "onCreate()");
		mHandler = new Handler();
		mInstance = this;
		//add by chenkai, 20131201, 网络监听
		ComConnectivityManager.getInstance().setContext(this);
		BeepAndVibrate.getInstance().setContext(this);
		
		BitmapUtils.getInstance().setContext(this);
		
		MyAccountManager.getInstance().setContext(this);
		
		mPreferManager = PreferenceManager.getDefaultSharedPreferences(this);
		HaierServiceObject.setContext(this);
		
		mImMgr = (InputMethodManager) this.getSystemService(Context.INPUT_METHOD_SERVICE);
		
		PhotoManagerUtilsV2.getInstance().setContext(this);
		//用于屏幕适配
		DisplayMetrics display = this.getResources().getDisplayMetrics();
		Log.d(TAG, display.toString());
		Log.d(TAG, getDeviceInfo(this));
		
		YouMengMessageHelper.getInstance().setContext(this);
		//用来下载名片使用
		VcfAsyncDownloadUtils.getInstance().setContext(this);
		//用来保存联系人使用
		AddrBookUtils.getInstance().setContext(this);
		//注册会用到IMEI号
		DevicesUtils.getInstance().setContext(this);
		ModuleViewUtils.getInstance().setContext(this);
		ComPreferencesManager.getInstance().setContext(this);
		
	}
	
	public synchronized static MyApplication getInstance() {
		return mInstance;
	}
	
	public File getCachedContactFile(String name) {
		return new File(getFilesDir(), name+ ".vcf");
	}
	
	public File getAppFilesDir(String dirName) {
		File root = new File(getFilesDir(), dirName);
		if (!root.exists()) {
			root.mkdirs();
		}
		return root;
	}
	
	public File getAccountDir(String accountMd) {
		File accountRoot = new File(getAppFilesDir("accounts"), accountMd);
		
		if (!accountRoot.exists()) {
			accountRoot.mkdirs();
		}
		return accountRoot;
	}
	
	/**返回产品图像文件files/product/avator*/
	public File getProductPreviewAvatorFile(String photoid) {
		return new File(getProductSubDir("avator"), photoid+ ".p");
	}
	
	/**返回产品发票文件files/product/bill/*/
	public File getProductFaPiaoFile(String photoid) {
		return new File(getProductSubDir("bill"), photoid+ ".b");
	}
	
	public File getProductDir() {
		File productRoot = new File(getAppFilesDir("accounts"), "product");
		if (!productRoot.exists()) {
			productRoot.mkdirs();
		}
		return productRoot;
	}
	public File getProductSubDir(String dirName) {
		File productRoot = new File(getProductDir(), dirName);
		if (!productRoot.exists()) {
			productRoot.mkdirs();
		}
		return productRoot;
	}
	/**得到账户名片的头像图片文件*/
	public File getAccountCardAvatorFile(String name) {
		return new File(getAccountDir(MyAccountManager.getInstance().getCurrentAccountUid()), name+ ".p");
	}
	public File getAccountCardAvatorFile(String accountMd, String name) {
		return new File(getAccountDir(accountMd), name+ ".p");
	}
	/**返回缓存目录caches/下面的临时头像文件*/
	public File getCachedPreviewAvatorFile(String photoid) {
		return new File(getCacheDir(), photoid+ ".p");
	}
	/**返回缓存目录caches/下面的临时vcf文件*/
	public File getCachedPreviewContactFile(String name) {
		return new File(getCacheDir(), name+ ".vcf");
	}
	
	@Override
	public void onTerminate() {
		super.onTerminate();
		ComConnectivityManager.getInstance().endConnectivityMonitor();
	}
	
	
	public boolean hasExternalStorage() {
	    	return Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED);
	}

	public void showMessageAsync(final int resId) {
		mHandler.post(new Runnable() {

			@Override
			public void run() {
				Toast.makeText(mInstance, resId, Toast.LENGTH_LONG).show();
			}
		});
	}
	
	public void showMessageAsync(final String msg) {
		mHandler.post(new Runnable() {

			@Override
			public void run() {
				Toast.makeText(mInstance, msg, Toast.LENGTH_LONG).show();
			}
		});
	}
	
	public void showMessageAsync(final int resId, final int length) {
		mHandler.post(new Runnable() {

			@Override
			public void run() {
				Toast.makeText(mInstance, resId, resId).show();
			}
		});
	}
	
	public void showMessageAsync(final String msg, final int length) {
		mHandler.post(new Runnable() {

			@Override
			public void run() {
				Toast.makeText(mInstance, msg, length).show();
			}
		});
	}
	
	public void showShortMessageAsync(final int msgId, final int toastId) {
		mHandler.post(new Runnable() {

			@Override
			public void run() {
				Toast.makeText(mInstance, msgId, toastId).show();
			}
		});
	}
	
	public void showMessage(int resId) {
		Toast.makeText(mInstance, resId, Toast.LENGTH_LONG).show();
	}
	
	public void showMessage(String msg) {
		Toast.makeText(mInstance, msg, Toast.LENGTH_LONG).show();
	}
	public void showMessage(String msg, int length) {
		Toast.makeText(mInstance, msg, length).show();
	}
	
	public void showMessage(int resId, int length) {
		Toast.makeText(mInstance, resId, length).show();
	}
	
	public void showShortMessage(int resId) {
		showMessage(resId, Toast.LENGTH_SHORT);
	}
	
	public void postAsync(Runnable runnable){
		mHandler.post(runnable);
	}
	public void postDelay(Runnable runnable, long delayMillis){
		mHandler.postDelayed(runnable, delayMillis);
	}
	
	
	public void showUnsupportMessage() {
    	showMessage(R.string.msg_unsupport_operation);
    }
	
	//add by chenkai, 20131123, security support begin
    private SecurityKeyValuesObject mSecurityKeyValuesObject;
    public SecurityKeyValuesObject getSecurityKeyValuesObject() {
    	if (mSecurityKeyValuesObject == null) {
    		//Here, we need to notice.
    		new Exception("warnning getSecurityKeyValuesObject() return null").printStackTrace();
    	}
    	return mSecurityKeyValuesObject;
    }
    public void setSecurityKeyValuesObject(SecurityKeyValuesObject securityKeyValuesObject) {
    	mSecurityKeyValuesObject = securityKeyValuesObject;
    }
    
  //add by chenkai, 20131208, updating check begin
    public File buildLocalDownloadAppFile(int downloadedVersionCode) {
    	StringBuilder sb = new StringBuilder("Warranty_");
    	sb.append(String.valueOf(downloadedVersionCode))
    	.append(".apk");
    	return new File(getExternalStorageRoot(".download"), sb.toString());
    }
    
    /**
     * 返回SD卡的应用根目录，type为子目录名字， 如download、.download
     * @param type
     * @return
     */
    public File getExternalStorageRoot(String type) {
    	if (!hasExternalStorage()) {
    		return null;
    	}
    	File root = new File(Environment.getExternalStorageDirectory(), getPackageName());
    	if (!root.exists()) {
    		root.mkdirs();
    	}
    	root =  new File(root, type);
    	if (!root.exists()) {
    		root.mkdir();
    	}
    	return root;
    }
    //add by chenkai, 20131208, updating check end
    
    /***
     * 显示通常的网络连接错误
     * @return
     */
    public String getGernalNetworkError() {
    	return this.getString(R.string.msg_gernal_network_error);
    }
    
  //add by chenkai, for Usage, 2013-06-05 begin
    /**return mnt/sdcard/xxx/accountmd目录*/
    public File getExternalStorageAccountRoot(String accountMd) {
    	if (!hasExternalStorage()) {
    		return null;
    	}
    	File root =  new File(getExternalStorageRoot("account"), accountMd);
    	if (!root.exists()) {
    		root.mkdir();
    	}
    	return root;
    }
    /**得到SD卡账号对应组件的目录,*/
    public File getExternalStorageModuleRootForAccount(String accountMd, String moduleName) {
    	if (!hasExternalStorage()) {
    		return null;
    	}
    	File root = new File(getExternalStorageAccountRoot(accountMd), moduleName);
    	if (!root.exists()) {
    		root.mkdirs();
    	}
    	return root;
    }
    /**返回产品使用说明书*/
    public File getProductUsagePdf(String ky) {
    	String accountUid = String.valueOf(MyAccountManager.getInstance().getAccountObject().mAccountUid);
		File goodsUsagePdfFile =  new File(getExternalStorageModuleRootForAccount(accountUid, "product") , ky + ".pdf");
		return goodsUsagePdfFile;
	}
    /**提示没有SD卡可用*/
    public void showNoSDCardMountedMessage() {
    	showMessage(R.string.msg_sd_unavailable);
    }
    //add by chenkai, for Usage, 2013-06-05 end
    
    public void hideInputMethod(IBinder token) {
    	if (mImMgr != null) {
    		mImMgr.hideSoftInputFromWindow(token, 0);
    	}
    }
    
    /**显示需要先新建家提示信息*/
	public void showNeedHomeMessage() {
    	showMessage(R.string.msg_need_home_operation);
    }
	/**显示需要先登录提示信息*/
	public void showNeedLoginMessage() {
    	showMessage(R.string.msg_need_login_operation);
    }
    /**
     * 返回缓存的品牌型号文件，如果有外置SD卡，该文件会存在外置存储卡xxx/account/xxxx/xinghao目录下，否则在手机内部存储中xxx/files/
     * @param pingpaiCode
     * @return
     */
    public File getCachedXinghaoFile(String pingpaiCode) {
    	File xinghaoFile =  null;
    	if (hasExternalStorage()) {
    		xinghaoFile =  new File(getCachedXinghaoExternalRoot() , pingpaiCode + ".json");
    	} else {
    		xinghaoFile =  new File(getCachedXinghaoInternalRoot() , pingpaiCode + ".json");;
    	}
		return xinghaoFile;
    }
    /**
     * 得到sdcard上的型号目录/mnt/sdcard/xxxx/xinghao
     * @return
     */
    public File getCachedXinghaoExternalRoot() {
    	return getExternalStorageRoot("xinghao");
    }
    /**
     * 得到sdcard上的型号目录/xxx/files/xinghao
     * @return
     */
    public File getCachedXinghaoInternalRoot() {
    	return getAppFilesDir("xinghao");
    }
    
    public File getAppFiles(String fileName) {
    	File root = getFilesDir();
    	if (!root.exists()) {
    		root.mkdirs();
    	}
		return new File(root, fileName);
	}
  //add by chenkai, 锁定认证字段 20140701 begin
    /**显示认证锁定的保修卡提示框*/
    public AlertDialog showLockedEditMode(Context context, String msg, DialogInterface.OnClickListener callback) {
    	AlertDialog dialog = new AlertDialog.Builder(context)
	    	.setMessage(msg)
	    	.setPositiveButton(android.R.string.ok, callback)
			.show();
    	return dialog;
    }
    public AlertDialog showLockedEditMode(Context context, int msgId, DialogInterface.OnClickListener callback) {
    	AlertDialog dialog = new AlertDialog.Builder(context)
	    	.setMessage(msgId)
	    	.setCancelable(false)
	    	.setPositiveButton(android.R.string.ok, callback)
			.show();
    	return dialog;
    }
  //add by chenkai, 锁定认证字段 20140701 begin
	public static String getDeviceInfo(Context context) {
	    try{
	        JSONObject json = new JSONObject();
	        TelephonyManager tm = (android.telephony.TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
	  
	        String device_id = tm.getDeviceId();
	      
	        WifiManager wifi = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
	          
	        String mac = wifi.getConnectionInfo().getMacAddress();
	        json.put("mac", mac);
	      
	       if(TextUtils.isEmpty(device_id) ){
	            device_id = mac;
	       }
	      
	      if( TextUtils.isEmpty(device_id) ){
	           device_id = android.provider.Settings.Secure.getString(context.getContentResolver(), android.provider.Settings.Secure.ANDROID_ID);
	      }
	      
	      json.put("device_id", device_id);
	      return json.toString();
	    }catch(Exception e){
	      e.printStackTrace();
	    }
	    return null;
	}
                  
}
