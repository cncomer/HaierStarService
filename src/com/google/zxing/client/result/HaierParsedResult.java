/*
 * Copyright 2007 ZXing authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.zxing.client.result;

import java.io.IOException;
import java.io.InputStream;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import android.text.TextUtils;

import com.bestjoy.app.haierstartservice.HaierServiceObject;
import com.bestjoy.app.haierstartservice.account.BaoxiuCardObject;
import com.bestjoy.app.utils.DebugUtils;
import com.google.zxing.BarcodeFormat;
import com.shwy.bestjoy.utils.InfoInterfaceImpl;

/**
 * A simple result type encapsulating a string that has no further
 * interpretation.
 * 
 * @author chenkai
 */
public final class HaierParsedResult extends ParsedResult {

	private static final String TAG = "HaierParsedResult";
  private final String text;
  private final String param;
  private BarcodeFormat mBarcodeFormat;
  private ResultBaoxiuCardType mResultBaoxiuCardType = ResultBaoxiuCardType.Haier;
  public static enum ResultBaoxiuCardType {
	  Haier,
	  General, //通用
  }
  
  public HaierParsedResult(String text, String param) {
    super(ParsedResultType.BXK);
    this.text = text;
    this.param = param;
    mBarcodeFormat = BarcodeFormat.QR_CODE;
  }
  
  public HaierParsedResult(String text, String param, BarcodeFormat barcodeFormat) {
	    super(ParsedResultType.BXK);
	    this.text = text;
	    this.param = param;
	    mBarcodeFormat = barcodeFormat;
	  }

  public HaierParsedResult(String text, String param, BarcodeFormat barcodeFormat, ResultBaoxiuCardType baoxiuCardType) {
	    super(ParsedResultType.BXK);
	    this.text = text;
	    this.param = param;
	    mBarcodeFormat = barcodeFormat;
	    mResultBaoxiuCardType = baoxiuCardType;
	  }

  public String getParam() {
    return param;
  }
  
  public BarcodeFormat getBarcodeFormat() {
	  return mBarcodeFormat;
  }

  @Override
  public String getDisplayResult() {
    return text;
  }
  /**
   * 返回扫码内容的保修卡类型
   * @return
   */
  public ResultBaoxiuCardType getResultBaoxiuCardType() {
	return mResultBaoxiuCardType;
  }
  public void setResultBaoxiuCardType(ResultBaoxiuCardType baoxiuCardType) {
		mResultBaoxiuCardType = baoxiuCardType;
   }
  
  public static class HaierBaoxiuCardParser extends InfoInterfaceImpl{
	  /**
	   * <Data>
	        <ResultMessage>
				<MsgCode>1</MsgCode>
				<MsgContent>成功</MsgContent>
				<MsgTime>2014/4/26 16:14:29</MsgTime>
			</ResultMessage>
			<ProductInfo>
				<Brand>海尔</Brand>             品牌
				<Class>洗共体—双桶洗衣机</Class>   名称
				<Type>XPB80-1186BS</Type>       型号
				<BarCode>CA0KQ200M00PAD4AY005</BarCode> 编码
				<WY>3<WY>  保修期
			</ProductInfo>
		</Data>
	   * @param content
	   * @return 返回错误信息，如果null,则表示检索成功
	   */
	  public static String parse(InputStream is, BaoxiuCardObject baoxiuCardObject) {
			 DebugUtils.logParser(TAG, "Start parse");
			 String error = null;
			 String msgCode = null;
			try {
				XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
				factory.setNamespaceAware(true);
		        XmlPullParser parser = factory.newPullParser();
		        parser.setInput(is, "utf-8");
		        beginDocument(parser, "Data");
		        int type = 0;
		        while ((type=parser.next()) != XmlPullParser.END_DOCUMENT) {
		        	if (type == XmlPullParser.START_TAG) {
		        		String tag = parser.getName();
		                DebugUtils.logParser(TAG, "Start tag "+tag);
		 	       	    if ("MsgCode".equals(tag)) {
		 	       	    	//如果返回的
		 	       	    	msgCode = nextTextElement(parser);
		 	       	        if (!"1".equals(msgCode)) {
		 	       	        	DebugUtils.logParser(TAG, "MsgCode = " + msgCode);
		 	       	        }
		 	       	    } else if ("MsgContent".equals(tag)) {
		 	       	    	if (!"1".equals(msgCode)) error = nextTextElement(parser);
		       		    } else if ("Brand".equals(tag)) {
		 	       	    	baoxiuCardObject.mPinPai = nextTextElement(parser);
		 	       	        baoxiuCardObject.mBXPhone =HaierServiceObject.getBXPhoneHaierPinpaiGenaral(baoxiuCardObject.mPinPai, "");
		       		    } else if ("Class".equals(tag)) {
		       		    	String classContent = nextTextElement(parser);
		       		    	if (!TextUtils.isEmpty(classContent)) {
		       		    		int index = classContent.indexOf("—");
		       		    		if (index > 0) {
		       		    			baoxiuCardObject.mLeiXin = classContent.substring(index+1);
		       		    		} else {
		       		    			baoxiuCardObject.mLeiXin = classContent;
		       		    		}
		       		    	}
		       		    	
		       		    } else if ("Type".equals(tag)) {
		       		    	baoxiuCardObject.mXingHao = nextTextElement(parser);
		       		    } else if ("BarCode".equals(tag)) {
		       		    	baoxiuCardObject.mSHBianHao = nextTextElement(parser);
		       		    } else if ("WY".equals(tag)) {
		       		    	baoxiuCardObject.mWY = nextTextElement(parser);
		       		    } else if ("CELL".equals(tag)) {
		       		    	baoxiuCardObject.mBXPhone = nextTextElement(parser);
		       		    }
		        	}
		        }
			} catch (XmlPullParserException e) {
				e.printStackTrace();
				error = e.getMessage();
			} catch (IOException e) {
				e.printStackTrace();
				error = e.getMessage();
			}
			return error;
		}
 
	  
  }
  
}
