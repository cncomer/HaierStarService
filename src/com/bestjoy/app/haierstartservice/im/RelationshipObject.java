package com.bestjoy.app.haierstartservice.im;

import java.io.InputStream;
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
import android.os.Parcel;
import android.os.Parcelable;

import com.bestjoy.app.haierstartservice.HaierServiceObject.HaierResultObject;
import com.bestjoy.app.haierstartservice.database.BjnoteContent;
import com.bestjoy.app.haierstartservice.database.HaierDBHelper;
import com.shwy.bestjoy.utils.DebugUtils;
import com.shwy.bestjoy.utils.InfoInterface;
import com.shwy.bestjoy.utils.NetworkUtils;
import com.shwy.bestjoy.utils.PageInfo;

public class RelationshipObject implements InfoInterface, Parcelable{
	private static final String TAG = "RelationshipObject";

	public String mUID, mLeiXin, mXingHao, mCell, mTarget, mTargetName, mBuyDate, mRelationshipServiceId, mRelationshipId, mLocalDate;
	public int mTargetType = IMHelper.TARGET_TYPE_P2P;

	/**
	 * {"StatusCode":"1",
	 * "StatusMessage":"登录成功",
	 * "Data":{"total":2,"rows":[{"suid":"682038","userName":"王坤","LeiXin":"手机","XingHao":"A6","cell":"18621951099","BuyDate":"20140911","id":"2","uid":"42300"},{"suid":"682038","userName":"123","LeiXin":"手机","XingHao":"A6","cell":"18611986102","BuyDate":"20140911","id":"1","uid":"607421"}]}}
	 */
	public static List<RelationshipObject> parseList(InputStream is, PageInfo pageInfo) {
		HaierResultObject serviceResultObject = HaierResultObject.parse(NetworkUtils.getContentFromInput(is));
		List<RelationshipObject> list = new ArrayList<RelationshipObject>();
		if (serviceResultObject.isOpSuccessfully()) {
			try {
				JSONObject jsonObject = serviceResultObject.mJsonData;
				pageInfo.mTotalCount = jsonObject.getInt("total");
				JSONArray rows = jsonObject.getJSONArray("rows");
				long rowsLen = rows.length();
				
				DebugUtils.logD(TAG, "parseList find rows " + rowsLen);
				for(int index = 0; index < rowsLen; index++) {
					list.add(parse(rows.getJSONObject(index)));
				}
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}
		return list;
	}
	
	public static RelationshipObject parse(JSONObject row) throws JSONException {
		RelationshipObject relastionship = new RelationshipObject();
		relastionship.mUID = row.getString("suid");
		relastionship.mTargetName = row.getString("userName");
		relastionship.mLeiXin = row.getString("LeiXin");
		relastionship.mXingHao = row.getString("XingHao");
		
		relastionship.mCell = row.getString("cell");
		relastionship.mBuyDate = row.getString("BuyDate");
		relastionship.mTarget = row.getString("uid");
		
		relastionship.mRelationshipServiceId = row.getString("id");
		
		return relastionship;
	}
	
	public static RelationshipObject getFromCursor(Cursor cursor) {
		RelationshipObject object = new RelationshipObject();
		object.mUID = cursor.getString(BjnoteContent.RELATIONSHIP.INDEX_RELASTIONSHIP_UID);
		object.mTargetType = cursor.getInt(BjnoteContent.RELATIONSHIP.INDEX_RELASTIONSHIP_TARGET_TYPE);
		object.mTarget = cursor.getString(BjnoteContent.RELATIONSHIP.INDEX_RELASTIONSHIP_TARGET);
		object.mTargetName = cursor.getString(BjnoteContent.RELATIONSHIP.INDEX_RELASTIONSHIP_UNAME);
		object.mLeiXin = cursor.getString(BjnoteContent.RELATIONSHIP.INDEX_RELASTIONSHIP_LEIXING);
		object.mXingHao = cursor.getString(BjnoteContent.RELATIONSHIP.INDEX_RELASTIONSHIP_XINGHAO);
		object.mCell = cursor.getString(BjnoteContent.RELATIONSHIP.INDEX_RELASTIONSHIP_CELL);
		
		object.mBuyDate = cursor.getString(BjnoteContent.RELATIONSHIP.INDEX_RELASTIONSHIP_BUYDATE);
		
		object.mRelationshipServiceId = cursor.getString(BjnoteContent.RELATIONSHIP.INDEX_RELASTIONSHIP_SERVICE_ID);
		object.mRelationshipId = cursor.getString(BjnoteContent.RELATIONSHIP.INDEX_RELASTIONSHIP_ID);
		object.mLocalDate = cursor.getString(BjnoteContent.RELATIONSHIP.INDEX_RELASTIONSHIP_LOCAL_DATE);
		
		return object;
	}

	public static final String WHERE = HaierDBHelper.RELATIONSHIP_SERVICE_ID + "=? and " + HaierDBHelper.RELATIONSHIP_UID + "=? and " + HaierDBHelper.RELATIONSHIP_TARGET + "=?";
	@Override
	public boolean saveInDatebase(ContentResolver cr, ContentValues addtion) {
		ContentValues values = new ContentValues();
		values.put(HaierDBHelper.RELATIONSHIP_UID, mUID);
		values.put(HaierDBHelper.RELATIONSHIP_NAME, mTargetName);
		values.put(HaierDBHelper.RELATIONSHIP_TYPE, mTargetType);
		values.put(HaierDBHelper.RELATIONSHIP_TARGET, mTarget);
		values.put(HaierDBHelper.RELATIONSHIP_SERVICE_ID, mRelationshipServiceId);
		values.put(HaierDBHelper.DATA1, mLeiXin);
		values.put(HaierDBHelper.DATA2, mXingHao);
		values.put(HaierDBHelper.DATA3, mCell);
		values.put(HaierDBHelper.DATA4, mBuyDate);
		values.put(HaierDBHelper.DATE, new Date().getTime());
		String[] selectionArgs = new String[]{mRelationshipServiceId, mUID, mTarget};
		//首先判断是不是存在数据
		long id = BjnoteContent.existed(cr, BjnoteContent.RELATIONSHIP.CONTENT_URI, WHERE, selectionArgs);
		if (id > -1) {
			//已存在，我们仅仅是更新操作
			int update = BjnoteContent.update(cr, BjnoteContent.RELATIONSHIP.CONTENT_URI, values, BjnoteContent.ID_SELECTION, new String[]{});
			DebugUtils.logD(TAG, "saveInDatebase() update exsited serviceId# " + mRelationshipServiceId + ", name=" + mTargetName + ", updated " + update);
			return update > 0;
		} else {
			Uri uri = BjnoteContent.insert(cr, BjnoteContent.RELATIONSHIP.CONTENT_URI, values);
			DebugUtils.logD(TAG, "saveInDatebase() insert serviceId# " + mRelationshipServiceId + ", name=" + mTargetName + ", uri " + uri);
			return uri != null;
		}
	}
	
     public static final Parcelable.Creator<RelationshipObject> CREATOR  = new Parcelable.Creator<RelationshipObject>() {
	 public RelationshipObject createFromParcel(Parcel in) {
	     return new RelationshipObject(in);
	 }
	
	 public RelationshipObject[] newArray(int size) {
	     return new RelationshipObject[size];
	 }
	};
	
	public RelationshipObject(Parcel in) {
		mUID = in.readString();
		mTarget = in.readString();
		mTargetName = in.readString();
		mLeiXin = in.readString();
		mXingHao = in.readString();
		mCell = in.readString();
		mBuyDate = in.readString();
		mRelationshipServiceId = in.readString();
		mRelationshipId = in.readString();
		mLocalDate = in.readString();
		mTargetType = in.readInt();
	}
	
	public RelationshipObject(){};


	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeString(mUID);
		dest.writeString(mTarget);
		dest.writeString(mTargetName);
		dest.writeString(mLeiXin);
		dest.writeString(mXingHao);
		dest.writeString(mCell);
		dest.writeString(mBuyDate);
		dest.writeString(mRelationshipServiceId);
		dest.writeString(mRelationshipId);
		dest.writeString(mLocalDate);
		dest.writeInt(mTargetType);
	}
	
	
	
	
	
}
