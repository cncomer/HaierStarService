package com.google.zxing.client.result;

import java.util.LinkedList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.text.TextUtils;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.Result;
import com.shwy.bestjoy.utils.DebugUtils;

public class HaierParser extends ResultParser{
	private static final String TAG = "HaierParser";
	public static final Pattern FIND_PATTERN = Pattern.compile("http://oid.haier.com/oid\\?ewm=(.+)");
	/**前缀列表，只要是扫码得到的内容前缀在这个列表中，我们就认为是一个保修卡条码，按照海尔的保修卡条码处理*/
	public static final LinkedList<String> FIND_LIST = new LinkedList<String>();
	static {
		FIND_LIST.add("http://c.dzbxk.com");
	}
	@Override
	public ParsedResult parse(Result theResult) {
		String rawText = theResult.getText().trim().replaceAll(" ", ""); 
		if (TextUtils.isEmpty(rawText)) {
			return null;
		}
		if (theResult.getBarcodeFormat() == BarcodeFormat.CODE_128) {
			if (rawText.length() == 20) {
				DebugUtils.logD(TAG, "find Haier CODE_128 " + rawText);
				return new HaierParsedResult(rawText, rawText, theResult.getBarcodeFormat(), HaierParsedResult.ResultBaoxiuCardType.Haier);
			}
		}
		Matcher matcher = FIND_PATTERN.matcher(rawText);
		String param = null;
		if (matcher.find()) {
			param = matcher.group(1);
			DebugUtils.logD(TAG, "find Haier barcode " + param + ", and length is "+ param.length());
			return new HaierParsedResult(rawText, param);
		} else if (checkFindList(rawText)) {
			return new HaierParsedResult(rawText, rawText.replace("http://", ""), theResult.getBarcodeFormat(), HaierParsedResult.ResultBaoxiuCardType.General);
		} else {
			return null;
		}
	}
	
	public static boolean checkFindList(String text) {
		for(String prefix:FIND_LIST) {
			if (text.startsWith(prefix)) {
				return true;
			}
		}
		return false;
	}

}
