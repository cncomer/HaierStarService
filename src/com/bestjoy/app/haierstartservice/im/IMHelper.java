package com.bestjoy.app.haierstartservice.im;

import java.io.InputStream;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.text.TextUtils;

import com.bestjoy.app.haierstartservice.HaierServiceObject.HaierResultObject;
import com.bestjoy.app.haierstartservice.database.BjnoteContent;
import com.bestjoy.app.haierstartservice.database.HaierDBHelper;
import com.shwy.bestjoy.utils.DebugUtils;
import com.shwy.bestjoy.utils.InfoInterface;
import com.shwy.bestjoy.utils.NetworkUtils;
import com.shwy.bestjoy.utils.PageInfo;

public class IMHelper {

	private static final String TAG = "IMHelper";
	/**
	 * 消息类型， 0是登录，1是退出， 2是消息（可以理解为自己向服务器发送的消息）, 3是转发的消息， data数据中也会有一个type, 1是群消息，此时target字段对应群id, 比如sn, 2是点对点消息，target是对方uid
	 *
	 */
	public static final String EXTRA_TYPE = "type";
	/**
	 * 消息体
	 */
	public static final String EXTRA_DATA = "data";
	public static final String EXTRA_UID = "uid";
	public static final String EXTRA_PWD = "pwd";
	public static final String EXTRA_TEXT = "text";
	public static final String EXTRA_TOKEN = "n";
	public static final String EXTRA_TARGET = "target";
	public static final String EXTRA_SERVICE_TIME = "createtime";
	public static final String EXTRA_SERVICE_ID = "serviceid";
	public static final String EXTRA_UNAME = "usrname";
	
	public static final int TYPE_LOGIN = 0;  //登录
	public static final int TYPE_EXIT = 1;  
	public static final int TYPE_MESSAGE = 2;  //用户发送的信息
	public static final int TYPE_MESSAGE_FORWARD = 3; //用户收到的其他用户发送的消息
	/**群消息*/
	public static final int TARGET_TYPE_QUN = 1;
	/**点对点消息*/
	public static final int TARGET_TYPE_P2P = 2;
	
	public static final String[] PROJECTION = new String[]{
		HaierDBHelper.ID,              //0
		HaierDBHelper.IM_SERVICE_ID,   //1
		HaierDBHelper.IM_TARGET_TYPE,  //2
		HaierDBHelper.IM_TARGET,       //3
		HaierDBHelper.IM_TEXT,         //4
		HaierDBHelper.IM_UID,           //5
		HaierDBHelper.IM_UNAME,         //6
		HaierDBHelper.IM_SERVICE_TIME,  //7
		HaierDBHelper.DATE,             //8
		HaierDBHelper.IM_MESSAGE_STATUS,//9
		HaierDBHelper.IM_SEEN,         //10
	};
	
	public static final int INDEX_ID = 0;
	public static final int INDEX_SERVICE_ID = 1;
	public static final int INDEX_TRAGET_TYPE = 2;
	public static final int INDEX_TARGET = 3;
	public static final int INDEX_TEXT = 4;
	public static final int INDEX_UID = 5;
	public static final int INDEX_UNAME = 6;
	public static final int INDEX_SERVICE_TIME = 7;
	public static final int INDEX_LOCAL_TIME = 8;
	public static final int INDEX_STATUS = 9;
	public static final int INDEX_SEEN = 10;
	/**按照消息的服务器id升序排序*/
	public static final String SORT_BY_MESSAGE_ID = HaierDBHelper.IM_SERVICE_TIME + " asc";
	
//	public static final String UID_AND_TARGET_SELECTION = HaierDBHelper.IM_UID + "=? and " + HaierDBHelper.IM_TARGET_TYPE + "=? and " + HaierDBHelper.IM_TARGET + "=?";
	public static final String QUN_SELECTION = HaierDBHelper.IM_TARGET_TYPE + "=? and " + HaierDBHelper.IM_TARGET + "=?";
	public static final String UID_SELECTION = HaierDBHelper.IM_UID + "=?";
	public static final String FRIEND_SELECTION = HaierDBHelper.IM_UID + "=? and " + HaierDBHelper.IM_TARGET + "=?";
	public static final String UID_MESSAGEID_SELECTION = UID_SELECTION + " and " + HaierDBHelper.ID + "=?";
	public static final String SERVICEID_QUN_SELECTION = HaierDBHelper.IM_SERVICE_ID + "=? and " + QUN_SELECTION;
	public static final String SERVICEID_FRIEND_SELECTION = HaierDBHelper.IM_SERVICE_ID + "=? and " + FRIEND_SELECTION;
	
	public static final String FRIEND_P2P_SELECTION = "(" + FRIEND_SELECTION + ") or (" + FRIEND_SELECTION + ")";
	
	public static  DateFormat SERVICE_DATE_TIME_FORMAT = new SimpleDateFormat("yyyyMMddHHmmss");
	public static  DateFormat LOCAL_DATE_TIME_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	/**
	 * 会话开始前，我们需要先登录IM服务器
	 * @param uid
	 * @param pwd
	 * @return
	 */
	public static JSONObject createOrJoinConversation(String uid, String pwd, String name) {
		JSONObject jsonObject = new JSONObject();
		try {
			jsonObject.put(EXTRA_TYPE, String.valueOf(TYPE_LOGIN));
			jsonObject.put(EXTRA_UID, uid).put(EXTRA_PWD, pwd).put(EXTRA_UNAME, name);
			jsonObject.put(EXTRA_DATA, "");
			DebugUtils.logD(TAG, "createOrJoinConversation json=" + jsonObject.toString());
			return jsonObject;
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return jsonObject;
	}
	/***
	 * 退出登录状态
	 * @param uid
	 * @param pwd
	 * @return
	 */
	public static JSONObject exitConversation(String uid, String pwd, String name) {
		JSONObject jsonObject = new JSONObject();
		try {
			jsonObject.put(EXTRA_TYPE, String.valueOf(TYPE_EXIT));
			jsonObject.put(EXTRA_UID, uid).put(EXTRA_PWD, pwd).put(EXTRA_UNAME, name);
			jsonObject.put(EXTRA_DATA, "");
			DebugUtils.logD(TAG, "exitConversation json=" + jsonObject.toString());
			return jsonObject;
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return jsonObject;
	}
	/**
	 * 创建消息体
	 * @param uid
	 * @param pwd
	 * @param targetType
	 * @param target
	 * @param text
	 * @return
	 */
	public static JSONObject createMessageConversation(ConversationItemObject conversation) {
		JSONObject jsonObject = new JSONObject();
		try {
			jsonObject.put(EXTRA_TYPE, String.valueOf(TYPE_MESSAGE));
			jsonObject.put(EXTRA_UID, conversation.mUid).put(EXTRA_PWD, conversation.mPwd).put(IMHelper.EXTRA_UNAME, conversation.mUName);
			JSONObject data = new JSONObject();
			data.put(EXTRA_TYPE, String.valueOf(conversation.mTargetType)).put(EXTRA_TARGET, conversation.mTarget).put(EXTRA_TEXT, conversation.mMessage).put(EXTRA_TOKEN, String.valueOf(conversation.mId));
			jsonObject.put(EXTRA_DATA, data);
			DebugUtils.logD(TAG, "createMessageConversation json=" + jsonObject.toString());
			return jsonObject;
		} catch (JSONException e) {
			e.printStackTrace();
		}
		DebugUtils.logD(TAG, "createMessageConversation json=" + jsonObject.toString());
		return jsonObject;
	}
	
	public static Cursor getAllLocalQunMessage(ContentResolver cr, String uid, int targetType, String target) {
		return cr.query(BjnoteContent.IM.CONTENT_URI_QUN, PROJECTION, QUN_SELECTION, new String[]{String.valueOf(targetType), target}, SORT_BY_MESSAGE_ID);
	}
	
	public static Cursor getAllLocalMessage(ContentResolver cr, String uid, int targetType, String target) {
		if (targetType == IMHelper.TARGET_TYPE_QUN) {
			return getAllLocalQunMessage(cr, uid, targetType, target);
		} else if (targetType == IMHelper.TARGET_TYPE_P2P) {
			return cr.query(BjnoteContent.IM.CONTENT_URI_FRIEND, PROJECTION, FRIEND_P2P_SELECTION, new String[]{String.valueOf(uid), target, target, String.valueOf(uid)}, SORT_BY_MESSAGE_ID);
		}
		return null;
	}
	
	/**
	 * 更新信息数据，最直观的用法就是根据服务器返回的id来更新本地信息的发送状态
	 * @param cr
	 * @param values
	 * @param where
	 * @param selectionArgs
	 * @return
	 */
	public static int update(ContentResolver cr, int targetType, ContentValues values, String where, String[] selectionArgs) {
		Uri uri = null;
		if (targetType == IMHelper.TARGET_TYPE_QUN) {
			uri = BjnoteContent.IM.CONTENT_URI_QUN;
		} else if (targetType == IMHelper.TARGET_TYPE_P2P) {
			uri = BjnoteContent.IM.CONTENT_URI_FRIEND;
		}
		return cr.update(uri, values, where, selectionArgs);
	}
	
	public static void deleteAllMessages(ContentResolver cr, long uid) {
		DebugUtils.logD(TAG, "deleteAllMessages for uid " + uid);
		 int deleted = cr.delete(BjnoteContent.IM.CONTENT_URI_QUN, UID_SELECTION, new String[]{String.valueOf(uid)});
		 DebugUtils.logD(TAG, "deleteAllQunMessages " + deleted);
		 deleted = cr.delete(BjnoteContent.IM.CONTENT_URI_FRIEND, UID_SELECTION, new String[]{String.valueOf(uid)});
		 DebugUtils.logD(TAG, "deleteAllFriendsMessages " + deleted);
	}
	
	public static ConversationItemObject getConversationItemObject(JSONObject result) {
		ConversationItemObject conversationItemObject = new ConversationItemObject();
		if (result != null) {
			conversationItemObject.mMessage = result.optString(IMHelper.EXTRA_TEXT, "");
			conversationItemObject.mServiceId = result.optString(IMHelper.EXTRA_SERVICE_ID, "");
			conversationItemObject.mId = Integer.valueOf(result.optString(IMHelper.EXTRA_TOKEN, "-1"));
			conversationItemObject.mTargetType = Integer.valueOf(result.optString(IMHelper.EXTRA_TYPE, "0"));
			conversationItemObject.mTarget = result.optString(IMHelper.EXTRA_TARGET, "");
			String timeStr = result.optString(IMHelper.EXTRA_SERVICE_TIME, "");
			try {
				Date date = SERVICE_DATE_TIME_FORMAT.parse(timeStr);
				conversationItemObject.mServiceDate = date.getTime();
				DebugUtils.logD(TAG, "getConversationItemObject convert timeStr " + timeStr + " to Date " + conversationItemObject.mServiceDate);
			} catch (ParseException e) {
				e.printStackTrace();
			}
			return conversationItemObject;
		}
		//这里一般是不会走到的，所以我们打印出堆栈信息
		new Exception(TAG + "getConversationItemObject return null").printStackTrace();
		return null;
		
	}
	public static int saveList(ContentResolver cr, List<? extends InfoInterface> infoObjects) {
		int insertOrUpdateCount = 0;
		if (infoObjects != null) {
			for(InfoInterface object:infoObjects) {
				if (object.saveInDatebase(cr, null)) {
					insertOrUpdateCount++;
				}
			}
		}
		return insertOrUpdateCount;
	}
	public static List<ConversationItemObject> parseList(InputStream is, PageInfo pageInfo, int targetType) {
		HaierResultObject serviceResultObject = HaierResultObject.parse(NetworkUtils.getContentFromInput(is));
		List<ConversationItemObject> list = new ArrayList<ConversationItemObject>();
		if (serviceResultObject.isOpSuccessfully()) {
			try {
				JSONObject jsonObject = serviceResultObject.mJsonData;
				pageInfo.mTotalCount = jsonObject.getInt("total");
				JSONArray rows = jsonObject.getJSONArray("rows");
				long rowsLen = rows.length();
				
				DebugUtils.logD(TAG, "parseList find rows " + rowsLen);
				ConversationItemObject conversationItemObject = null;
				for(int index = 0; index < rowsLen; index++) {
					jsonObject = rows.getJSONObject(index);
					conversationItemObject = new ConversationItemObject();
					conversationItemObject.mMessageStatus = 1;
					conversationItemObject.mServiceId = jsonObject.getString("mid");
					conversationItemObject.mMessage = jsonObject.getString("mcontent");
					conversationItemObject.mUid = jsonObject.getString("fuser");
					conversationItemObject.mUName = jsonObject.getString("fname");
					conversationItemObject.mTarget = jsonObject.getString("tuser");
					conversationItemObject.mTargetType = targetType;
					String timeStr = jsonObject.optString("mestime", "");
					try {
						Date date = SERVICE_DATE_TIME_FORMAT.parse(timeStr);
						conversationItemObject.mServiceDate = date.getTime();
						DebugUtils.logD(TAG, "getConversationItemObject convert timeStr " + timeStr + " to Date " + conversationItemObject.mServiceDate);
					} catch (ParseException e) {
						e.printStackTrace();
					}
					list.add(conversationItemObject);
				}
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}
		return list;
		
	}
	
	
	public static class ImServiceResultObject {
		public int mStatusCode = 0;
		public String mStatusMessage;
		public JSONObject mJsonData;
		public String mType;
		public String mStrData;
		public String mUid, mPwd, mUName;
		
		public static ImServiceResultObject parse(String content) {
			ImServiceResultObject resultObject = new ImServiceResultObject();
			if (TextUtils.isEmpty(content)) {
				return resultObject;
			}
			try {
				JSONObject jsonObject = new JSONObject(content);
				resultObject.mStatusCode = Integer.parseInt(jsonObject.getString("StatusCode"));
				resultObject.mStatusMessage = jsonObject.getString("StatusMessage");
				//消息类型
				resultObject.mType = jsonObject.getString("type");
				resultObject.mUid = jsonObject.getString(IMHelper.EXTRA_UID);
				resultObject.mPwd = jsonObject.getString(IMHelper.EXTRA_PWD);
				resultObject.mUName = jsonObject.getString(IMHelper.EXTRA_UNAME);
				try {
					resultObject.mJsonData = jsonObject.getJSONObject("data");
				} catch (JSONException e) {
					resultObject.mStrData = jsonObject.getString("data");
				}
			} catch (JSONException e) {
				e.printStackTrace();
				resultObject.mStatusMessage = e.getMessage();
			}
			return resultObject;
		}
		public boolean isOpSuccessfully() {
			return mStatusCode == 1;
		}
	}
}
