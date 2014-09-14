package com.bestjoy.app.haierstartservice.database;


import java.io.FileNotFoundException;

import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.content.res.AssetFileDescriptor;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.text.TextUtils;

import com.shwy.bestjoy.utils.DebugUtils;

public class DeviceProvider extends ContentProvider{
	private static final String TAG = "DeviceProvider";
	private SQLiteDatabase mContactDatabase;
	private String[] mTables = new String[]{
			DeviceDBHelper.TABLE_NAME_DEVICE_DALEI,
			DeviceDBHelper.TABLE_NAME_DEVICE_XIAOLEI,
			DeviceDBHelper.TABLE_NAME_DEVICE_PINPAI,
			DeviceDBHelper.TABLE_NAME_DEVICE_PROVINCE,
			DeviceDBHelper.TABLE_NAME_DEVICE_CITY,
			DeviceDBHelper.TABLE_NAME_DEVICE_DISTRICT,
//			ContactsDBHelper.TABLE_NAME_MYLIFE_CONSUME,
			DeviceDBHelper.TABLE_NAME_DEVICE_HAIERREGION,
	};
	private static final int BASE = 8;
	
	private static final int DALEI = 0x0000;
	private static final int DALEI_ID = 0x0001;
	
	private static final int XIAOLEI = 0x0100;
	private static final int XIAOLEI_ID = 0x0101;
	
	private static final int PINPAI = 0x0200;
	private static final int PINPAI_ID = 0x0201;
	
	private static final int PROVINCE = 0x0300;
	private static final int PROVINCE_ID = 0x0301;

	private static final int CITY = 0x0400;
	private static final int CITY_ID = 0x0401;

	private static final int DISTRICT = 0x0500;
	private static final int DISTRICT_ID = 0x0501;

	private static final int HAIER_REGION = 0x0600;
	private static final int HAIER_REGION_CODE = 0x0601;
	private static final int HAIER_COUNTROY = 0x0602;
	private static final int HAIER_PROVINCE = 0x0603;
	private static final int HAIER_CITY = 0x0604;
	private static final int HAIER_REGION_NAME = 0x0605;
	private static final int HAIER_ADMIN_CODE = 0x0606;
	private static final int HAIER_PRO_CODE = 0x0607;
	private static final int HAIER_CITY_CODE = 0x0608;
	private static final int HAIER_AREA_CODE = 0x0609;
	private static final int HAIER_UPDATE_TIME = 0x0610;
	private static final int HAIER_POST_CODE = 0x0611;
	
	/**关闭设备数据库*/
	private static final int CLOSE_DEVICE = 0x0612;
	
	private static final UriMatcher sURIMatcher = new UriMatcher(UriMatcher.NO_MATCH);
	 static {
	        // URI matching table
	        UriMatcher matcher = sURIMatcher;

	        matcher.addURI(BjnoteContent.DEVICE_AUTHORITY, "dalei", DALEI);
	        matcher.addURI(BjnoteContent.DEVICE_AUTHORITY, "dalei/#", DALEI_ID);
	        
	        matcher.addURI(BjnoteContent.DEVICE_AUTHORITY, "xiaolei", XIAOLEI);
	        matcher.addURI(BjnoteContent.DEVICE_AUTHORITY, "xiaolei/#", XIAOLEI_ID);
	        
	        matcher.addURI(BjnoteContent.DEVICE_AUTHORITY, "pinpai", PINPAI);
	        matcher.addURI(BjnoteContent.DEVICE_AUTHORITY, "pinpai/#", PINPAI_ID);

	        matcher.addURI(BjnoteContent.DEVICE_AUTHORITY, "province", PROVINCE);
	        matcher.addURI(BjnoteContent.DEVICE_AUTHORITY, "province/#", PROVINCE_ID);

	        matcher.addURI(BjnoteContent.DEVICE_AUTHORITY, "city", CITY);
	        matcher.addURI(BjnoteContent.DEVICE_AUTHORITY, "city/#", CITY_ID);

	        matcher.addURI(BjnoteContent.DEVICE_AUTHORITY, "district", DISTRICT);
	        matcher.addURI(BjnoteContent.DEVICE_AUTHORITY, "district/#", DISTRICT_ID);

	        matcher.addURI(BjnoteContent.DEVICE_AUTHORITY, "haierregion", HAIER_REGION);
	        matcher.addURI(BjnoteContent.DEVICE_AUTHORITY, "haierregion/#", HAIER_REGION_CODE);
	        
	        matcher.addURI(BjnoteContent.DEVICE_AUTHORITY, "closedevice", CLOSE_DEVICE);
	        
	        //TODO 增加
	 }
	
	synchronized SQLiteDatabase getDatabase(Context context) {
        // Always return the cached database, if we've got one
        if (mContactDatabase != null) {
            return mContactDatabase;
        }

        mContactDatabase = SQLiteDatabase.openOrCreateDatabase(context.getDatabasePath(DeviceDBHelper.DB_DEVICE_NAME), null);
        mContactDatabase.setLockingEnabled(true);
        return mContactDatabase;
	}
	
	synchronized void closeDatabase() {
		 if (mContactDatabase != null && mContactDatabase.isOpen()) {
			 DebugUtils.logD(TAG, "close device Database");
			 mContactDatabase.close();
			 mContactDatabase = null;
	     }
	}
	
	@Override
	public boolean onCreate() {
		return false;
	}
	
	/**
     * Wrap the UriMatcher call so we can throw a runtime exception if an unknown Uri is passed in
     * @param uri the Uri to match
     * @return the match value
     */
    private static int findMatch(Uri uri, String methodName) {
        int match = sURIMatcher.match(uri);
        if (match < 0) {
            throw new IllegalArgumentException("Unknown uri: " + uri);
        } 
        DebugUtils.logD(TAG, methodName + ": uri=" + uri + ", match is " + match);
        return match;
    }
    
    private void notifyChange(int match) {
    	Context context = getContext();
    	Uri notify = BjnoteContent.DEVICE_CONTENT_URI;
    	switch(match) {
    	case DALEI:
    	case DALEI_ID:
    		notify = BjnoteContent.DaLei.CONTENT_URI;
    		break;
		case XIAOLEI:
		case XIAOLEI_ID:
			notify = BjnoteContent.XiaoLei.CONTENT_URI;
			break;
		case PINPAI:
		case PINPAI_ID:
			notify = BjnoteContent.PinPai.CONTENT_URI;
			break;
		case PROVINCE_ID:
			notify = BjnoteContent.Province.CONTENT_URI;
			break;
		case CITY_ID:
			notify = BjnoteContent.City.CONTENT_URI;
			break;
		case DISTRICT_ID:
			notify = BjnoteContent.District.CONTENT_URI;
			break;
		case HAIER_REGION:
		case HAIER_REGION_CODE:
			notify = BjnoteContent.HaierRegion.CONTENT_URI;
			break;
    	}
    	ContentResolver resolver = context.getContentResolver();
        resolver.notifyChange(notify, null);
    }

	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		int match = findMatch(uri, "delete");
        Context context = getContext();

        // See the comment at delete(), above
        SQLiteDatabase db = getDatabase(context);
        String table = mTables[match>>BASE];
        DebugUtils.logProvider(TAG, "delete data from table " + table);
        int count = 0;
        switch(match) {
	        case DALEI:
	    	case DALEI_ID:
			case XIAOLEI:
			case XIAOLEI_ID:
			case PINPAI:
			case PINPAI_ID:
        	count = db.delete(table, buildSelection(match, uri, selection), selectionArgs);
        }
        if (count >0) notifyChange(match);
		return count;
	}

	@Override
	public String getType(Uri uri) {
		return null;
	}

	@Override
	public Uri insert(Uri uri, ContentValues values) {
		 int match = findMatch(uri, "insert");
		 String table = mTables[match>>BASE];
		 switch(match) {
		 	case DALEI:
	    	case DALEI_ID:
			case XIAOLEI:
			case XIAOLEI_ID:
			case PINPAI:
			case PINPAI_ID:
				//不支持新增更新操作
				DebugUtils.logProvider(TAG, "ignore insert values into table " + table);
				return null;
		 }
         Context context = getContext();
         // See the comment at delete(), above
         SQLiteDatabase db = getDatabase(context);
         DebugUtils.logProvider(TAG, "insert values into table " + table);
         //Insert 操作不允许设置_id字段，如果有的话，我们需要移除
         if (values.containsKey(HaierDBHelper.ID)) {
      		values.remove(HaierDBHelper.ID);
      	 }
     	 long id = db.insert(table, null, values);
     	 if (id > 0) {
     		notifyChange(match);
   		    return ContentUris.withAppendedId(uri, id);
     	 }
		 return null;
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection,
			String[] selectionArgs, String sortOrder) {
		 int match = findMatch(uri, "query");
         Context context = getContext();
         // See the comment at delete(), above
         SQLiteDatabase db = getDatabase(context);
         String table = mTables[match>>BASE];
         DebugUtils.logProvider(TAG, "query table " + table);
         Cursor result = null;
         switch(match) {
         	case DALEI:
         	case DALEI_ID:
         	case XIAOLEI:
         	case XIAOLEI_ID:
         	case PINPAI:
         	case PINPAI_ID:
			case PROVINCE:
			case PROVINCE_ID:
			case CITY:
			case CITY_ID:
			case DISTRICT:
			case DISTRICT_ID:
			case HAIER_REGION:
			case HAIER_REGION_CODE:
        	     result = db.query(table, projection, selection, selectionArgs, null, null, sortOrder);
        	     break;
			case CLOSE_DEVICE:
				closeDatabase();
				break;
         }
		return result;
	}

	@Override
	public int update(Uri uri, ContentValues values, String selection,
			String[] selectionArgs) {
		int match = findMatch(uri, "update");
        String table = mTables[match>>BASE];
        int count = 0;
        switch(match) {
	 	case DALEI:
    	case DALEI_ID:
		case XIAOLEI:
		case XIAOLEI_ID:
		case PINPAI:
		case PINPAI_ID:
			//不支持新增更新操作
			DebugUtils.logProvider(TAG, "ignore update values into table " + table);
			return 0;
        }
        Context context = getContext();
        SQLiteDatabase db = getDatabase(context);
        DebugUtils.logProvider(TAG, "update data for table " + table);
        count = db.update(table, values, buildSelection(match, uri, selection), selectionArgs);
        if (count >0) notifyChange(match);
		return count;
	}
	
	private String buildSelection(int match, Uri uri, String selection) {
		long id = -1;
		switch(match) {
	    	case DALEI_ID:
			case XIAOLEI_ID:
			case PINPAI_ID:
			try {
				id = ContentUris.parseId(uri);
			} catch(java.lang.NumberFormatException e) {
				e.printStackTrace();
			}
			break;
		}
		
		if (id == -1) {
			return selection;
		}
		DebugUtils.logProvider(TAG, "find id from Uri#" + id);
		StringBuilder sb = new StringBuilder();
		sb.append(HaierDBHelper.ID);
		sb.append("=").append(id);
		if (!TextUtils.isEmpty(selection)) {
			sb.append(" and ");
			sb.append(selection);
		}
		DebugUtils.logProvider(TAG, "rebuild selection#" + sb.toString());
		return sb.toString();
	}

	@Override
	public ParcelFileDescriptor openFile(Uri uri, String mode)
			throws FileNotFoundException {
		int match = findMatch(uri, "openFile");
		//这里用来打开私有文件，通过这样的方式，其他程序可以读取URI指向的文件
		return super.openFile(uri, mode);
	}

	@Override
	public AssetFileDescriptor openAssetFile(Uri uri, String mode)
			throws FileNotFoundException {
		return super.openAssetFile(uri, mode);
	}

	
	
}
