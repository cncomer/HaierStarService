package com.bestjoy.app.utils;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

public class BitmapUtils {
	
	private static BitmapUtils mInstance = new BitmapUtils();
	private Context mContext;
	
	private BitmapUtils(){};
	
	public static BitmapUtils getInstance() {
		return mInstance;
	}
	
	public void setContext(Context context) {
		mContext = context;
	}

	public static Bitmap[] getSuitedBitmaps(Context context, int[] resIds, int w, int h) {
		BitmapFactory.Options options = new BitmapFactory.Options();
		Bitmap[] bitmaps = new Bitmap[resIds.length];
		Resources res = context.getResources();
		int index = 0;
		for(int id:resIds) {
			// Decode image bounds
			options.inJustDecodeBounds = true;
			BitmapFactory.decodeResource(res, id, options);
			int sampleSize = 1;
			if (options.outHeight > options.outWidth) {
				sampleSize = options.outHeight / h;
			} else {
				sampleSize = options.outWidth / w;
			}
			options.inJustDecodeBounds = false;
			options.inSampleSize = sampleSize;
			bitmaps[index] = BitmapFactory.decodeResource(res, id, options);
			index++;
		}
		
		return bitmaps;
	}
}
