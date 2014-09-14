package com.bestjoy.app.haierstartservice.service;

import android.app.Notification;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.IBinder;
import android.widget.ImageView;

import com.bestjoy.app.haierstartservice.service.PhotoManagerUtilsV2.TaskType;

public class PhotoManagerService extends Service{
	private PhotoManagerUtilsV2 mPhotoManagerUtils;
	private static PhotoManagerService mInstance = null;
	private static final int PHOTO_SERVICE_FOREGROUND = 10000;

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}
	
	public static PhotoManagerService getInstance() {
		return mInstance;
	}
	
	public static boolean isServiceRunning() {
		return mInstance != null;
	}

	@Override
	public void onCreate() {
		super.onCreate();
		startForeground(PHOTO_SERVICE_FOREGROUND, new Notification());
		mPhotoManagerUtils = PhotoManagerUtilsV2.getInstance();
		mPhotoManagerUtils.setContext(this);
		mInstance = this;
	}

	@Override
	public void onStart(Intent intent, int startId) {
		super.onStart(intent, startId);
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		return super.onStartCommand(intent, flags, startId);
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		mInstance = null;
	}
	
	public void setAvatorSize(int width, int height) {
		mPhotoManagerUtils.setAvatorSize(width, height);
	}
	
	public void updatePhoto(String photoId, TaskType type, Bitmap bitmap) {
		mPhotoManagerUtils.addBitmapToCache(photoId, type, bitmap);
	}
	
	public void requestToken(String token) {
		mPhotoManagerUtils.requestToken(token);
	}
	
	public void releaseToken(String token) {
		mPhotoManagerUtils.releaseToken(token);
	}
	
//	public void loadAvatorAsync(String token, ImageView view, String mm) {
//		mPhotoManagerUtils.loadAvatorAsync(token, view, mm);
//	}
//	
//	
//	public void loadAvatorAsync(String token, ImageView view, String mm, byte[] photo) {
//		mPhotoManagerUtils.loadAvatorAsync(token, view, mm, photo);
//	}
	
	public void loadPhotoAsync(String token, ImageView view, String no, byte[] photo, TaskType type) {
		mPhotoManagerUtils.loadPhotoAsync(token, view, no, photo, type);
	}
	public void loadLocalPhotoAsync(String token, ImageView view, String no, byte[] photo, TaskType type) {
		mPhotoManagerUtils.loadLocalPhotoAsync(token, view, no, photo, type);
	}

	public static Intent getServiceIntent(Context context) {
		Intent service = new Intent(context, PhotoManagerService.class);
		service.setAction("com.bestjoy.app.haierwarrantycard.service.intent.initphotoservice");
		return service;
	}
}
