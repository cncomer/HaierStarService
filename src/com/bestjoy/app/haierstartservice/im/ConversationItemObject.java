package com.bestjoy.app.haierstartservice.im;

import java.util.Date;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;

import com.bestjoy.app.haierstartservice.account.MyAccountManager;
import com.bestjoy.app.haierstartservice.database.BjnoteContent;
import com.bestjoy.app.haierstartservice.database.HaierDBHelper;
import com.shwy.bestjoy.utils.DebugUtils;
import com.shwy.bestjoy.utils.InfoInterface;

public class ConversationItemObject implements InfoInterface{
	private static final String TAG = "ConversationItemObject";
	/**信息本地id*/
	public long mId = -1;
	/**信息服务器id*/
	public String mServiceId = "-1";
	/**信息类型*/
	public int mTargetType;
	/**信息目标*/
	public String mTarget;
	public String mUid, mPwd, mUName, mMessage;
	/**信息服务器时间*/
	public long mServiceDate;
	public long mLocalCreateDate;
	/**信息当前状态，如发送中0，发送成功1*/
	public int mMessageStatus = 0;
	/**是否已经看过，0表示未读，1表示已读*/
	public int mSeen = 0;
	
	public static final int SEEN = 1;
	public static final int UN_SEEN = 0;
	
	public static final String[] ID_PROJECTION = new String[]{
		HaierDBHelper.ID,              //0
		HaierDBHelper.IM_SERVICE_ID,   //1
	};
	public static final String UID_AND_TARGET_SELECTION = HaierDBHelper.IM_UID + "=? and " + HaierDBHelper.IM_TARGET_TYPE + "=? and " + HaierDBHelper.IM_TARGET + "=?";
	public static final String SID_UID_AND_TARGET_SELECTION = HaierDBHelper.IM_SERVICE_ID + "=? and " + UID_AND_TARGET_SELECTION;
	public static final String ID_SELECTION = HaierDBHelper.ID + "=?";

	public boolean hasId() {
		return mId > -1;
	}
	public void setReadStatus(int seen) {
		mSeen = seen;
	}
	
	public static ConversationItemObject getConversationItemObjectFromCursor(Cursor c) {
		ConversationItemObject conversationItemObject = new ConversationItemObject();
		conversationItemObject.mId = c.getLong(IMHelper.INDEX_ID);
		conversationItemObject.mMessage = c.getString(IMHelper.INDEX_TEXT);
		conversationItemObject.mServiceId = c.getString(IMHelper.INDEX_SERVICE_ID);
		conversationItemObject.mServiceDate = Long.valueOf(c.getString(IMHelper.INDEX_SERVICE_TIME));
		conversationItemObject.mLocalCreateDate = Long.valueOf(c.getString(IMHelper.INDEX_LOCAL_TIME));
		conversationItemObject.mMessageStatus = c.getInt(IMHelper.INDEX_STATUS);
		conversationItemObject.mSeen = c.getInt(IMHelper.INDEX_SEEN);
		
		conversationItemObject.mUid = c.getString(IMHelper.INDEX_UID);
		conversationItemObject.mUName = c.getString(IMHelper.INDEX_UNAME);
		conversationItemObject.mPwd = MyAccountManager.getInstance().getAccountObject().mAccountPwd;
		
		conversationItemObject.mTarget = c.getString(IMHelper.INDEX_TARGET);
		conversationItemObject.mTargetType = c.getInt(IMHelper.INDEX_TRAGET_TYPE);
		return conversationItemObject;
	}
	
	public boolean deleteMessage(ContentResolver cr) {
		Uri url = null;
		String where = IMHelper.UID_MESSAGEID_SELECTION;
		String[] selectionArgs = new String[]{mUid, String.valueOf(mId)};
		if (mTargetType == IMHelper.TARGET_TYPE_QUN) {
			url = BjnoteContent.IM.CONTENT_URI_QUN;
		} else if (mTargetType == IMHelper.TARGET_TYPE_P2P){
			url = BjnoteContent.IM.CONTENT_URI_FRIEND;
		}
		int deleted =  BjnoteContent.delete(cr, url, where, selectionArgs);
		DebugUtils.logD(TAG, "deleteMessage " + mMessage + ", deleted " + (deleted > 0));
		return deleted > 0;
	}
	
	@Override
	public boolean saveInDatebase(ContentResolver cr, ContentValues addtion) {
		ContentValues values = new ContentValues();
		values.put(HaierDBHelper.IM_UNAME, mUName);
		values.put(HaierDBHelper.DATE, new Date().getTime());
		values.put(HaierDBHelper.IM_MESSAGE_STATUS, mMessageStatus);
		values.put(HaierDBHelper.IM_SERVICE_ID, mServiceId);
		values.put(HaierDBHelper.IM_TARGET_TYPE, mTargetType);
		values.put(HaierDBHelper.IM_SERVICE_TIME, mServiceDate);
		values.put(HaierDBHelper.IM_SEEN, mSeen);
		if (addtion != null) {
			values.putAll(addtion);
		}
		Uri url = null;
		String where = null;
		String[] selectionArgs = null;
		if (mTargetType == IMHelper.TARGET_TYPE_QUN) {
			url = BjnoteContent.IM.CONTENT_URI_QUN;
			where = IMHelper.SERVICEID_QUN_SELECTION;
			selectionArgs = new String[]{mServiceId, String.valueOf(mTargetType), mTarget};
		} else if (mTargetType == IMHelper.TARGET_TYPE_P2P){
			url = BjnoteContent.IM.CONTENT_URI_FRIEND;
			where = IMHelper.SERVICEID_FRIEND_SELECTION;
			selectionArgs = new String[]{mServiceId, mUid, mTarget};
		}
		long id = isExsited(cr, url, where, selectionArgs);
		if (id != -1) {
			int updated = cr.update(url, values, where, selectionArgs);
			if (updated > 0) {
				DebugUtils.logD(TAG, "saveInDatebase update exsited Id#" + id);
			} else {
				DebugUtils.logD(TAG, "saveInDatebase failly update exsited Id#" + id);
			}
			return updated > 0;
		} else {
			values.put(HaierDBHelper.IM_TARGET, mTarget);
			values.put(HaierDBHelper.IM_TEXT, mMessage);
			values.put(HaierDBHelper.IM_UID, mUid);
			Uri data = cr.insert(url, values);
			if (data != null) {
				mId = ContentUris.parseId(data);
				DebugUtils.logD(TAG, "saveInDatebase insert Id#" + mId);
			} else {
				DebugUtils.logD(TAG, "saveInDatebase failly insert ServiceId#" + mServiceId);
			}
			return data != null;
		}
	}
	/**
	 * 通常我们发出一条消息的时候会调用该方法
	 * @param cr
	 * @param addtion
	 * @return
	 */
	public boolean saveInDatebaseWithoutCheckExisted(ContentResolver cr, ContentValues addtion) {
		ContentValues values = new ContentValues();
		values.put(HaierDBHelper.IM_UNAME, mUName);
		mLocalCreateDate = new Date().getTime();
		values.put(HaierDBHelper.DATE, mLocalCreateDate);
		values.put(HaierDBHelper.IM_MESSAGE_STATUS, mMessageStatus);
		values.put(HaierDBHelper.IM_SERVICE_ID, mServiceId);
		values.put(HaierDBHelper.IM_TARGET_TYPE, mTargetType);
		values.put(HaierDBHelper.IM_SERVICE_TIME, mServiceDate);
		values.put(HaierDBHelper.IM_SEEN, mSeen);
		if (addtion != null) {
			values.putAll(addtion);
		}
		values.put(HaierDBHelper.IM_TARGET, mTarget);
		values.put(HaierDBHelper.IM_TEXT, mMessage);
		values.put(HaierDBHelper.IM_UID, mUid);
		Uri url = null;
		if (mTargetType == IMHelper.TARGET_TYPE_QUN) {
			url = BjnoteContent.IM.CONTENT_URI_QUN;
		} else if (mTargetType == IMHelper.TARGET_TYPE_P2P){
			url = BjnoteContent.IM.CONTENT_URI_FRIEND;
		}
		Uri uri = cr.insert(url, values);
		if (uri != null) {
			DebugUtils.logD(TAG, "saveInDatebase insert ServiceId#" + mServiceId);
			mId = ContentUris.parseId(uri);
		} else {
			DebugUtils.logD(TAG, "saveInDatebase failly insert ServiceId#" + mServiceId);
		}
		return mId > -1;
	}
	
	public boolean updateInDatebase(ContentResolver cr, ContentValues addtion) {
		ContentValues values = new ContentValues();
		values.put(HaierDBHelper.DATE, new Date().getTime());
		values.put(HaierDBHelper.IM_MESSAGE_STATUS, mMessageStatus);
		values.put(HaierDBHelper.IM_SERVICE_ID, mServiceId);
		values.put(HaierDBHelper.IM_SERVICE_TIME, mServiceDate);
		values.put(HaierDBHelper.IM_SEEN, mSeen);
		if (addtion != null) {
			values.putAll(addtion);
		}
		int updated = 0;
		if (mTargetType == IMHelper.TARGET_TYPE_QUN) {
			updated = cr.update(BjnoteContent.IM.CONTENT_URI_QUN, values, ID_SELECTION, new String[]{String.valueOf(mId)});
		} else if (mTargetType == IMHelper.TARGET_TYPE_P2P){
			updated = cr.update(BjnoteContent.IM.CONTENT_URI_FRIEND, values, ID_SELECTION, new String[]{String.valueOf(mId)});
		}
		
		if (updated > 0) {
			DebugUtils.logD(TAG, "updateInDatebase Id#" + mId);
		} else {
			DebugUtils.logD(TAG, "updateInDatebase failly Id#" + mId);
		}
		return updated > 0;
	}
	
	private long isExsited(ContentResolver cr, Uri uri, String where, String[] selectionArgs) {
		Cursor c = cr.query(uri, ID_PROJECTION, where, selectionArgs, null);
		if (c != null) {
			if (c.moveToNext()) {
				return c.getLong(0); 
			}
			c.close();
		}
		return -1;
	}


}
