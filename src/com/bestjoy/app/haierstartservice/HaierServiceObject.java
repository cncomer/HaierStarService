package com.bestjoy.app.haierstartservice;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.text.TextUtils;

import com.bestjoy.app.haierstartservice.account.BaoxiuCardObject;
import com.bestjoy.app.utils.DebugUtils;
import com.shwy.bestjoy.utils.SecurityUtils;
import com.shwy.bestjoy.utils.UrlEncodeStringBuilder;


public class HaierServiceObject {

	public static final String SERVICE_URL = "http://115.29.231.29/Haier/";
	public static final String PRODUCT_AVATOR_URL= "http://115.29.231.29/proimg/";
	public static final String PRODUCT_GENERAL_AVATOR_URL= "http://115.29.231.29/pimg/";
	/**发票路径的前缀*/
	public static final String FAPIAO_URL = "http://115.29.231.29/Fapiao/";
	
	public static final String CARD_DELETE_URL = SERVICE_URL + "DeleteBaoXiuByBIDUID.ashx?";
	
	public static final String HOME_DELETE_URL = SERVICE_URL + "DeleteAddressByAID.ashx?";
	
	private static String mHaierPinpaiName;
	public static final String BX_PHONE_HAIER = "400699999";
	
	private static String mKasadiPinpaiName;
	public static final String BX_PHONE_KASADI = "4006399699";
	
	private static String mTongShuaiPinpaiName;
	public static final String BX_PHONE_TONGSHUAI = "4006999999";
	
	public static final String DES_PASSWORD = "Haier@1.";
	public static void setContext(Context context) {
		mHaierPinpaiName = context.getString(R.string.pinpai_haier);
		mKasadiPinpaiName = context.getString(R.string.pinpai_kasadi);
		mTongShuaiPinpaiName = context.getString(R.string.pinpai_tongshuai);
	}
	public static boolean isHaierPinpai(String pinpaiName) {
		return mHaierPinpaiName.equals(pinpaiName);
	}
	/**
	 * 是否是卡萨帝品牌
	 * @param pinpaiName
	 * @return
	 */
	public static boolean isKasadiPinpai(String pinpaiName) {
		return mKasadiPinpaiName.equals(pinpaiName);
	}
	/**
	 * 是否是统帅品牌
	 * @param pinpaiName
	 * @return
	 */
	public static boolean isTongShuaiPinpai(String pinpaiName) {
		return mTongShuaiPinpaiName.equals(pinpaiName);
	}
	/**
	 * 是否是海尔品牌
	 * @param pinpaiName
	 * @return
	 */
	public static boolean isHaierPinpaiGenaral(String pinpaiName) {
		return isHaierPinpai(pinpaiName)
				|| isKasadiPinpai(pinpaiName)
				|| isTongShuaiPinpai(pinpaiName);
	}
	/**
	 * 返回品牌的售后服务电话
	 * @param pinpaiName
	 * @param defaultValue
	 * @return
	 */
	public static String getBXPhoneHaierPinpaiGenaral(String pinpaiName, String defaultValue) {
		if (isHaierPinpai(pinpaiName)) {
			return BX_PHONE_HAIER;
		} else if (isKasadiPinpai(pinpaiName)) {
			return BX_PHONE_KASADI;
		} else if (isTongShuaiPinpai(pinpaiName)) {
			return BX_PHONE_TONGSHUAI;
		} 
		return defaultValue;
	}

	/***
	   * 产品图片网址  http://115.29.231.29/pimg/5070A000A.jpg
	   * @return
	   */
	public static String getProdcutGeneralAvatorUrl(String ky) {
		  StringBuilder sb = new StringBuilder(PRODUCT_GENERAL_AVATOR_URL);
		  sb.append(ky).append(".jpg");
		  return sb.toString();
	}
	/***
	   * 产品图片网址  http://115.29.231.29/proimg/507/5070A000A.jpg
	   * @return
	   */
	public static String getProdcutAvatorUrl(String ky) {
		if (ky.length() > 3) {
			String ky3 = ky.substring(0, 3);
			  StringBuilder sb = new StringBuilder(PRODUCT_AVATOR_URL);
			  sb.append(ky3).append("/").append(ky).append(".jpg");
			  return sb.toString(); 
		} else {
			return getProdcutGeneralAvatorUrl(ky);
		}
		
	}
	
	//modify by chenkai, 修改发票后台同步修改新建更新和登录后台, 20140622 begin
		public static String getCreateBaoxiucardUri() {
			StringBuilder sb = new StringBuilder(SERVICE_URL);
			sb.append("20140625/AddBaoXiu.ashx");
			return sb.toString();
		}
		
		public static String getUpdateBaoxiucardUri() {
			StringBuilder sb = new StringBuilder(SERVICE_URL);
			sb.append("20140625/updateBaoXiu.ashx");
			return sb.toString();
		}
		
		/**
		 * 发票路径为http://www.dzbxk.com/fapiao/图片名.jpg, 图片名=md5(AID+UID)
		 * @param aid
		 * @param bid
		 * @return
		 */
		public static String getBaoxiucardFapiao(String photoId) {
			//modify by chenkai, 20140701, 将发票地址存进数据库（不再拼接），增加海尔奖励延保时间 begin
			//StringBuilder sb = new StringBuilder(FAPIAO_URL);
			//sb.append(photoId).append(".jpg");
			//return sb.toString();
			return BaoxiuCardObject.getBaoxiuCardObject().getFapiaoServicePath();
			//modify by chenkai, 20140701, 将发票地址存进数据库（不再拼接），增加海尔奖励延保时间 end
		}
		//modify by chenkai, 修改发票后台同步修改新建更新和登录后台, 20140622 end
	/**
	 * 删除保修数据： serverIP/Haier/DeleteBaoXiuByBIDUID.ashx
	 * @param BID:保修ID
	 * @param UID:用户ID
	 * @return
	 */
	public static String getBaoxiuCardDeleteUrl(String bid, String uid) {
		StringBuilder sb = new StringBuilder(CARD_DELETE_URL);
		sb.append("BID=").append(bid)
		.append("&UID=").append(uid);
		return sb.toString();
	}
	
	//add by chenkai, 20140701, 将登录和更新调用的地址抽离出来，以便修改 begin
	/**
	 * 返回登陆调用URL
	 * @param tel
	 * @param pwd
	 * @return
	 */
	public static String getLoginOrUpdateUrl(String tel, String pwd) {
		UrlEncodeStringBuilder sb = new UrlEncodeStringBuilder(HaierServiceObject.SERVICE_URL);
		sb.append("Start/Slogin.ashx?cell=").append(tel)
		.append("&pwd=").appendUrlEncodedString(pwd);
		return sb.toString();
	}
	//add by chenkai, 20140701, 将登录和更新调用的地址抽离出来，以便修改 end
	
	//add by chenkai, 20140726, 将注册调用的地址抽离出来，以便修改 begin
		/**
		 * 返回登陆调用URL
		 * @param para
		 * @param jsonString
		 * @return
		 */
		public static String getRegisterUrl(String para, String jsonString) {
			UrlEncodeStringBuilder sb = new UrlEncodeStringBuilder(HaierServiceObject.SERVICE_URL);
			sb.append("20140826/Register.ashx?")
			.append(para).append("=").appendUrlEncodedString(jsonString);
			return sb.toString();
		}
		//add by chenkai, 20140726, 将注册调用的地址抽离出来，以便修改 end
	
		//add by chenkai, 20140726, 将发送短信抽离出来，以便修改 begin
		/**
		 * 返回登陆调用URL
		 * @param para
		 * @param jsonString
		 * @return
		 */
		public static String getFindPasswordUrl(String para, String jsonString) {
			UrlEncodeStringBuilder sb = new UrlEncodeStringBuilder(HaierServiceObject.SERVICE_URL);
			sb.append("20140726/forgetpwd.ashx?")
			.append(para).append("=").appendUrlEncodedString(jsonString);
			return sb.toString();
		}
		
		public static String getYanzhengmaUrl(String para, String jsonString) {
			UrlEncodeStringBuilder sb = new UrlEncodeStringBuilder(HaierServiceObject.SERVICE_URL);
			sb.append("20140726/GetYanZheng.ashx?")
			.append(para).append("=").appendUrlEncodedString(jsonString);
			return sb.toString();
		}
		//add by chenkai, 20140726, 将发送短信抽离出来，以便修改 end
		
	public static class HaierResultObject {
		public int mStatusCode = 0;
		public String mStatusMessage;
		public JSONObject mJsonData;
		public String mStrData;
		public String mRawString;
		
		public static HaierResultObject parse(String content) {
			HaierResultObject resultObject = new HaierResultObject();
			if (TextUtils.isEmpty(content)) {
				DebugUtils.logD("HaierResultObject", "content is empty = " + content);
				return resultObject;
			}
			try {
				resultObject.mRawString = content;
				JSONObject jsonObject = new JSONObject(content);
				resultObject.mStatusCode = Integer.parseInt(jsonObject.getString("StatusCode"));
				resultObject.mStatusMessage = jsonObject.getString("StatusMessage");
				DebugUtils.logD("HaierResultObject", "StatusCode = " + resultObject.mStatusCode);
				DebugUtils.logD("HaierResultObject", "StatusMessage = " +resultObject.mStatusMessage);
				try {
					resultObject.mJsonData = jsonObject.getJSONObject("Data");
				} catch (JSONException e) {
					resultObject.mStrData = jsonObject.getString("Data");
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
		
		@Override
		public String toString() {
			return mRawString;
		}
	}
	
	//add by chenkai, for Usage, 2014.05.31 begin
	/**www.51cck.com/PD前9位数字/PD前13位[.htm][.pdf]*/
    public static final String GOODS_INTRODUCTION_BASE = "http://www.51cck.com/";
	/***
	   * www.51cck.com/KY前9位数字/KY.pdf
	   * @return
	   */
	  public static String getProductUsageUrl(String ky9, String ky) {
		  StringBuilder sb = new StringBuilder(GOODS_INTRODUCTION_BASE);
		  sb.append(ky9).append("/").append(ky).append(".pdf");
		  return sb.toString();
	  }
	  /***
	   * http://www.51cck.com/haier/264574251GD0N5T00W.pdf
	   * @return
	   */
	  public static String getProductUsageUrl(String file) {
		  StringBuilder sb = new StringBuilder(GOODS_INTRODUCTION_BASE);
		  sb.append("haier/").append(file);
		  return sb.toString();
	  }
	 //add by chenkai, for Usage, 2014.05.31 end
	  
	  /**
	   * 查询是否有使用说明书,http://115.29.231.29/haier/getPDfByKy.ashx?KY=2050100P1&token=df6037a3709a77279dde4334c4038178
	   * @param ky 产品的9位KY
	   * @param token Md5(KY)
	   * @return
	   */
	  public static String getProductPdfUrlForQuery(String ky) {
		  StringBuilder sb = new StringBuilder(SERVICE_URL);
		  sb.append("getPDfByKy.ashx?KY=").append(ky).append("&token=").append(SecurityUtils.MD5.md5(ky));
		  return sb.toString();
	  }
	  
	  /**
	   * 是否支持直接从服务器下发的验证码短信中提取验证码并回填
	   * @return
	   */
	  public static boolean isSupportReceiveYanZhengMa() {
		  return false;
	  }
	  
	  /**
	   * http://115.29.231.29/Haier/Apple/RegisterDevice.ashx?UID=1&pushtoken=test&devicetype=android
	   * @return
	   */
	  public static String getUpdateDeviceTokenUrl(String uid, String deviceToken, String deviceType) {
		  UrlEncodeStringBuilder sb = new UrlEncodeStringBuilder(SERVICE_URL);
		  sb.append("Apple/RegisterDevice.ashx?UID=").appendUrlEncodedString(uid)
		  .append("&pushtoken=").appendUrlEncodedString(deviceToken)
		  .append("&devicetype=").appendUrlEncodedString(deviceType);
		  return sb.toString();
		  
	  }
	  
	  
	  public static String getBaoxiucardSalesmanAvatorPreview(String mm) {
		  UrlEncodeStringBuilder sb = new UrlEncodeStringBuilder("http://www.mingdown.com/mmimage/");
		  sb.append(mm).append(".jpg");
		  return sb.toString();
	  }
	  
	  public static String updateBaoxiucardSalesmanInfo(String para, String jsonString) {
		  UrlEncodeStringBuilder sb = new UrlEncodeStringBuilder(SERVICE_URL);
		  sb.append("AddBIDMM.ashx?")
		  .append(para).append("=").appendUrlEncodedString(jsonString);
		  return sb.toString();
	  }
	  /**
	   * 从条码中识别出保修卡条码，从网络获取条码的保修卡对象信息
	   * @return
	   */
	  public static String queryBaoxiuCardUrlFromBarCode() {
		  UrlEncodeStringBuilder sb = new UrlEncodeStringBuilder(SERVICE_URL);
		  sb.append("Deal.ashx?para=");
		  return sb.toString();
	  }
	  
	  public static String buildPageQuery(String url, int pageIndex, int pageSize) {
		  StringBuilder sb = new StringBuilder(url).append('&');
		  sb.append("pageindex=").append(pageIndex).append('&');
		  sb.append("pagesize=").append(pageSize);
    	  return sb.toString();
	  }
	  /**
	   * 获取suid的关系
	   *  http://115.29.231.29/Haier/Start/GetServiceUserBaoXiu.ashx?suid=682038&pwd=844605
	   */
	  public static String getRelationshipUrl(String suid, String pwd) {
		  UrlEncodeStringBuilder sb = new UrlEncodeStringBuilder(SERVICE_URL);
		  sb.append("Start/GetServiceUserBaoXiu.ashx?suid=").append(suid).append("&pwd=").append(pwd);
		  return sb.toString();
	  }
	  
	 
	  
}
