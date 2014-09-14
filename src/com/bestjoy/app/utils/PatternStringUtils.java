package com.bestjoy.app.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PatternStringUtils {

	public static String getStringPattern(String str, String pattern) {
		Pattern mPattern = Pattern.compile(pattern);
		Matcher mMatcher = mPattern.matcher(str);
		if(mMatcher.find()) {
			return mMatcher.group(1);
		}
		return null;
	}
}
