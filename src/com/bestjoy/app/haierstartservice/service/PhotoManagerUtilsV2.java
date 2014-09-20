package com.bestjoy.app.haierstartservice.service;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.ClientProtocolException;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.ImageView;

import com.bestjoy.app.haierstartservice.HaierServiceObject;
import com.bestjoy.app.haierstartservice.MyApplication;
import com.bestjoy.app.haierstartservice.R;
import com.shwy.bestjoy.utils.Contents;
import com.shwy.bestjoy.utils.DebugUtils;
import com.shwy.bestjoy.utils.Intents;
import com.shwy.bestjoy.utils.NetworkUtils;
import com.shwy.bestjoy.utils.NotifyRegistrant;

public class PhotoManagerUtilsV2 {
	private static final String TAG ="PhotoManagerUtils";
	private static PhotoManagerUtilsV2 INSTANCE = new PhotoManagerUtilsV2();
	private static Bitmap mDefaultBitmap;
	private static Bitmap mDefaultCircleTopicBitmap;
	private static Bitmap mDefaultCirclePhotoBitmap;
	private static Bitmap mDefaultLoadBitmap;
	
	private static Bitmap mDefaultKyBitmap;
	private static Bitmap mDefaultBaoxiucardAvator;
	private Context mContext;
	private Resources mResources;
	private static final int MAX_CAPACITY = 100;
	private static float MAX_RESULT_IMAGE_SIZE = 140f;
	private float mCurrentImageSize = MAX_RESULT_IMAGE_SIZE;
	
	public static final String EXTRA_DOWNLOAD_STATUS="status";
	public static final String EXTRA_DOWNLOAD_STATUS_MESSAGE="message";
	
	private LinkedHashMap<String, LinkedList<AvatorAsyncTask>> mAsyncTaskTokenMap = new LinkedHashMap<String, LinkedList<AvatorAsyncTask>>(20) {
		@Override
		protected boolean removeEldestEntry(Entry<String, LinkedList<AvatorAsyncTask>> eldest) {
			boolean over = size() >= 20;
			if (over) {
				cancel(eldest.getKey());
			}
			return over;
		}
	};
	
	private static final int HARD_CACHE_CAPACITY = 100;
    // Hard cache, with a fixed maximum capacity and a life duration
    private final static HashMap<String, SoftReference<Bitmap>> sHardBitmapCache = new LinkedHashMap<String, SoftReference<Bitmap>>(HARD_CACHE_CAPACITY / 2);
 
    public void addBitmapToCache(String photoId, TaskType type, Bitmap bitmap) {
    	String sPhotoId = checkPhotoId(photoId,type);
        if (bitmap != null) {
            synchronized (sHardBitmapCache) {
                try {
                    sHardBitmapCache.put(sPhotoId, new SoftReference<Bitmap>(bitmap));
                    DebugUtils.logPhotoUtils(TAG, "final step add bitmap to BitmapCache for photoId " + sPhotoId);
                } catch (OutOfMemoryError ex) {
                    // Oops, out of memory, clear cache
                    clearCache();
                }
            }
        } else {
        	sHardBitmapCache.remove(sPhotoId);
        	DebugUtils.logPhotoUtils(TAG, " remove bitmap from BitmapCache for photoId " + sPhotoId);
        }
    }
 
    public static Bitmap getBitmapFromCache(String photoId, TaskType type) {
        if (photoId == null) {
            return null;
        }
        // First try the hard reference cache
        synchronized (sHardBitmapCache) {
            final SoftReference<Bitmap> bitmapSP = sHardBitmapCache.get(checkPhotoId(photoId,type));
            if (bitmapSP != null) {
                // Bitmap found in hard cache
                // Move element to first position, so that it is removed last
                sHardBitmapCache.remove(photoId);
                Bitmap bitmap = bitmapSP.get();
                if (bitmap != null) {
                	sHardBitmapCache.put(photoId, bitmapSP);
                	DebugUtils.logPhotoUtils(TAG, "get bitmap from BitmapCache for photoId " + photoId);
                	return bitmap;
                }
                
            }
        }
        return null;
    }
    /***
     * 有时候，对于不同的TaskType，其photoId是同样的，比如展会通，同一个展会的ID，ids.png和id.png分别就是不同的TaskType,我们就不能仅仅是依据photoId来缓存了。
     * 这里，使用的是保存的文件名来区分。
     * @param photoId
     * @param type
     * @return
     */
    private static String checkPhotoId(String photoId, TaskType type) {
    	switch(type) {
    	case HOME_DEVICE_AVATOR:
			default:
				return photoId;
		}
    }
 
    public void clearCache() {
        sHardBitmapCache.clear();
    }
 
    public static HashMap<String, SoftReference<Bitmap>> getShardBitmapcache() {
        return sHardBitmapCache;
    }
	
	private PhotoManagerUtilsV2() { }
	
	
	public void setContext(Context context) {
		if (mContext == null) {
			mContext = context;
			mResources = context.getResources();
			MAX_RESULT_IMAGE_SIZE = mContext.getResources().getDimension(R.dimen.barcode_image_view_size);
			mCurrentImageSize = MAX_RESULT_IMAGE_SIZE;
			mDefaultKyBitmap = BitmapFactory.decodeResource(mResources, R.drawable.ky_default);
			mDefaultBitmap = scaleBitmap(BitmapFactory.decodeResource(mResources, R.drawable.bjfile_icon), TaskType.PREVIEW);
			mDefaultBaoxiucardAvator = BitmapFactory.decodeResource(mResources, R.drawable.baoxiuka_avator_default);
		}
		
//		initCache();
//		mContext.getCacheDir().mkdirs();
	}
	
	public void setAvatorSize(int width, int height) {
		mCurrentImageSize = Math.min(width, height);
	}
	
	public void requestToken(String token) {
		if (mAsyncTaskTokenMap.containsKey(token)) {
		    cancel(token);
		} else {
			mAsyncTaskTokenMap.put(token, new LinkedList<AvatorAsyncTask>());
		}
	}
	
	public void releaseToken(String token) {
		if (mAsyncTaskTokenMap.containsKey(token)) {
			cancel(token);
			mAsyncTaskTokenMap.remove(token);
		} 
	}
	
	/*public*/ AvatorAsyncTask findTask(String token, String NO, boolean remove) {
		if (mAsyncTaskTokenMap.containsKey(token)) {
			LinkedList<AvatorAsyncTask> tasks = mAsyncTaskTokenMap.get(token);
			for(AvatorAsyncTask task : tasks) {
				if (task.match(token, NO)) {
					//����mAsyncTaskTokenMap�ҵ���һ��ƥ�䣬removeΪtrue������Ҫ�Ƴ���
					if (remove) tasks.remove(task);
					return task;
				}
			}
		} 
		return null;
	}
	
	/*public*/ void removeTask(String token, AvatorAsyncTask task) {
		if (mAsyncTaskTokenMap.containsKey(token)) {
			LinkedList<AvatorAsyncTask> tasks = mAsyncTaskTokenMap.get(token);
//			if (tasks.contains(task)) {
				boolean removed = tasks.remove(task);
				if (removed) {
					DebugUtils.logPhotoUtils(TAG, "Ok:remove a task with token " + token + " id is " + task.mPhotoId);
				} else {
					DebugUtils.logPhotoUtils(TAG, "Failed:remove a task with token " + token + " id is " + task.mPhotoId);
				}
//			}
			
		} 
	}
	
	/*public*/ void addTask(String token, AvatorAsyncTask task) {
		if (mAsyncTaskTokenMap.containsKey(token)) {
			LinkedList<AvatorAsyncTask> tasks = mAsyncTaskTokenMap.get(token);
			
			boolean added = tasks.add(task);
			if (added) {
				DebugUtils.logPhotoUtils(TAG, "Ok:add a task with token " + token + " id is " + task.mPhotoId);
			} else {
				DebugUtils.logPhotoUtils(TAG, "Failed:add a task with token " + token + " id is " + task.mPhotoId);
			}
		} else {
			DebugUtils.logPhotoUtils(TAG, "add a new token " + token + " in mAsyncTaskTokenMap ");
			mAsyncTaskTokenMap.put(token, new LinkedList<AvatorAsyncTask>());
			addTask(token, task);
		}
	}
	
	/*public*/ void cancel(String token) {
		DebugUtils.logPhotoUtils(TAG, "cancel():cancel all tasks with token " + token);
		if (mAsyncTaskTokenMap.containsKey(token)) {
			 LinkedList<AvatorAsyncTask> tasks = mAsyncTaskTokenMap.get(token);
			 for(AvatorAsyncTask task: tasks) {
				 DebugUtils.logPhotoUtils(TAG, "cancel():cancel task with no is " + task.mPhotoId);
				 task.cancel(true);
			 }
			 int count = tasks.size();
			 if (count > 0) {
				 tasks.clear();
				 DebugUtils.logPhotoUtils(TAG, "cancel():has canceled " + count + " task");
			 } else {
				 DebugUtils.logPhotoUtils(TAG, "cancel():has no tasks to cancel for token " + token);
			 }
			 
			 
		}
	}
	
	public Bitmap getDefaultBitmap() {
		return mDefaultBitmap;
	}
	
	public Bitmap decodeFromInputStream(InputStream is, TaskType type) {
		if (is == null) return null;
		try {
			Bitmap bitmap =  BitmapFactory.decodeStream(is, null, null);
			return scaleBitmap(bitmap, type);
		} catch (OutOfMemoryError oom) {
			oom.printStackTrace();
			return null;
		}
		
	}
	
	public Bitmap decodeByteArray(File bitmapFileToCache, byte[] byteArray, TaskType type) {
		if (byteArray == null) return null;
		try {
			Bitmap bitmap =  BitmapFactory.decodeByteArray(byteArray, 0, byteArray.length);
			createCachedBitmapFile(bitmapFileToCache, bitmap);
			return scaleBitmap(bitmap, type);
		} catch (OutOfMemoryError oom) {
			oom.printStackTrace();
			return null;
		}
		
	}
	
	public Bitmap decodeFromCachedBitmapFile(File bitmapFile, TaskType type) {
		Bitmap bitmap = null;
		if (!bitmapFile.exists()) {
			return null;
		}
		try {
			bitmap =  BitmapFactory.decodeStream(new FileInputStream(bitmapFile), null, null);
			return scaleBitmap(bitmap, type);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return null;
		} catch (OutOfMemoryError oom) {
			oom.printStackTrace();
			return null;
		}
	}
	
	public static boolean createCachedBitmapFile(File bitmapFileToCache, Bitmap bitmap) {
		if (bitmapFileToCache.exists()) {
			bitmapFileToCache.delete();
		}
		try {
			return bitmap.compress(CompressFormat.PNG, 100, new FileOutputStream(bitmapFileToCache));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		return false;
		
	}
	
	public static void createCachedBitmapFile(InputStream is, File bitmapFileToCache) {
		byte[] buffer = new byte[4096];
		int read = 0;
		FileOutputStream fos = null;
		try {
			fos = new FileOutputStream(bitmapFileToCache);
			read = is.read(buffer);
			while(read != -1) {
				fos.write(buffer, 0, read);
				read = is.read(buffer);
			}
			fos.flush();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			NetworkUtils.closeOutStream(fos);
			
		}
	}
	
	/*public*/ Bitmap scaleBitmap(Bitmap srcBitmap, TaskType type) {
		if (srcBitmap == null) {
			return null;
		}
		int width = srcBitmap.getWidth();
		int height = srcBitmap.getHeight();
		DebugUtils.logPhotoUtils(TAG, "srcBitmap getWidth " + width + " getHeight " + height);
		float currentImageSize = 0f;
		switch(type) {
		case HOME_DEVICE_AVATOR:
			break;
			default:
				break;
		}
		if(true) return srcBitmap;
		// 计算缩放比例
//		float scaleWidth =  MAX_RESULT_IMAGE_SIZE / width;
//		float scaleHeight = MAX_RESULT_IMAGE_SIZE / height;
		float scale = mCurrentImageSize / Math.max(width, height);
		// 取得想要缩放的matrix参数
		Matrix matrix = new Matrix();
		matrix.postScale(scale, scale);
		// 得到新的图片
		Bitmap newBm = Bitmap.createBitmap(srcBitmap, 0, 0, width, height, matrix, true);
		return newBm;
	}
	
	public Bitmap getDefaultBitmap(TaskType type) {
		switch(type) {
		case HOME_DEVICE_AVATOR:
			return mDefaultKyBitmap;
		case Baoxiucard_Salesman_Avator:
			return mDefaultBaoxiucardAvator;
		case PREVIEW:
		case MYPREVIEW:
			default:
				return mDefaultBitmap; 
		}
	}
	
	class AvatarDrawable extends ColorDrawable {
		 
        private final WeakReference<AvatorAsyncTask> avatarTaskReference;
 
        public AvatarDrawable(AvatorAsyncTask avatarAsyncTask) {
            super(Color.WHITE);
            avatarTaskReference = new WeakReference<AvatorAsyncTask>(avatarAsyncTask);
        }
 
        public AvatorAsyncTask getAvatorAsyncTask() {
            return avatarTaskReference.get();
        }
    }
	
	class AvatarBitmapDrawable extends BitmapDrawable {
		 
        private final WeakReference<AvatorAsyncTask> avatarTaskReference;
        private TaskType taskType;
 
        public AvatarBitmapDrawable(AvatorAsyncTask avatarAsyncTask, TaskType taskType) {
        	super(PhotoManagerUtilsV2.this.getDefaultBitmap(taskType));
            avatarTaskReference = new WeakReference<AvatorAsyncTask>(avatarAsyncTask);
            this.taskType = taskType;
            
        }
 
        public AvatorAsyncTask getAvatorAsyncTask() {
            return avatarTaskReference.get();
        }

		@Override
		public void draw(Canvas canvas) {
			super.draw(canvas);
		}
        
        
    }
	
	 public static AvatorAsyncTask getAvatorAsyncTask(ImageView imageView) {
	        if (imageView != null) {
	            Drawable drawable = imageView.getDrawable();
	            if (drawable == null) {
	            	return null;
	            }
	            if (drawable instanceof AvatarDrawable) {
	            	AvatarDrawable avatorDrawable = (AvatarDrawable) drawable;
	                return avatorDrawable.getAvatorAsyncTask();
	            } else if (drawable instanceof AvatarBitmapDrawable) {
	            	AvatarBitmapDrawable avatorDrawable = (AvatarBitmapDrawable) drawable;
	                return avatorDrawable.getAvatorAsyncTask();
	            }
	        }
	        return null;
	 }
 
    public static boolean cancelPotentialDownload(String photoId, ImageView imageView) {
    	AvatorAsyncTask avatarAsyncTask = getAvatorAsyncTask(imageView);
 
        if (avatarAsyncTask != null) {
            if ( !photoId.equals(avatarAsyncTask.mPhotoId) ) {
            	DebugUtils.logPhotoUtils(TAG, "cancel existed unfinished AvatorAsyncTask for photoId " + photoId);
            	avatarAsyncTask.cancel(true);
            } else {
                // The same URL is already being downloaded.
                return false;
            }
        }
        return true;
    }
    
    public static Drawable getDrawable(String photoId, TaskType type, Context context) {
        Bitmap avatar = getBitmapFromCache(photoId, type);
        if (avatar != null) {
            return new BitmapDrawable(Bitmap.createScaledBitmap(avatar, 30, 30, false));
        }
        return null;
    }
    /**异步载入图片，可能会需要从服务器上下载*/
	public void loadPhotoAsync(String token, ImageView imageView, String photoId, byte[] photo, TaskType type) {
		loadPhotoAsync(token, imageView, photoId, photo, type, false);
	}
    /**异步载入图片，可能会需要从服务器上下载*/
	public void loadPhotoAsync(String token, ImageView imageView, String photoId, byte[] photo, TaskType type, boolean notify) {
		if (cancelPotentialDownload(photoId, imageView)) {
            Bitmap avatar = getBitmapFromCache(photoId, type);
            if (avatar != null && imageView != null) {
                imageView.setImageBitmap(avatar);
                //通知监听器，图片已经加载完成了
                Bundle data = new Bundle();
	            data.putBoolean(EXTRA_DOWNLOAD_STATUS, true);
	            data.putString(EXTRA_DOWNLOAD_STATUS_MESSAGE, "get Bitmap fromcache");
	            data.putString(Intents.EXTRA_PHOTOID, photoId);
	            data.putString(Intents.EXTRA_TYPE, type.toString());
	            NotifyRegistrant.getInstance().notify(data);
            } else {
            	internalLoadPhotoAsync(token, imageView, photoId, type, photo, notify);
            }
	    }
	}
	/**异步载入本地图片文件*/
	public void loadLocalPhotoAsync(String token, ImageView imageView, String photoId, byte[] photo, TaskType type) {
		loadLocalPhotoAsync(token, imageView, photoId, photo, type, false);
	}
	/**异步载入本地图片文件*/
	public void loadLocalPhotoAsync(String token, ImageView imageView, String photoId, byte[] photo, TaskType type, boolean notify) {
		if (cancelPotentialDownload(photoId, imageView)) {
			
            Bitmap avatar = getBitmapFromCache(photoId, type);
            if (avatar != null && imageView != null) {
                imageView.setImageBitmap(avatar);
                //通知监听器，图片已经加载完成了
                Bundle data = new Bundle();
	            data.putBoolean(EXTRA_DOWNLOAD_STATUS, true);
	            data.putString(EXTRA_DOWNLOAD_STATUS_MESSAGE, "get Bitmap fromcache");
	            data.putString(Intents.EXTRA_PHOTOID, photoId);
	            data.putString(Intents.EXTRA_TYPE, type.toString());
	            NotifyRegistrant.getInstance().notify(data);
            } else {
            	internalLoadLocalPhotoAsync(token, imageView, photoId, type, photo, notify);
            	
            }
	    }
	}
	
	/**异步载入本地图片文件*/
	private void internalLoadPhotoAsync(String token, ImageView imageView, String photoId, TaskType type, byte[] photo, boolean notify) {
		DebugUtils.logPhotoUtils(TAG, "step 1 set default bitmap");
//		imageView.setImageBitmap(getDefaultBitmap(type));
		
		LoadPhotoAsyncTask loadPhotoTask = new LoadPhotoAsyncTask(imageView, token, photoId, type, photo, notify);
//		AvatarDrawable avatorDrawable = new AvatarDrawable(loadPhotoTask);
		AvatarBitmapDrawable avatorDrawable = new AvatarBitmapDrawable(loadPhotoTask, type);
        if (imageView != null) {
            imageView.setImageDrawable(avatorDrawable);
        }
		loadPhotoTask.execute();
	}
	/**异步载入本地图片文件*/
	private void internalLoadLocalPhotoAsync(String token, ImageView imageView, String photoId, TaskType type, byte[] photo, boolean notify) {
		DebugUtils.logPhotoUtils(TAG, "step 1 set default bitmap");
//		imageView.setImageBitmap(getDefaultBitmap(type));
		
		LoadLocalPhotoAsyncTask loadPhotoTask = new LoadLocalPhotoAsyncTask(imageView, token, photoId, type, photo, notify);
//		AvatarDrawable avatorDrawable = new AvatarDrawable(loadPhotoTask);
		AvatarBitmapDrawable avatorDrawable = new AvatarBitmapDrawable(loadPhotoTask, type);
        if (imageView != null) {
            imageView.setImageDrawable(avatorDrawable);
        }
		loadPhotoTask.execute();
	}
	
	
	public static PhotoManagerUtilsV2 getInstance() {
		return INSTANCE;
	}
	/***
	 * 如果存在映射，说明对于某一个PhotoId已经有下载任务在进行了，我们等待他完成就可以了
	 */
	private static HashSet<String> mDownloadingMap = new HashSet<String>();
	abstract class  AvatorAsyncTask extends AsyncTask<Void, Void, Bitmap> {
		protected String aToken;
		protected String mPhotoId;
		protected WeakReference<ImageView> imageViewReference;
		private boolean mNotify = false;
		protected TaskType mTaskType;
		
		public AvatorAsyncTask(ImageView imageView, String token, String photoId, TaskType type, boolean notify) {
			imageViewReference = new WeakReference<ImageView>(imageView);
			mPhotoId = photoId;
			aToken = token;
			mTaskType = type;
			mNotify = notify;
			addTask(aToken, this);
		}
		
		
		public void setTokenAndNo(String token, String photoId) {
			aToken = token;
			mPhotoId = photoId;
		}

		public String getToken() {
			return aToken;
		}
		public String getPhotoId() {
			return mPhotoId;
		}
		
		public boolean match(String token, String photoId) {
			return !TextUtils.isEmpty(aToken) && aToken.equals(token) ||
					!TextUtils.isEmpty(mPhotoId) && mPhotoId.equals(photoId) ||
					aToken == null && token==null;
		}
		
		protected void notifyStatus(boolean status, String message) {
			 //下载完通知photoid下载
			if (mNotify) {
				Bundle data = new Bundle();
	            data.putBoolean(EXTRA_DOWNLOAD_STATUS, status);
	            data.putString(EXTRA_DOWNLOAD_STATUS_MESSAGE, message);
	            data.putString(Intents.EXTRA_PHOTOID, mPhotoId);
	            data.putString(Intents.EXTRA_TYPE, mTaskType.toString());
	            NotifyRegistrant.getInstance().notify(data);
			}
		}
		
		@Override
		protected Bitmap doInBackground(Void... arg0) {
			try {
				synchronized(mDownloadingMap) {
					while (!isCancelled() && mDownloadingMap.contains(mPhotoId)) {
						DebugUtils.logD(TAG, "other task is running with the same photoID=" + mPhotoId + ", so just wait.......");
						mDownloadingMap.wait();
					}
					DebugUtils.logD(TAG, "current task add into DownloadingMap for photoID=" + mPhotoId);
					mDownloadingMap.add(mPhotoId);
				}
			} catch (InterruptedException e) {
				DebugUtils.logD(TAG, "current task is Interrupted for photoID=" + mPhotoId);
				e.printStackTrace();
				notifyStatus(false, e.getMessage());
				return null;
			}
			if (isCancelled()) {
				DebugUtils.logD(TAG, "current task is canceled with the photoID=" + mPhotoId);
				return null;
			}
			return null;
		}
		@Override
		protected void onCancelled() {
			super.onCancelled();
			removeTask(aToken, this);
			notifyStatus(false, "onCancelled()");
			synchronized(mDownloadingMap) {
				if (mDownloadingMap.contains(mPhotoId)) {
					boolean removed = mDownloadingMap.remove(mPhotoId);
					DebugUtils.logD(TAG, "Task finish by canceled [in onCancelled()] for photoID=" + mPhotoId + ", remove PhotoId from mDownloadingMap, removed=" + removed);
				}
				mDownloadingMap.notifyAll();
			}
		}

		@Override
		protected void onPostExecute(Bitmap bitmap) {
			super.onPostExecute(bitmap);
			if (bitmap != null) {
				 if (imageViewReference != null) {
		                ImageView imageView = imageViewReference.get();
		                AvatorAsyncTask avatarAsyncTask = getAvatorAsyncTask(imageView);
		                if (this == avatarAsyncTask && imageView != null) {
		                	DebugUtils.logPhotoUtils(TAG, "setImageBitmap for photoId " + mPhotoId);
		                    imageView.setImageBitmap(bitmap);
		                }
		            }
				 notifyStatus(true, "");
            }
            addBitmapToCache(mPhotoId, mTaskType, bitmap);
			removeTask(aToken, this);
			synchronized(mDownloadingMap) {
				if (mDownloadingMap.contains(mPhotoId)) {
					boolean removed = mDownloadingMap.remove(mPhotoId);
					DebugUtils.logD(TAG, "Task finished for photoID=" + mPhotoId + ", remove PhotoId from mDownloadingMap, removed=" + removed);
				}
				mDownloadingMap.notifyAll();
			}
		}
		
	}
	
	public static File getFileToSave(TaskType type, String photoId) {
		switch(type) {
		case HOME_DEVICE_AVATOR:
			return MyApplication.getInstance().getProductPreviewAvatorFile(photoId);
		case FaPiao:
			return MyApplication.getInstance().getProductFaPiaoFile(photoId);
		case Baoxiucard_Salesman_Avator:
			return MyApplication.getInstance().getCachedPreviewAvatorFile(photoId);
		case MYPREVIEW:
			return MyApplication.getInstance().getAccountCardAvatorFile(photoId);
		case PREVIEW:
			return MyApplication.getInstance().getCachedPreviewAvatorFile(photoId);
		}
		return null;
	}
	
	public static String getServiceUrl(TaskType type, String photoId) {
		switch(type) {
		case PREVIEW:
		case MYPREVIEW:
			return HaierServiceObject.getRelationshipAvatorUrl(photoId);
//			return Contents.MingDang.buildAvatorUrl(photoId);
		case HOME_DEVICE_AVATOR:
			return HaierServiceObject.getProdcutAvatorUrl(photoId);
		case FaPiao:
			return HaierServiceObject.getBaoxiucardFapiao(photoId);
		case Baoxiucard_Salesman_Avator:
			return HaierServiceObject.getBaoxiucardSalesmanAvatorPreview(photoId);
		}
		return null;
	}
	
	class LoadPhotoAsyncTask extends AvatorAsyncTask {
		private byte[] lPhoto;
		private String mServiceUrl = null;
		
		public LoadPhotoAsyncTask(ImageView imageView, String token, String photoId, TaskType type, byte[] photo, boolean notify) {
			super(imageView, token, photoId, type, notify);
			lPhoto = photo;
		}
		
		private File getFileToSave() {
			return PhotoManagerUtilsV2.getFileToSave(mTaskType, mPhotoId);
		}
		
		private String getServiceUrl() {
			if (mServiceUrl == null) {
				mServiceUrl = PhotoManagerUtilsV2.getServiceUrl(mTaskType, mPhotoId);
			}
			return mServiceUrl;
		}

		@Override
		protected Bitmap doInBackground(Void... params) {
			super.doInBackground(params);
			InputStream is = null;
			Bitmap bitmap = null;
			File cachedBitmapFile = getFileToSave();
			if (cachedBitmapFile == null) {
				Log.e(TAG, "error, LoadPhotoAsyncTask call getFileToSave() which returns null for " + mTaskType.toString());
				notifyStatus(false, "Can't access cachedBitmapFile for photoid="+mPhotoId);
				return null;
			}
			DebugUtils.logPhotoUtils(TAG, "step 2 try to get avator from cached file " + cachedBitmapFile.getAbsolutePath());
			bitmap  = decodeFromCachedBitmapFile(cachedBitmapFile, mTaskType);
		    if (bitmap == null && lPhoto != null) {
				DebugUtils.logPhotoUtils(TAG, "step 3 try to get avator from supplied byte array");
				bitmap = decodeByteArray(cachedBitmapFile, lPhoto, mTaskType);
			} else if (lPhoto == null ) {
				DebugUtils.logPhotoUtils(TAG, "skip step 3 that try to get avator from supplied null byte array");
			}
		    if (this.isCancelled()) {
				if (bitmap != null) {
					bitmap.recycle();
					DebugUtils.logPhotoUtils(TAG, "bitmap.recycle() in bg1 for id " + mPhotoId);
				}
				return null;
			}
			if (bitmap == null) {
				String url = getServiceUrl();
				try {
					DebugUtils.logPhotoUtils(TAG, "step 4 download bitmap");
					HttpResponse respose = NetworkUtils.openContectionLockedV2(url, MyApplication.getInstance().getSecurityKeyValuesObject());
					if (respose.getStatusLine().getStatusCode() == HttpStatus.SC_NOT_FOUND) {
						DebugUtils.logPhotoUtils(TAG, "download bitmap failed, can't find image on server-side for photoid " + mPhotoId);
						notifyStatus(false, MyApplication.getInstance().getString(R.string.msg_no_existed_photo_in_service));
					    return null;
					}
					is = respose.getEntity().getContent();
					if (is != null) {
						DebugUtils.logPhotoUtils(TAG, "step 5 create the mm.p file using bitmap");
						createCachedBitmapFile(is, cachedBitmapFile);
						DebugUtils.logPhotoUtils(TAG, "step 6 try to get avator from cached mm.p file");
						bitmap = decodeFromCachedBitmapFile(cachedBitmapFile, mTaskType);
					}
				} catch (ClientProtocolException e) {
					e.printStackTrace();
					notifyStatus(false, e.getMessage());
				} catch (IOException e) {
					e.printStackTrace();
					notifyStatus(false, MyApplication.getInstance().getGernalNetworkError());
				} finally {
					DebugUtils.logPhotoUtils(TAG, "finally() for path="+url + ", is=" + is + ", bitmap="+bitmap);
					NetworkUtils.closeInputStream(is);
				}
			}
			if (this.isCancelled()) {
				if (bitmap != null)  {
					bitmap.recycle();
					bitmap = null;
					DebugUtils.logPhotoUtils(TAG, "bitmap.recycle() in bg2 for id " + mPhotoId);
				}
			}
			return bitmap;
		}
		
	}
	
	
	class LoadLocalPhotoAsyncTask extends AvatorAsyncTask {
		private byte[] lPhoto;
		
		public LoadLocalPhotoAsyncTask(ImageView imageView, String token, String photoId, TaskType type, byte[] photo, boolean notify) {
			super(imageView, token, photoId, type, notify);
			lPhoto = photo;
		}
		
		private File getFileToSave() {
			return PhotoManagerUtilsV2.getFileToSave(mTaskType, mPhotoId);
		}
		

		@Override
		protected Bitmap doInBackground(Void... params) {
			Bitmap bitmap = null;
			File cachedBitmapFile = getFileToSave();
			if (cachedBitmapFile == null) {
				Log.e(TAG, "error, LoadLocalPhotoAsyncTask call getFileToSave() which returns null for " + mTaskType.toString());
				return null;
			}
			DebugUtils.logPhotoUtils(TAG, "step 2 try to get avator from cached file " + cachedBitmapFile.getAbsolutePath());
			bitmap  = decodeFromCachedBitmapFile(cachedBitmapFile, mTaskType);
		    if (bitmap == null && lPhoto != null) {
				DebugUtils.logPhotoUtils(TAG, "step 3 try to get avator from supplied byte array");
				bitmap = decodeByteArray(cachedBitmapFile, lPhoto, mTaskType);
			} else if (lPhoto == null ) {
				DebugUtils.logPhotoUtils(TAG, "skip step 3 that try to get avator from supplied null byte array");
			}
		    if (this.isCancelled()) {
				if (bitmap != null) {
					bitmap.recycle();
					DebugUtils.logPhotoUtils(TAG, "bitmap.recycle() in bg1 for id " + mPhotoId);
				}
				return null;
			}
			return bitmap;
		}
		
	}
	
	
	public enum TaskType {
		PREVIEW("PreviewVcfType"),
		/**我的名片头像*/
		MYPREVIEW("MyPreviewVcfType"), 
		FaPiao("FaPiao"),
		HOME_DEVICE_AVATOR("HomeDeviceAvatorType"),  //设备avator
		Baoxiucard_Salesman_Avator("Baoxiucard_Salesman_Avator");
		private String mTypeName;
		TaskType(String typeName) {
			mTypeName=typeName;
		}
		
		public String toString() {
			return mTypeName;
		}
	}
}
