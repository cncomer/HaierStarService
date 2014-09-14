package com.bestjoy.app.haierstartservice.account;

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

import com.bestjoy.app.haierstartservice.HaierServiceObject;
import com.bestjoy.app.haierstartservice.database.BjnoteContent;
import com.bestjoy.app.haierstartservice.database.HaierDBHelper;
import com.shwy.bestjoy.utils.DebugUtils;
import com.shwy.bestjoy.utils.InfoInterface;
import com.shwy.bestjoy.utils.InfoInterfaceImpl;
import com.shwy.bestjoy.utils.NetworkUtils;
/**
 * 该类用于选择品牌后解析型号.
 * [{
        "MN": "索尼爱立信W902", 
        "KY": "1010Q005X"
    }]   

 * @author chenkai
 *
 */
public class XinghaoObject extends InfoInterfaceImpl{
	private static final String TAG = "XinghaoObject";
	public static final String[] XINGHAO_PROJECTION = new String[]{
		HaierDBHelper.ID,
		HaierDBHelper.DEVICE_XINGHAO_MN,
		HaierDBHelper.DEVICE_XINGHAO_KY,
		HaierDBHelper.DEVICE_XINGHAO_PCODE,
		HaierDBHelper.DEVICE_XINGHAO_WY
	};
	public static final String XINGHAO_CODE_SELECTION = HaierDBHelper.DEVICE_XINGHAO_PCODE + "=?";
	public static final String XINGHAO_MN_SELECTION = HaierDBHelper.DEVICE_XINGHAO_MN + "=?";
	public static final String XINGHAO_KY_SELECTION = HaierDBHelper.DEVICE_XINGHAO_KY + "=?";
	
	public static final String XINGHAO_CODE_MN_KY_SELECTION = XINGHAO_CODE_SELECTION + " and " + XINGHAO_MN_SELECTION + " and " + XINGHAO_KY_SELECTION;
	public String mMN, mKY, mWY;
	public String mPinpaiCode;
	
	public static final String UPDATE_XINGHAO = HaierServiceObject.SERVICE_URL + "GetXinHaoByPinPai.ashx?Code=";
    public static String getUpdateUrl(String pinpaiCode) {
    	StringBuilder sb = new StringBuilder(UPDATE_XINGHAO);
    	sb.append(pinpaiCode);
    	return sb.toString();
    }
	public static List<InfoInterface> parse(InputStream is, String pinpaiCode) {
		 List<InfoInterface> list = new ArrayList<InfoInterface>();
		if (is == null) {
			return list;
		}
		try {
			JSONArray jsonArray = new JSONArray(NetworkUtils.getContentFromInput(is));
			int len = jsonArray.length();
			 list = new ArrayList<InfoInterface>(len);
			if (len > 0) {
				JSONObject object = null;
				XinghaoObject xinghaoObject = null;
				for(int index = 0; index < len; index++) {
					object = jsonArray.getJSONObject(index);
					xinghaoObject = new XinghaoObject();
					xinghaoObject.mMN = object.getString("MN");
					xinghaoObject.mKY = object.getString("KY");
					xinghaoObject.mWY = object.getString("WY");
					xinghaoObject.mPinpaiCode = pinpaiCode;
					list.add(xinghaoObject);
				}
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return list;
	}
	
	@Override
	public boolean saveInDatebase(ContentResolver cr, ContentValues addtion) {
		ContentValues values = new ContentValues();
		if (addtion != null) {
			values.putAll(addtion);
		}
		values.put(HaierDBHelper.DEVICE_XINGHAO_PCODE, mPinpaiCode);
		values.put(HaierDBHelper.DEVICE_XINGHAO_MN, mMN);
		values.put(HaierDBHelper.DEVICE_XINGHAO_KY, mKY);
		values.put(HaierDBHelper.DEVICE_XINGHAO_WY, mWY);
		values.put(HaierDBHelper.DATE, new Date().getTime());
		
		if (isExsited(cr, mPinpaiCode, mMN, mKY) > 0) {
			int updated = cr.update(BjnoteContent.XingHao.CONTENT_URI, values, XINGHAO_CODE_MN_KY_SELECTION, new String[]{mPinpaiCode, mMN, mKY});
			if (updated > 0) {
				DebugUtils.logD(TAG, "saveInDatebase update exsited ky#" + mKY);
				return true;
			} else {
				DebugUtils.logD(TAG, "saveInDatebase failly update exsited ky#" + mKY);
			}
		} else {
			Uri uri = cr.insert(BjnoteContent.XingHao.CONTENT_URI, values);
			if (uri != null) {
				DebugUtils.logD(TAG, "saveInDatebase insert ky#" + mKY);
				return true;
			} else {
				DebugUtils.logD(TAG, "saveInDatebase failly insert ky#" + mKY);
			}
		}
		return false;
	}
	
	private long isExsited(ContentResolver cr, String pinpaiCode, String mn, String ky) {
		long id = -1;
		Cursor c = cr.query(BjnoteContent.XingHao.CONTENT_URI, XINGHAO_PROJECTION, XINGHAO_CODE_MN_KY_SELECTION, new String[]{pinpaiCode, mn, ky}, null);
		if (c != null) {
			if (c.moveToNext()) {
				id = c.getLong(0);
			}
			c.close();
		}
		return id;
	}

}
