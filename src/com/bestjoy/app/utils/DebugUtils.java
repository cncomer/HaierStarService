package com.bestjoy.app.utils;

import android.util.Log;

public class DebugUtils {
	public static final boolean DEBUG = true;

	public static void logD(String tag, String msg) {
		if (DEBUG) Log.d(tag, "cncom " + msg);
	}
	
	public static void logParser(String tag, String msg) {
		if (DEBUG) Log.d(tag, "cncom " + msg);
	}
	
	public static void logW(String tag, String msg) {
		Log.d(tag, "cncom " + msg);
	}
	
	public static void logE(String tag, String msg) {
		Log.d(tag, "cncom " + msg);
	}
}
