package com.bestjoy.app.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.EditText;
import android.widget.Toast;

import com.bestjoy.app.haierstartservice.R;
import com.iflytek.cloud.speech.RecognizerListener;
import com.iflytek.cloud.speech.RecognizerResult;
import com.iflytek.cloud.speech.SpeechConstant;
import com.iflytek.cloud.speech.SpeechError;
import com.iflytek.cloud.speech.SpeechListener;
import com.iflytek.cloud.speech.SpeechRecognizer;
import com.iflytek.cloud.speech.SpeechUser;
import com.iflytek.cloud.ui.RecognizerDialog;
import com.iflytek.cloud.ui.RecognizerDialogListener;

public class SpeechRecognizerEngine {
	private static final String TAG = "SpeechRecognizerEngine";
	private static SpeechRecognizerEngine mSpeechRecognizerEngine;
	private Context mContext;
	private Toast mToast;
	// 识别窗口
	private RecognizerDialog iatDialog;
	// 识别对象
	private SpeechRecognizer iatRecognizer;
	// 识别结果显示
	private static EditText mResultText;

	@SuppressLint("ShowToast")
	private SpeechRecognizerEngine(Context context) {
		DebugUtils.logD(TAG, "onCreate()");
		mContext = context;
		// 用户登录
		SpeechUser.getUser().login(mContext, null, null,
				"appid=" + mContext.getString(R.string.app_id), listener);
		// 创建听写对象,如果只使用无UI听写功能,不需要创建RecognizerDialog
		iatRecognizer = SpeechRecognizer.createRecognizer(mContext);
		// 初始化听写Dialog,如果只使用有UI听写功能,无需创建SpeechRecognizer
		iatDialog = new RecognizerDialog(mContext);
		mToast = Toast.makeText(mContext, "", Toast.LENGTH_LONG);
	}

	public static SpeechRecognizerEngine getInstance(Context context) {
		if (mSpeechRecognizerEngine == null) {
			mSpeechRecognizerEngine = new SpeechRecognizerEngine(context);
		}
		return mSpeechRecognizerEngine;
	}

	public static void setResultText(EditText mAskInput) {
		mResultText = mAskInput;
	}
	
	/**
	 * 显示听写对话框.
	 * @param context 
	 * 
	 * @param
	 */
	public void showIatDialog(Context context) {
		//if (null == iatDialog) {
			// 初始化听写Dialog
			iatDialog = new RecognizerDialog(context);
		//}

		// 清空Grammar_ID，防止识别后进行听写时Grammar_ID的干扰
		iatDialog.setParameter(SpeechConstant.CLOUD_GRAMMAR, null);
		;
		// 设置听写Dialog的引擎
		iatDialog.setParameter(SpeechConstant.DOMAIN, "iat");
		iatDialog.setParameter(SpeechConstant.SAMPLE_RATE, "16000");

		// 清空结果显示框.
		mResultText.setText(null);
		// 显示听写对话框
		iatDialog.setListener(recognizerDialogListener);
		iatDialog.show();
		showTip(mContext.getString(R.string.start_speak));
	}

	RecognizerListener recognizerListener = new RecognizerListener() {

		@Override
		public void onBeginOfSpeech() {
			showTip(mContext.getString(R.string.start_speak));
		}

		@Override
		public void onError(SpeechError err) {
			showTip(err.getPlainDescription(true));
		}

		@Override
		public void onEndOfSpeech() {
			showTip(mContext.getString(R.string.end_speak));
		}

		@Override
		public void onEvent(int eventType, int arg1, int arg2, String msg) {

		}

		@Override
		public void onResult(RecognizerResult results, boolean isLast) {
			String text = JsonParser.parseIatResult(results.getResultString());
			mResultText.append(text);
			mResultText.setSelection(mResultText.length());
		}

		@Override
		public void onVolumeChanged(int volume) {
			showTip(mContext.getString(R.string.speak_tips) + volume);
		}

	};
	/**
	 * 识别回调监听器
	 */
	RecognizerDialogListener recognizerDialogListener = new RecognizerDialogListener() {
		@Override
		public void onResult(RecognizerResult results, boolean isLast) {
			String text = JsonParser.parseIatResult(results.getResultString());
			mResultText.append(text);
			mResultText.setSelection(mResultText.length());
		}

		/**
		 * 识别回调错误.
		 */
		public void onError(SpeechError error) {

		}

	};

	private void showTip(String str) {
		if (!TextUtils.isEmpty(str)) {
			mToast.setText(str);
			mToast.show();
		}
	}

	/**
	 * 获取字节流对应的字符串,文件默认编码为UTF-8
	 * 
	 * @param inputStream
	 * @return
	 * @throws UnsupportedEncodingException
	 * @throws IOException
	 */
	@SuppressWarnings("unused")
	private String readStringFromInputStream(InputStream inputStream)
			throws UnsupportedEncodingException, IOException {
		BufferedReader reader = new BufferedReader(new InputStreamReader(
				inputStream, "UTF-8"));
		StringBuilder builder = new StringBuilder();
		String line;
		while ((line = reader.readLine()) != null) {
			builder.append(line);
		}
		return builder.toString();
	}

	/**
	 * 用户登录回调监听器.
	 */
	private SpeechListener listener = new SpeechListener() {

		@Override
		public void onData(byte[] arg0) {
		}

		@Override
		public void onCompleted(SpeechError error) {
			if (error != null) {
				Toast.makeText(mContext,
						mContext.getString(R.string.login_fail),
						Toast.LENGTH_SHORT).show();
			}
		}

		@Override
		public void onEvent(int arg0, Bundle arg1) {
		}
	};
}
