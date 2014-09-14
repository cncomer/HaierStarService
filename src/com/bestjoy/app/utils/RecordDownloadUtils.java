package com.bestjoy.app.utils;

import java.io.IOException;
import java.io.InputStream;

import org.apache.http.client.ClientProtocolException;

import android.text.TextUtils;

import com.bestjoy.app.haierstartservice.MyApplication;
import com.bestjoy.app.haierstartservice.account.MyAccountManager;
import com.shwy.bestjoy.utils.DebugUtils;
import com.shwy.bestjoy.utils.NetworkUtils;

/**
 * 助手类，用于记录下载，将下载人记录存入对方名片下载数据库，将交换人的名号保存到对方收名片夹的名片接受记录表中.
 * 格式:http://www.mingdown.com/cell/jiaohuan.ashx?mm1=下载的名号&mm2=要交换的名号
 * @author chenkai
 *
 */
public class RecordDownloadUtils {
	private static final String TAG = "RecordDownloadUtils";
	/**http://www.mingdown.com/cell/adddownloadrecord.aspx?*/
	private static final String mDownloadRecordUriPrefix = "http://www.mingdown.com/cell/adddownloadrecord.aspx?";

	/**
	 * 该操作是一个阻塞联网操作，不能放在UI线程
	 * Http://www.mingdown.com/cell/ adddownloadrecord.aspx?MM=下载的名片名号 &&cell=下载人的默认本机号码
	 * @param downloadedMm 下载的名片名号
	 * @param tel           下载人的默认本机号码
	 * @return 
	 */
	public static boolean recordDownloadLocked(String downloadedMm, String tel) {
		DebugUtils.logD(TAG, "recordDownloadLocked downloadedMm=" + downloadedMm + " tel=" + tel);
		if (TextUtils.isEmpty(tel)) {
			DebugUtils.logD(TAG, "tel is empty, so we just return true");
			return true;
		}
		StringBuilder sb = new StringBuilder(mDownloadRecordUriPrefix);
		sb.append("MM=").append(downloadedMm)
		.append("&&cell=").append(tel);
		InputStream is = null;
		try {
			is = NetworkUtils.openContectionLocked(sb.toString(), MyApplication.getInstance().getSecurityKeyValuesObject());
			DebugUtils.logD(TAG, "record download successfully.");
			return true;
		} catch (ClientProtocolException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (is != null) {
				try {
					is.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		return false;
	}
	
	public static void recordDownloadInThread(final String downloadedMm) {
		new Thread(new Runnable() {

			@Override
			public void run() {
				DebugUtils.logD(TAG, "recordDownloadInThread run()" );
				recordDownloadLocked(downloadedMm, MyAccountManager.getInstance().getDefaultPhoneNumber());
			}
			
		}).start();
	}
	/**
	 * 记录交换
	 * @param downloadedMm 要下载的名号
	 * @param exchangeMm   用于交换的名号
	 * @return 如果记录成功，返回true,否则返回false
	 */
	public static boolean recordExchange(String downloadedMm, String exchangeMm) {
		StringBuilder sb = new StringBuilder("http://www.mingdown.com/cell/jiaohuan.ashx?");
		sb.append("mm1=").append(downloadedMm).append("&mm2=").append(exchangeMm);
		try {
			InputStream is = NetworkUtils.openContectionLocked(sb.toString(), MyApplication.getInstance().getSecurityKeyValuesObject());
			if (is == null) {
				return false;
			}
			String resultStr = NetworkUtils.getContentFromInput(is);
			DebugUtils.logD(TAG, "service return " + resultStr);
			if ("ok".equals(resultStr)) {
				DebugUtils.logD(TAG, "finish recording exchange " + resultStr);
				return true;
			}
		} catch (ClientProtocolException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return false;
	}
}
