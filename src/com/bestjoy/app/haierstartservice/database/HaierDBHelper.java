package com.bestjoy.app.haierstartservice.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.shwy.bestjoy.utils.DebugUtils;

/**
 * @author Sean Owen
 * @author chenkai
 */
public final class HaierDBHelper extends SQLiteOpenHelper {
private static final String TAG = "HaierDBHelper";
  private static final int DB_VERSION = 3;
  private static final String DB_NAME = "haierstartservice.db";
  public static final String ID = "_id";
 
  public static final String FLAG_DELETED = "deleted";
  public static final String DATE = "date";
  //account table
  public static final String TABLE_NAME_ACCOUNTS = "accounts";
  /**用户唯一识别码*/
  public static final String ACCOUNT_UID = "uid";
  public static final String ACCOUNT_DEFAULT = "isDefault";
  public static final String ACCOUNT_TEL = "tel";
  public static final String ACCOUNT_NAME = "name";
  public static final String ACCOUNT_PWD = "password";
  public static final String ACCOUNT_HOME_COUNT = "home_count";

  public static final String ACCOUNT_PHONES = "phones";

  public static final String ACCOUNT_HAS_PHOTO = "hasPhoto";
  
  public static final String ACCOUNT_TITLE = "title";
  public static final String ACCOUNT_ORG = "org";
  public static final String ACCOUNT_MM = "mm";
  public static final String ACCOUNT_WORKADDRESS = "workaddress";
  
  //home table
  public static final String TABLE_NAME_HOMES = "homes";
  /**地址id,每个地址的id,这个目前没用,要是更改地址的话可能会用到*/
  public static final String HOME_AID = "aid";
  public static final String HOME_NAME = "name";
  /**详细地址*/
  public static final String HOME_DETAIL = "home_detail";
  public static final String HOME_DEFAULT = "isDefault";
  /**我的家的保修卡个数*/
  public static final String HOME_CARD_COUNT = "baoxiucard_count";
  /**我的家TAB位置,用户可以调整顺序*/
  public static final String POSITION = "position";
  
  //cards table
  public static final String TABLE_NAME_CARDS = "cards";
  /**所属家*/
  public static final String CARD_AID = "aid";
  /**所属账户*/
  public static final String CARD_UID = "uid";
  /**保修卡服务器id*/
  public static final String CARD_BID = "bid";
  /**名称*/
  public static final String CARD_NAME = "name";
  /**设备类别，比如大类是电视剧*/
  public static final String CARD_TYPE = "LeiXin";
  /**品牌*/
  public static final String CARD_PINPAI = "PinPai";
  /**商品编号*/
  public static final String CARD_SERIAL = "SHBianHao";
  /**型号*/
  public static final String CARD_MODEL = "XingHao";
  /**保修电话*/
  public static final String CARD_BXPhone = "BXPhone";
  /**发票文件名*/
  public static final String CARD_FPname = "FPname";
  /**发票绝对路径*/
  public static final String CARD_FPaddr = "FPaddr";
  /**购买价格*/
  public static final String CARD_PRICE = "BuyPrice";
  /**购买日期*/
  public static final String CARD_BUT_DATE = "BuyDate";
  /**购买途径*/
  public static final String CARD_BUY_TUJING = "BuyTuJing";
  /**延保时间*/
  public static final String CARD_YANBAO_TIME = "YanBaoTime";
  /**延保单位*/
  public static final String CARD_YANBAO_TIME_COMPANY = "YanBaoDanWei";
  /**整机保修时间*/
  public static final String CARD_WY = "wy";
  /**延保电话*/
  public static final String CARD_YBPhone = "YBPhone";
  /**KY编码*/
  public static final String CARD_KY = "ky";
  /**延保单位电话*/
  public static final String CARD_YANBAO_TIME_COPMANY_TEL = "YanBaoDanWeiCommanyTel";
  /**整机保修，目前不定义*/
  public static final String DEVICE_WARRANTY_PERIOD = "warranty_period";
  /**配件保修*/
  public static final String CARD_COMPONENT_VALIDITY = "component_validity";
  
  public static final String CARD_MM_ONE = "mmone";
  public static final String CARD_MM_ONE_TEL = "mmone_tel";
  public static final String CARD_MM_ONE_NAME = "mmone_name";
  
  public static final String CARD_MM_TWO = "mmtwo";
  public static final String CARD_MM_TWO_TEL = "mmtwo_tel";
  public static final String CARD_MM_TWO_NAME = "mmtwo_name";
  
  /**用来构建保修卡设备预览图*/
  public static final String CARD_PKY = "pky";
  
  //add by chenkai, 锁定认证字段 20140701 begin
  /**保修记录是否锁定,如果是1表示这个保修卡是厂家认可的，发票内容不能更改，也不能删除*/
  public static final String REWARD_STATUS = "rewardStatus";
  //add by chenkai, 锁定认证字段 20140701 end
  
  //这里是设备表的扩展，如遥控器
  
  
  /**型号数据表，这个我们会新增到预置的device.db数据库文件中*/
  public static final String TABLE_NAME_DEVICE_XINGHAO = "xinghao";
  /**品牌五位code码，用来过滤数据用的*/
  public static final String DEVICE_XINGHAO_PCODE = "pcode";
  public static final String DEVICE_XINGHAO_MN = "MN";
  public static final String DEVICE_XINGHAO_KY = "KY";
  public static final String DEVICE_XINGHAO_WY = "WY";
  
  // Qrcode scan part begin
  public static final String TABLE_SCAN_NAME = "history";
  public static final String ID_COL = "id";
  public static final String TEXT_COL = "text";
  public static final String FORMAT_COL = "format";
  public static final String DISPLAY_COL = "display";
  public static final String TIMESTAMP_COL = "timestamp";
  public static final String DETAILS_COL = "details";
  // Qrcode scan part end
  
  //友盟的推送消息历史
  public static final String TABLE_YOUMENG_PUSHMESSAGE_HISTORY = "youmeng_push_message_history";
  public static final String YOUMENG_TEXT = "text";
  public static final String YOUMENG_TITLE = "title";
  public static final String YOUMENG_MESSAGE_ID = "msg_id";
  public static final String YOUMENG_MESSAGE_ACTIVITY = "activity";
  public static final String YOUMENG_MESSAGE_URL = "url";
  public static final String YOUMENG_MESSAGE_CUSTOM = "custom";
  public static final String YOUMENG_MESSAGE_RAW = "raw_json";
  
  
//IM模块 begin
  public static final String TABLE_IM_QUN_HISTORY = "im_message_qun_history";
  public static final String TABLE_IM_FRIEND_HISTORY = "im_message_friend_history";
  /**服务器上对应的消息ID*/
  public static final String IM_SERVICE_ID = "service_id";
  /**消息正文*/
  public static final String IM_TEXT = "text";
  /**消息是否已经看过了*/
  public static final String IM_SEEN = "seen";
  /**消息对象，根据IM_TARGET_TYPE的值来区分对象*/
  public static final String IM_TARGET = "target";
  public static final String IM_TARGET_TYPE = "target_type";
  public static final String IM_UID = "uid";
  public static final String IM_UNAME = "name";
  public static final String IM_SERVICE_TIME = "service_time";
  /**消息发送状态，对于收到的消息总是1，否则位0*/
  public static final String IM_MESSAGE_STATUS = "message_status";
  //IM模块 end
  
  //好友关系 begin
  public static final String TABLE_ACCOUNT_RELATIONSHIP = "account_relationship";
  public static final String RELATIONSHIP_TYPE = IM_TARGET_TYPE;
  public static final String RELATIONSHIP_TARGET = IM_TARGET;
  public static final String RELATIONSHIP_NAME = IM_UNAME;
  public static final String RELATIONSHIP_UID = IM_UID;
  /**对应于服务器上的id*/
  public static final String RELATIONSHIP_SERVICE_ID = IM_SERVICE_ID;
  /**消息对象，根据IM_TARGET_TYPE的值来区分对象*/
  public static final String DATA1 = "data1";
  public static final String DATA2 = "data2";
  public static final String DATA3 = "data3";
  public static final String DATA4 = "data4";
  public static final String DATA5 = "data5";
  //好友关系 end
  
  public HaierDBHelper(Context context) {
    super(context, DB_NAME, null, DB_VERSION);
  }
  
  private SQLiteDatabase mWritableDatabase;
  private SQLiteDatabase mReadableDatabase;
  
  public synchronized SQLiteDatabase openWritableDatabase() {
	  if (mWritableDatabase == null) {
		  mWritableDatabase = getWritableDatabase();
	  }
	  return mWritableDatabase;
  }
  
  public synchronized SQLiteDatabase openReadableDatabase() {
	  if (mReadableDatabase == null) {
		  mReadableDatabase = getReadableDatabase();
	  }
	  return mReadableDatabase;
  }
  
  public synchronized void closeReadableDatabase() {
	  if (mReadableDatabase != null && mReadableDatabase.isOpen()) {
		  mReadableDatabase.close();
		  mReadableDatabase = null;
	  }
  }
  
  public synchronized void closeWritableDatabase() {
	  if (mWritableDatabase != null && mWritableDatabase.isOpen()) {
		  mWritableDatabase.close();
		  mWritableDatabase = null;
	  }
  }
  
  public synchronized void closeDatabase() {
	  closeReadableDatabase();
	  closeWritableDatabase();
  }

  @Override
  public void onCreate(SQLiteDatabase sqLiteDatabase) {
      DebugUtils.logD(TAG, "onCreate");
   
       // Create Account table
  	   createAccountTable(sqLiteDatabase);
  		//Create Homes table
  		createHomesTable(sqLiteDatabase);
  		// Create devices table
  		createBaoxiuCardsTable(sqLiteDatabase);
//  		createTriggerForMyCardTable(sqLiteDatabase);
  		// Create scan history
  		createScanHistory(sqLiteDatabase);
  		
  		createXinghaoTable(sqLiteDatabase);
  		
  		createYoumengMessageTable(sqLiteDatabase);
  		
  		createImMessageTable(sqLiteDatabase);
  		createAccountRelationshipTable(sqLiteDatabase);
  		
  }
  
  private void createTriggerForAccountTable(SQLiteDatabase sqLiteDatabase) {
	  String sql = "CREATE TRIGGER insert_account" + " BEFORE INSERT " + " ON " + TABLE_NAME_ACCOUNTS + 
			  " BEGIN UPDATE " + TABLE_NAME_ACCOUNTS + " SET isDefault = 0 WHERE uid != new.uid and isDefault = 1; END;";
	  sqLiteDatabase.execSQL(sql);
	  
	  sql = "CREATE TRIGGER update_default_account" + " BEFORE UPDATE " + " ON " + TABLE_NAME_ACCOUNTS + 
			  " BEGIN UPDATE " + TABLE_NAME_ACCOUNTS + " SET isDefault = 0 WHERE uid != old.uid and isDefault = 1; END;";
	  sqLiteDatabase.execSQL(sql);
	  
  }
  
  private void createTriggerForHomeTable(SQLiteDatabase sqLiteDatabase) {
	  String sql = "CREATE TRIGGER insert_home_update_account" + " AFTER INSERT " + " ON " + TABLE_NAME_HOMES + 
			  " BEGIN UPDATE " + TABLE_NAME_ACCOUNTS + " SET home_count = home_count+1 WHERE uid = new.uid; END;";
	  sqLiteDatabase.execSQL(sql);
	  
	  sql = "CREATE TRIGGER delete_home_update_account" + " AFTER DELETE " + " ON " + TABLE_NAME_HOMES + 
			  " BEGIN UPDATE " + TABLE_NAME_ACCOUNTS + " SET home_count = home_count-1 WHERE uid = old.uid; END;";
	  sqLiteDatabase.execSQL(sql);
	
  }
  
  private void createTriggerForBaoxiuCardsTable(SQLiteDatabase sqLiteDatabase) {
	  String sql = "CREATE TRIGGER insert_cards_update_home" + " AFTER INSERT " + " ON " + TABLE_NAME_CARDS + 
			  " BEGIN UPDATE " + TABLE_NAME_HOMES + " SET baoxiucard_count = baoxiucard_count+1 WHERE aid = new.aid; END;";
	  sqLiteDatabase.execSQL(sql);
	  
	  sql = "CREATE TRIGGER delete_card_update_home" + " AFTER DELETE " + " ON " + TABLE_NAME_CARDS + 
			  " BEGIN UPDATE " + TABLE_NAME_HOMES + " SET baoxiucard_count = baoxiucard_count-1 WHERE aid = old.aid; END;";
	  sqLiteDatabase.execSQL(sql);
	  
	  sql = "CREATE TRIGGER insert_cards_update_account" + " AFTER INSERT " + " ON " + TABLE_NAME_CARDS + 
			  " BEGIN UPDATE " + TABLE_NAME_ACCOUNTS + " SET baoxiucard_count = baoxiucard_count+1 WHERE uid = new.uid; END;";
	  sqLiteDatabase.execSQL(sql);
	  
	  sql = "CREATE TRIGGER delete_card_update_account" + " AFTER DELETE " + " ON " + TABLE_NAME_CARDS + 
			  " BEGIN UPDATE " + TABLE_NAME_ACCOUNTS + " SET baoxiucard_count = baoxiucard_count-1 WHERE uid = old.uid; END;";
	  sqLiteDatabase.execSQL(sql);
	
  }
  
  
  private void createAccountTable(SQLiteDatabase sqLiteDatabase) {
	  sqLiteDatabase.execSQL(
	            "CREATE TABLE " + TABLE_NAME_ACCOUNTS + " (" +
	            ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
	            ACCOUNT_UID + " INTEGER NOT NULL DEFAULT 0, " +
	            ACCOUNT_TEL + " TEXT, " +
	            ACCOUNT_PWD + " TEXT, " +
	            ACCOUNT_DEFAULT + " INTEGER NOT NULL DEFAULT 1, " +
	            ACCOUNT_HOME_COUNT + " INTEGER NOT NULL DEFAULT 0, " +
	            HOME_CARD_COUNT + " INTEGER NOT NULL DEFAULT 0, " +
	            ACCOUNT_NAME + " TEXT, " +
	            ACCOUNT_TITLE + " TEXT, " +
	            ACCOUNT_ORG + " TEXT, " +
	            ACCOUNT_WORKADDRESS + " TEXT, " +
	            ACCOUNT_MM + " TEXT, " +
	            ACCOUNT_PHONES  + " TEXT, " +
	            ACCOUNT_HAS_PHOTO + " INTEGER NOT NULL DEFAULT 0, " +
	            DATE + " TEXT" +
	            ");");
	  createTriggerForAccountTable(sqLiteDatabase);
  }
  
  private void createHomesTable(SQLiteDatabase sqLiteDatabase) {
	  sqLiteDatabase.execSQL(
	            "CREATE TABLE " + TABLE_NAME_HOMES + " (" +
	            ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
	            ACCOUNT_UID + " INTEGER NOT NULL DEFAULT 0, " +
	            HOME_AID + " INTEGER, " +
	            HOME_NAME + " TEXT, " +
	            HOME_CARD_COUNT + " INTEGER NOT NULL DEFAULT 0, " +
	            DeviceDBHelper.DEVICE_PRO_NAME + " TEXT, " +
	            DeviceDBHelper.DEVICE_CITY_NAME + " TEXT, " +
	            DeviceDBHelper.DEVICE_DIS_NAME + " TEXT, " +
	            HOME_DETAIL + " TEXT, " +
	            HOME_DEFAULT + " INTEGER NOT NULL DEFAULT 1, " +
	            POSITION + " INTEGER NOT NULL DEFAULT 1, " +
	            DATE + " TEXT" +
	            ");");
	  createTriggerForHomeTable(sqLiteDatabase);
  }
  
  private void createBaoxiuCardsTable(SQLiteDatabase sqLiteDatabase) {
	  sqLiteDatabase.execSQL(
	            "CREATE TABLE " + TABLE_NAME_CARDS + " (" +
	            ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
	            CARD_UID + " INTEGER NOT NULL DEFAULT 0, " +  //账户id
	            CARD_AID + " INTEGER, " +     //家id
	            CARD_BID + " INTEGER, " +     //保修卡服务器id
	            CARD_TYPE + " TEXT, " +
	            CARD_NAME + " TEXT, " +
	            CARD_PINPAI + " TEXT, " +
	            CARD_MODEL + " TEXT, " +
	            CARD_SERIAL + " TEXT, " +
	            CARD_BXPhone + " TEXT, " +
	            CARD_FPname + " TEXT, " +
	            CARD_FPaddr + " TEXT, " +
	            CARD_BUT_DATE + " TEXT, " +
	            CARD_PRICE + " TEXT, " +
	            CARD_BUY_TUJING + " TEXT, " +
	            CARD_WY + " TEXT, " +
	            CARD_YBPhone + " TEXT, " +
	            CARD_KY + " TEXT, " +
	            CARD_PKY + " TEXT, " +
	            CARD_MM_ONE + " TEXT, " +
	            CARD_MM_ONE_TEL + " TEXT, " +
	            CARD_MM_ONE_NAME + " TEXT, " +
	            CARD_MM_TWO + " TEXT, " +
	            CARD_MM_TWO_TEL + " TEXT, " +
	            CARD_MM_TWO_NAME + " TEXT, " +
	            CARD_YANBAO_TIME + " TEXT, " +
	            CARD_YANBAO_TIME_COMPANY + " TEXT, " +
	            CARD_YANBAO_TIME_COPMANY_TEL + " TEXT, " +
	            DEVICE_WARRANTY_PERIOD + " TEXT, " +
	            CARD_COMPONENT_VALIDITY + " TEXT, " +
	            REWARD_STATUS + " INTEGER NOT NULL DEFAULT 0, " + //add by chenkai, 锁定认证字段 20140701
	            DATE + " TEXT" +
	            ");");
	  createTriggerForBaoxiuCardsTable(sqLiteDatabase);
  }
  
  private void createScanHistory(SQLiteDatabase sqLiteDatabase) {
	  sqLiteDatabase.execSQL(
	            "CREATE TABLE " + TABLE_SCAN_NAME + " (" +
	            ID_COL + " INTEGER PRIMARY KEY, " +
	            TEXT_COL + " TEXT, " +
	            FORMAT_COL + " TEXT, " +
	            DISPLAY_COL + " TEXT, " +
	            TIMESTAMP_COL + " INTEGER, " +
	            DETAILS_COL + " TEXT);");
  }
  
  private void createXinghaoTable(SQLiteDatabase sqLiteDatabase) {
	  sqLiteDatabase.execSQL(
	            "CREATE TABLE " + TABLE_NAME_DEVICE_XINGHAO + " (" +
	            ID + " INTEGER PRIMARY KEY, " +
	            DEVICE_XINGHAO_PCODE + " TEXT, " +
	            DEVICE_XINGHAO_MN + " TEXT, " +
	            DEVICE_XINGHAO_KY + " TEXT, " +
	            DEVICE_XINGHAO_WY + " TEXT, " +
	            DATE + " TEXT);");
  }
  /**
   * {"msg_id":"us65502140752348982811","body":{"play_vibrate":"true","text":"111112222","title":"1111","ticker":"1111","play_lights":"true","play_sound":"true","after_open":"go_app","activity":"","url":"","custom":""},"random_min":0,"alias":"","display_type":"notification"}
   * @param sqLiteDatabase
   */
  private void createYoumengMessageTable(SQLiteDatabase sqLiteDatabase) {
	  sqLiteDatabase.execSQL(
	            "CREATE TABLE " + TABLE_YOUMENG_PUSHMESSAGE_HISTORY + " (" +
	            ID + " INTEGER PRIMARY KEY, " +
	            YOUMENG_MESSAGE_ID + " TEXT, " +
	            YOUMENG_TITLE + " TEXT, " +
	            YOUMENG_TEXT + " TEXT, " +
	            YOUMENG_MESSAGE_ACTIVITY + " TEXT, " +
	            YOUMENG_MESSAGE_URL + " TEXT, " +
	            YOUMENG_MESSAGE_CUSTOM + " TEXT, " +
	            YOUMENG_MESSAGE_RAW + " TEXT, " +
	            DATE + " TEXT);");
  }
  
  private void createImMessageTable(SQLiteDatabase sqLiteDatabase) {
	  sqLiteDatabase.execSQL(
	            "CREATE TABLE " + TABLE_IM_QUN_HISTORY + " (" +
	            ID + " INTEGER PRIMARY KEY, " +
	            IM_MESSAGE_STATUS + " INTEGER NOT NULL DEFAULT 0, " +  //信息发送状态
	            IM_SERVICE_ID + " TEXT, " +
	            IM_TEXT + " TEXT, " +
	            IM_SEEN + " INTEGER, " +   //是否看过了
	            IM_TARGET + " TEXT, " +
	            IM_TARGET_TYPE + " INTEGER, " +
	            IM_UID + " TEXT, " +
	            IM_UNAME + " TEXT, " +
	            IM_SERVICE_TIME + " TEXT, " +
	            DATE + " TEXT);");
	  
	  sqLiteDatabase.execSQL(
	            "CREATE TABLE " + TABLE_IM_FRIEND_HISTORY + " (" +
	            ID + " INTEGER PRIMARY KEY, " +
	            IM_MESSAGE_STATUS + " INTEGER NOT NULL DEFAULT 0, " +  //信息发送状态
	            IM_SERVICE_ID + " TEXT, " +
	            IM_TEXT + " TEXT, " +
	            IM_SEEN + " INTEGER, " +    //是否看过了
	            IM_TARGET + " TEXT, " +
	            IM_TARGET_TYPE + " INTEGER, " +
	            IM_UID + " TEXT, " +
	            IM_UNAME + " TEXT, " +
	            IM_SERVICE_TIME + " TEXT, " +
	            DATE + " TEXT);");
  }
  
  private void createAccountRelationshipTable(SQLiteDatabase sqLiteDatabase) {
	  sqLiteDatabase.execSQL(
	            "CREATE TABLE " + TABLE_ACCOUNT_RELATIONSHIP + " (" +
	            ID + " INTEGER PRIMARY KEY, " +
	            RELATIONSHIP_TYPE + " INTEGER, " + 
	            RELATIONSHIP_TARGET + " TEXT, " +
	            RELATIONSHIP_NAME + " TEXT, " +
	            RELATIONSHIP_UID + " TEXT, " +
	            RELATIONSHIP_SERVICE_ID + " TEXT, " +
	            DATA1 + " TEXT, " +
	            DATA2 + " TEXT, " +
	            DATA3 + " TEXT, " +
	            DATA4 + " TEXT, " +
	            DATA5 + " INTEGER NOT NULL DEFAULT -1, " +
	            DATE + " TEXT);");
  }
  
  private void addTextColumn(SQLiteDatabase sqLiteDatabase, String table, String column) {
	    String alterForTitleSql = "ALTER TABLE " + table +" ADD " + column + " TEXT";
		sqLiteDatabase.execSQL(alterForTitleSql);
  }
  private void addIntColumn(SQLiteDatabase sqLiteDatabase, String table, String column, int defaultValue) {
	    String alterForTitleSql = "ALTER TABLE " + table +" ADD " + column + " INTEGER NOT NULL DEFAULT " + defaultValue;
		sqLiteDatabase.execSQL(alterForTitleSql);
}

  @Override
  public void onUpgrade(SQLiteDatabase sqLiteDatabase, int oldVersion, int newVersion) {
	  DebugUtils.logD(TAG, "onUpgrade oldVersion " + oldVersion + " newVersion " + newVersion);
	  if (oldVersion < 3 ) {
			sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME_ACCOUNTS);
		    sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME_HOMES);
		    sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME_CARDS);
		    sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + TABLE_SCAN_NAME);
		    sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME_DEVICE_XINGHAO);
		    sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + TABLE_YOUMENG_PUSHMESSAGE_HISTORY);
		    sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + TABLE_IM_FRIEND_HISTORY);
		    sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + TABLE_IM_QUN_HISTORY);
		    sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + TABLE_ACCOUNT_RELATIONSHIP);
		    
		    sqLiteDatabase.execSQL("DROP TRIGGER IF EXISTS " + "insert_account");
		    sqLiteDatabase.execSQL("DROP TRIGGER IF EXISTS " + "update_default_account");
		    sqLiteDatabase.execSQL("DROP TRIGGER IF EXISTS " + "insert_home_update_account");
		    sqLiteDatabase.execSQL("DROP TRIGGER IF EXISTS " + "delete_home_update_account");
		    
		    sqLiteDatabase.execSQL("DROP TRIGGER IF EXISTS " + "insert_cards_update_home");
		    sqLiteDatabase.execSQL("DROP TRIGGER IF EXISTS " + "delete_card_update_home");
		    sqLiteDatabase.execSQL("DROP TRIGGER IF EXISTS " + "insert_cards_update_account");
		    sqLiteDatabase.execSQL("DROP TRIGGER IF EXISTS " + "delete_cards_update_account");
		    
		    sqLiteDatabase.execSQL("DROP TRIGGER IF EXISTS " + "insert_cards_update_home");
		    sqLiteDatabase.execSQL("DROP TRIGGER IF EXISTS " + "delete_card_update_home");
		    onCreate(sqLiteDatabase);
		    return;
		} 
  }
}
