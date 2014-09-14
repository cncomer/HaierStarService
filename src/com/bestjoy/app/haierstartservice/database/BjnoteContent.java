package com.bestjoy.app.haierstartservice.database;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;

public class BjnoteContent {

	public static final String AUTHORITY = "com.bestjoy.app.haierstartservice.provider.BjnoteProvider";
    // The notifier authority is used to send notifications regarding changes to messages (insert,
    // delete, or update) and is intended as an optimization for use by clients of message list
    // cursors (initially, the email AppWidget).
    public static final String NOTIFIER_AUTHORITY = "com.bestjoy.app.haierstartservice.notify.BjnoteProvider";

    public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY);
    
    public static final String DEVICE_AUTHORITY = "com.bestjoy.app.haierstartservice.provider.DeviceProvider";
    public static final String DEVICE_NOTIFIER_AUTHORITY = "com.bestjoy.app.haierstartservice.notify.DeviceProvider";
    public static final Uri DEVICE_CONTENT_URI = Uri.parse("content://" + DEVICE_AUTHORITY);
    
    // All classes share this
    public static final String RECORD_ID = "_id";

    public static final String[] COUNT_COLUMNS = new String[]{"count(*)"};

    /**
     * This projection can be used with any of the EmailContent classes, when all you need
     * is a list of id's.  Use ID_PROJECTION_COLUMN to access the row data.
     */
    public static final String[] ID_PROJECTION = new String[] {
        RECORD_ID
    };
    public static final int ID_PROJECTION_COLUMN = 0;

    public static final String ID_SELECTION = RECORD_ID + " =?";
    
    
    public static class Accounts extends BjnoteContent{
    	public static final Uri CONTENT_URI = Uri.withAppendedPath(BjnoteContent.CONTENT_URI, "accounts");
    }
    
    public static class Homes extends BjnoteContent{
    	public static final Uri CONTENT_URI = Uri.withAppendedPath(BjnoteContent.CONTENT_URI, "homes");
    }
    /**我的保修卡设备*/
    public static class BaoxiuCard extends BjnoteContent{
    	public static final Uri CONTENT_URI = Uri.withAppendedPath(BjnoteContent.CONTENT_URI, "baoxiucard");
    	public static final Uri BILL_CONTENT_URI = Uri.withAppendedPath(BjnoteContent.CONTENT_URI, "baoxiucard/preview/bill");
    }
    
    public static class DaLei extends BjnoteContent{
    	public static final Uri CONTENT_URI = Uri.withAppendedPath(BjnoteContent.DEVICE_CONTENT_URI, "dalei");
    }
    
    public static class XiaoLei extends BjnoteContent{
    	public static final Uri CONTENT_URI = Uri.withAppendedPath(BjnoteContent.DEVICE_CONTENT_URI, "xiaolei");
    }
    
    public static class PinPai extends BjnoteContent{
    	public static final Uri CONTENT_URI = Uri.withAppendedPath(BjnoteContent.DEVICE_CONTENT_URI, "pinpai");
    }
    
    public static class XingHao extends BjnoteContent{
    	public static final Uri CONTENT_URI = Uri.withAppendedPath(BjnoteContent.CONTENT_URI, "xinghao");
    }
    
    public static class Province extends BjnoteContent{
    	public static final Uri CONTENT_URI = Uri.withAppendedPath(BjnoteContent.DEVICE_CONTENT_URI, "province");
    }
    
    public static class City extends BjnoteContent{
    	public static final Uri CONTENT_URI = Uri.withAppendedPath(BjnoteContent.DEVICE_CONTENT_URI, "city");
    }
    
    public static class District extends BjnoteContent{
    	public static final Uri CONTENT_URI = Uri.withAppendedPath(BjnoteContent.DEVICE_CONTENT_URI, "district");
    }
    
    public static class ScanHistory extends BjnoteContent{
    	public static final Uri CONTENT_URI = Uri.withAppendedPath(BjnoteContent.CONTENT_URI, "scan_history");
    }
    
    public static class HaierRegion extends BjnoteContent{
    	public static final Uri CONTENT_URI = Uri.withAppendedPath(BjnoteContent.DEVICE_CONTENT_URI, "haierregion");
    }
    
    public static class YMESSAGE extends BjnoteContent{
    	public static final Uri CONTENT_URI = Uri.withAppendedPath(BjnoteContent.CONTENT_URI, "ymessage");
    	
    	public static String[] PROJECTION = new String[]{
    		HaierDBHelper.ID,
    		HaierDBHelper.YOUMENG_MESSAGE_ID,
    		HaierDBHelper.YOUMENG_TITLE,
    		HaierDBHelper.YOUMENG_TEXT,
    		HaierDBHelper.YOUMENG_MESSAGE_ACTIVITY,
    		HaierDBHelper.YOUMENG_MESSAGE_URL,
    		HaierDBHelper.YOUMENG_MESSAGE_CUSTOM,
    		HaierDBHelper.YOUMENG_MESSAGE_RAW, 
    		HaierDBHelper.DATE,
    	};
    	
    	public static final int INDEX_ID = 0;
    	public static final int INDEX_MESSAGE_ID = 1;
    	public static final int INDEX_TITLE = 2;
    	public static final int INDEX_TEXT = 3;
    	public static final int INDEX_MESSAGE_ACTIVITY = 4;
    	public static final int INDEX_MESSAGE_URL = 5;
    	public static final int INDEX_MESSAGE_CUSTOM = 6;
    	public static final int INDEX_MESSAGE_RAW = 7;
    	public static final int INDEX_DATE = 8;
    	
    	public static final String WHERE_YMESSAGE_ID = HaierDBHelper.YOUMENG_MESSAGE_ID + "=?";
    }
    
    /**调用该类的CONTENT_URI来关闭设备数据库*/
    public static class CloseDeviceDatabase extends BjnoteContent{
    	private static final Uri CONTENT_URI = Uri.withAppendedPath(BjnoteContent.DEVICE_CONTENT_URI, "closedevice");
    	/**调用该方法来关闭设备数据库*/
    	public static void closeDeviceDatabase(ContentResolver cr) {
    		cr.query(CONTENT_URI, null, null, null, null);
    	}
    }
    
    public static class IM extends BjnoteContent{
    	public static final Uri CONTENT_URI = Uri.withAppendedPath(BjnoteContent.CONTENT_URI, "im/qun");
    	public static final Uri CONTENT_URI_QUN = Uri.withAppendedPath(BjnoteContent.CONTENT_URI, "im/qun");
    	public static final Uri CONTENT_URI_FRIEND = Uri.withAppendedPath(BjnoteContent.CONTENT_URI, "im/friend");
    }
    
    public static class RELATIONSHIP extends BjnoteContent{
    	public static final Uri CONTENT_URI = Uri.withAppendedPath(BjnoteContent.CONTENT_URI, "relationship");
    	public static final String UID_SELECTION = HaierDBHelper.RELATIONSHIP_UID + "=?";
    	public static final String SORT_BY_ID = HaierDBHelper.ID + " asc";
    	public static final String[] RELATIONSHIP_PROJECTION = new String[]{
    		HaierDBHelper.ID,              //0
    		HaierDBHelper.RELATIONSHIP_SERVICE_ID,   //1
    		HaierDBHelper.RELATIONSHIP_TYPE,  //2
    		HaierDBHelper.RELATIONSHIP_TARGET,       //3
    		HaierDBHelper.RELATIONSHIP_UID,         //4
    		HaierDBHelper.RELATIONSHIP_NAME,           //5
    		HaierDBHelper.DATA1,         //6
    		HaierDBHelper.DATA2,  //7
    		HaierDBHelper.DATA3,  //8
    		HaierDBHelper.DATA4,  //9
    		HaierDBHelper.DATE,             //10
    	};
    	public static final int INDEX_RELASTIONSHIP_ID = 0;
    	public static final int INDEX_RELASTIONSHIP_SERVICE_ID = 1;
    	public static final int INDEX_RELASTIONSHIP_TARGET_TYPE = 2;
    	public static final int INDEX_RELASTIONSHIP_TARGET = 3;
    	public static final int INDEX_RELASTIONSHIP_UID = 4;
    	public static final int INDEX_RELASTIONSHIP_UNAME = 5;
    	public static final int INDEX_RELASTIONSHIP_LEIXING = 6;
    	public static final int INDEX_RELASTIONSHIP_XINGHAO = 7;
    	public static final int INDEX_RELASTIONSHIP_CELL = 8;
    	public static final int INDEX_RELASTIONSHIP_BUYDATE = 9;
    	public static final int INDEX_RELASTIONSHIP_LOCAL_DATE = 10;
    	/**返回我的全部关系*/
    	public static Cursor getAllRelationships(ContentResolver cr, String uid) {
    		return cr.query(BjnoteContent.RELATIONSHIP.CONTENT_URI, RELATIONSHIP_PROJECTION, UID_SELECTION, new String[]{uid}, SORT_BY_ID);
    	}
    	
    }
    
    public static long existed(ContentResolver cr, Uri uri, String where, String[] selectionArgs) {
    	long id = -1;
		Cursor c = cr.query(uri, ID_PROJECTION, where, selectionArgs, null);
		if (c != null) {
			if (c.moveToNext()) {
				id = c.getLong(0);
			}
			c.close();
		}
		return id;
	}
	
	public static int update(ContentResolver cr, Uri uri, ContentValues values, String where, String[] selectionArgs) {
		return cr.update(uri, values, where, selectionArgs);
	}
	
	public static Uri insert(ContentResolver cr, Uri uri, ContentValues values) {
		return cr.insert(uri, values);
	}
	
	public static int delete(ContentResolver cr, Uri uri,  String where, String[] selectionArgs) {
		return cr.delete(uri, where, selectionArgs);
	}
}
