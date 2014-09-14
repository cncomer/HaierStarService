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

import java.util.LinkedList;
import java.util.List;

import android.text.TextUtils;

import com.shwy.bestjoy.utils.Contents;
import com.shwy.bestjoy.utils.DebugUtils;

public class AddressBookParsedResult extends ParsedResult {

  private final String[] names;
  private final String pronunciation;
  private final String[] phoneNumbers;
  private final String[] emails;
  private final String note;
  private final String[] addresses;
  private final String org;
  private final String birthday;
  private final String title;
  private String[] url;
  private final byte[] photo;//二进制图片
  private String bid;//bid uri
  private final String auth = "19860613814";
  private String mCloudMM;
  /**联系人分组名称*/
  private String mCategory;
  
  private String mTag;
  
  
  private String[] mMobilePhoneNumbers;
  /**是否是商家名片*/
  private boolean mIsMerchant = false;
  /**是否支持商家名片处理*/
  private static final boolean SUPPORT_MERCHAT = false;
  
  /**
   * 
   * @param names
   * @param pronunciation
   * @param phoneNumbers
   * @param emails
   * @param note
   * @param addresses
   * @param org
   * @param birthday
   * @param title
   * @param url
   * @param photo
   * @param bid bid url
   * @param mCategory
   */
  public AddressBookParsedResult(String[] names,
          String pronunciation,
          String[] phoneNumbers,
          String[] emails,
          String note,
          String[] addresses,
          String org,
          String birthday,
          String title,
          String[] webUrl,
          byte[] photo,
          String mm,
          String category,
          String tag) {
	  super();
	  this.names = names;
	  this.pronunciation = pronunciation;
	  this.phoneNumbers = phoneNumbers;
	  this.emails = emails;
	  this.note = note;
	  this.addresses = addresses;
	  this.org = org;
	  this.birthday = birthday;
	  this.title = title;
	  this.photo = photo;
	  this.bid = mm;
	  this.url = webUrl;
	  this.mTag = tag;
	  if (TextUtils.isEmpty(category)) {
		  // 默认分组
		  mCategory = "System Group: My Contacts";
	  } else {
		  mCategory = category;
	  }
	  int index= 0;
	  if (url != null) {
		  for (String each : url) {
			  if (!TextUtils.isEmpty(each)) {
				  DebugUtils.logD("AddressBookParsedResult", "url: " + url[index]);
				  mCloudMM = getMmFromCloudUrl(each);
				  if (!TextUtils.isEmpty(mCloudMM)) {
				      bid = mCloudMM;
					  url[index] = getDirectCloudUrl();
					  DebugUtils.logD("AddressBookParsedResult", "mCloudUrl: " + url[index]);
					  break;
				  }
			  }
			  index++;
		  }
	  }
	  mIsMerchant = false;
	  if (SUPPORT_MERCHAT && !TextUtils.isEmpty(bid) 
			  && Contents.MingDang.isMingDangNo(bid) 
			  && bid.endsWith(Contents.MingDang.FLAG_MERCHANT)) {
		  mIsMerchant = true;
	  }
	  resetParsedResultType(getParsedResultType(bid));
	  DebugUtils.logD("AddressBookParsedResult", "bid: " + this.bid);
		  
  }
  
  private ParsedResultType getParsedResultType(String mm) {
	  return mIsMerchant ? ParsedResultType.MYLIFE:ParsedResultType.ADDRESSBOOK;
  }
  
  public boolean isMerchant() {
	  if (!SUPPORT_MERCHAT) {
		  return false;
	  }
	  return mIsMerchant;
  }
  
  public static String getMmFromCloudUrl(String cloudUrl) {
	  cloudUrl = cloudUrl.toLowerCase();
	  if (!cloudUrl.startsWith("http://")) {
		  cloudUrl = "http://" + cloudUrl;
	  }
	  return Contents.MingDang.isCloudUri(cloudUrl);
  }
  
  public String[] getNames() {
    return names;
  }

  public String getFirstName() {
	  if (names != null && names.length > 0) {
		  return names[0];
	  }
	  return null;
  }
  /**
   * In Japanese, the name is written in kanji, which can have multiple readings. Therefore a hint
   * is often provided, called furigana, which spells the name phonetically.
   *
   * @return The pronunciation of the getNames() field, often in hiragana or katakana.
   */
  public String getPronunciation() {
    return pronunciation;
  }
  
  public boolean hasPhoneNumbers() {
	  return phoneNumbers != null && phoneNumbers.length > 0;
  }

  public String[] getPhoneNumbers() {
    return phoneNumbers;
  }
  /**返回手机号码*/
  public String[] getMobilePhoneNumbers() {
	  if (phoneNumbers == null) {
		  return null;
	  }
      if (mMobilePhoneNumbers == null) {
    	  List<String> mobileNumberList = new LinkedList<String>();
    	  for (String value : phoneNumbers) {
	  		  //if length of phone number is more than 11, we treat it as mobile type.
	  			boolean isMobile = value.length()>=11 && value.startsWith("1");
	  			if (isMobile) {
	  				mobileNumberList.add(value);
	  			}
  	      }
    	  mMobilePhoneNumbers =  mobileNumberList.toArray(new String[0]);
	  }
      return mMobilePhoneNumbers;
	  
  }

  public String[] getEmails() {
    return emails;
  }
  
  public String getFirstEmail() {
	  if (emails != null && emails.length > 0) {
		  return emails[0];
	  }
	  return null;
  }

  public String getNote() {
    return note;
  }

  public String[] getAddresses() {
    return addresses;
  }
  
  public boolean hasAddresses() {
	  return addresses != null && addresses.length > 0;
  }
  
  public String getFirstAddress() {
	  if (addresses != null && addresses.length > 0) {
		  return addresses[0];
	  }
	  return null;
  }

  public String getTitle() {
    return title;
  }

  public String getOrg() {
    return org;
  }

  public String[] getURL() {
    return url;
  }
  
  public String getCloudUrl() {
	  return Contents.MingDang.buildCloudUri(mCloudMM);
  }
  
  public String getDirectCloudUrl() {
	  return Contents.MingDang.buildDirectCloudUri(mCloudMM);
  }
  
  public String getDownloadUrl() {
	  return Contents.MingDang.buildDownloadUri(mCloudMM);
  }
  
  public String getCloudMM() {
	  return mCloudMM;
  }
  
  public String getCategory() {
	  return mCategory;
  }
  /**
   * 如果二维码有X-BM，使用作为bid:,没有则使用云名片网址中的17位MM号码
   * @return
   */
  public String getMM() {
	  return bid;
  }
  
  public byte[] getPhoto() {
	    return photo;
  }
  
  public String getBid() {
	    return bid;
  }

  public String getAuth() {
	  return auth;
  }
  
  /**
   * @return birthday formatted as yyyyMMdd (e.g. 19780917)
   */
  public String getBirthday() {
    return birthday;
  }
  
  public String getTag() {
	    return mTag;
	  }

  public String getDisplayResult() {
    StringBuilder result = new StringBuilder(100);
    maybeAppend(names, result);
    maybeAppend(pronunciation, result);
    maybeAppend(title, result);
    maybeAppend(org, result);
    maybeAppend(addresses, result);
    maybeAppend(phoneNumbers, result);
    maybeAppend(emails, result);
    maybeAppend(url, result);
    maybeAppend(birthday, result);
    maybeAppend(note, result);
    maybeAppend(mCloudMM, result);
    maybeAppend(getCloudUrl(), result);
//    maybeAppend(getDownloadUrl(), result);
    return result.toString();
  }

}
