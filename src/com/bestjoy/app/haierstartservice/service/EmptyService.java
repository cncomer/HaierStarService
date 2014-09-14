package com.bestjoy.app.haierstartservice.service;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import com.shwy.bestjoy.utils.DebugUtils;

public class EmptyService extends Service{

	private static final String TAG = "EmptyService";
	@Override
	public IBinder onBind(Intent arg0) {
		return null;
	}

	@Override
	public void onCreate() {
		super.onCreate();
		DebugUtils.logD(TAG, "onCreate()");
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		DebugUtils.logD(TAG, "onDestroy()");
	}

	@Override
	public void onStart(Intent intent, int startId) {
		super.onStart(intent, startId);
		DebugUtils.logD(TAG, "onStart()");
	}
	
	

}
