package com.shwy.bestjoy.contacts;

import java.util.ArrayList;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import android.provider.ContactsContract.PhoneLookup;
import android.text.TextUtils;
import android.util.Log;

import com.bestjoy.app.haierstartservice.MyApplication;
import com.bestjoy.app.haierstartservice.R;
import com.bestjoy.app.utils.BeepAndVibrate;
import com.google.zxing.client.result.AddressBookParsedResult;
import com.shwy.bestjoy.utils.Contents;
import com.shwy.bestjoy.utils.DebugUtils;



public class AddrBookAddManager {
	private final static String TAG="AddrBookAddManager";
	private  Context context;
	private BeepAndVibrate media;
	private static final Uri GROUP_URI = ContactsContract.Groups.CONTENT_URI;
	private static final String[] GROUP_PROJECTION = new String[]{
    	ContactsContract.Groups._ID,          //0
    	ContactsContract.Groups.TITLE,        //1
    	ContactsContract.Groups.SYSTEM_ID,    //2
    };
	/**indicate that whether the group isn't deleted*/
	private static final String GROUP_DELETED_WHERE = ContactsContract.Groups.DELETED +"=0";
	
	private ContentResolver mCr;
	
	/**
	 * @param context 程序上下文
	 * @param media 声音和震动管理器
	 */
	public AddrBookAddManager(Context context, BeepAndVibrate media) {
		this.context = context;
		mCr = context.getContentResolver();
		this.media = media;
	}
	
	/**
	 * 如果没有找到分组，则创建分组
	 * @param groupName such as System Group:My Contacts
	 * @return
	 */
	private long findGroupIdForGroupName(String groupName) {
		long id = -1;
		/**
		 * get all groups undeleted in Group table
		 * @return a Cursor, may be null
		 */
		if (DebugUtils.DEBUG_ADD_CONTACT) Log.v(TAG, "findGroupIdForGroupName:groupName " + groupName);
		Cursor groups =  mCr.query(GROUP_URI, GROUP_PROJECTION, GROUP_DELETED_WHERE, null, null);
		if (groups != null && groups.getCount() !=0) {
			String groupTitle = null;
			while(groups.moveToNext()) {
				groupTitle = groups.getString(1);
				if (groupTitle.equalsIgnoreCase(groupName)) {
					if (DebugUtils.DEBUG_ADD_CONTACT) Log.v(TAG, "find group title: " + groupTitle);
					id =  groups.getLong(0);
					groups.close();
					groups = null;
					return id;
				}
			}
		}
		if (id == -1) {
			Log.v(TAG, "findGroupIdForGroupName no find group " + groupName + " in contact.db");
			ContentValues values = new ContentValues(2);
			values.put(ContactsContract.Groups.TITLE, groupName);
			Uri uri = mCr.insert(GROUP_URI, values);
			id = ContentUris.parseId(uri);
			Log.v(TAG, "findGroupIdForGroupName create new group " + groupName + " in contact.db");
		}
		return id;
	}
	
	
	public final Uri createContactEntry(AddressBookParsedResult addressResult) {
		if (DebugUtils.DEBUG_ADD_CONTACT) Log.v(TAG, "createContactEntry");
		String[] addressResultValues = null;
		//XXX 未来可能要判断是否存在同名同号联系人，存在的话可能需要特殊处理
		try {
			Account[] accounts = AccountManager.get(context).getAccounts();
			String[] account;
			if (accounts == null || accounts.length == 0) {
				account = new String[2];
			} else {
				account = new String[] {accounts[0].name, accounts[0].type};
			}
			ArrayList<ContentProviderOperation> ops = new ArrayList<ContentProviderOperation>();
     		
			ops.add(ContentProviderOperation.newInsert(ContactsContract.RawContacts.CONTENT_URI)
					.withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, account[1])
					.withValue(ContactsContract.RawContacts.ACCOUNT_NAME,account[0])
					.build());

			addressResultValues = addressResult.getNames();
			if (addressResultValues != null && addressResultValues[0] != null) {
				ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
						.withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
						.withValue(ContactsContract.Data.MIMETYPE,
								ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
						.withValue(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, addressResultValues[0])
						.build());
			}
			
			// 分组联系人
			String category = addressResult.getCategory();
			if (TextUtils.isEmpty(category)) {
				category = "System Group: My Contacts";
			}
			long myContactsGroupId = findGroupIdForGroupName(category);

			if (myContactsGroupId != -1) {
				ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
						.withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
						.withValue(ContactsContract.Data.MIMETYPE,
								ContactsContract.CommonDataKinds.GroupMembership.CONTENT_ITEM_TYPE)
						.withValue(ContactsContract.CommonDataKinds.GroupMembership.GROUP_ROW_ID, myContactsGroupId)
						.build());
			}
			
			addressResultValues = addressResult.getPhoneNumbers();
			if (addressResultValues != null) {
				boolean isMobile = false;
				for(String value:addressResultValues) {
					//if length of phone number is more than 11, we treat it as mobile type.
					isMobile = value.length()>=11 && value.startsWith("1");
					ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
							.withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
							.withValue(ContactsContract.Data.MIMETYPE,
									ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
							.withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, value)
							.withValue(ContactsContract.CommonDataKinds.Phone.TYPE,
									isMobile?ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE:ContactsContract.CommonDataKinds.Phone.TYPE_WORK)
							.build());
				}
			}
			
			addressResultValues = addressResult.getEmails();
			if (addressResultValues != null) {
				for(String value:addressResultValues) {
					ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
							.withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
							.withValue(ContactsContract.Data.MIMETYPE,
									ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE)
							.withValue(ContactsContract.CommonDataKinds.Email.DATA, value)
							.withValue(ContactsContract.CommonDataKinds.Email.TYPE,
									ContactsContract.CommonDataKinds.Email.TYPE_WORK)
							.build());
				}
			}
			
			addressResultValues = addressResult.getAddresses();
			if (addressResultValues != null) {
				for(String value:addressResultValues) {
					ops.add(ContentProviderOperation
							.newInsert(ContactsContract.Data.CONTENT_URI)
							.withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
							.withValue(ContactsContract.Data.MIMETYPE,
									ContactsContract.CommonDataKinds.StructuredPostal.CONTENT_ITEM_TYPE)
							.withValue(ContactsContract.CommonDataKinds.StructuredPostal.TYPE ,
									ContactsContract.CommonDataKinds.StructuredPostal.TYPE_WORK)
							.withValue(ContactsContract.CommonDataKinds.StructuredPostal.DATA, value)
						    .build());
				}
			}
			
			addressResultValues = addressResult.getURL();
			if (addressResultValues != null) {
				for(String value:addressResultValues) {
//					String[] cloudStruct = Contents.MingDang.getCloudUri(value);
//					boolean isCloudUri = cloudStruct != null;
//					String url = value;
//					if (isCloudUri) {
//						DebugLogger.logD(TAG, "find url " + url);
//						url = cloudStruct[1];
//						DebugLogger.logD(TAG, "convert to url " + url);
//					}
					boolean isCloudUri = Contents.MingDang.isCloudUri(value) != null;
					ops.add(ContentProviderOperation
							.newInsert(ContactsContract.Data.CONTENT_URI)
							.withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
							.withValue(ContactsContract.Data.MIMETYPE,
									ContactsContract.CommonDataKinds.Website.CONTENT_ITEM_TYPE)
							.withValue(ContactsContract.CommonDataKinds.Website.TYPE ,
									isCloudUri ? ContactsContract.CommonDataKinds.Website.TYPE_PROFILE:
										ContactsContract.CommonDataKinds.Website.TYPE_WORK)
							.withValue(ContactsContract.CommonDataKinds.Website.DATA, value)
							.withValue(ContactsContract.CommonDataKinds.Website.LABEL, "hello")
						    .build());
				}
			}
			
			String note = addressResult.getNote();
			if (note != null)
				ops.add(ContentProviderOperation
					.newInsert(ContactsContract.Data.CONTENT_URI)
					.withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
					.withValue(ContactsContract.Data.MIMETYPE,
							ContactsContract.CommonDataKinds.Note.CONTENT_ITEM_TYPE)
					.withValue(ContactsContract.CommonDataKinds.Note.NOTE,note)
				    .build());

			ops.add(ContentProviderOperation
				.newInsert(ContactsContract.Data.CONTENT_URI)
				.withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
				.withValue(ContactsContract.Data.MIMETYPE,
						ContactsContract.CommonDataKinds.Organization.CONTENT_ITEM_TYPE)
				.withValue(ContactsContract.CommonDataKinds.Organization.TYPE ,
							ContactsContract.CommonDataKinds.Organization.TYPE_WORK)
				.withValue(ContactsContract.CommonDataKinds.Organization.COMPANY, addressResult.getOrg())
				.withValue(ContactsContract.CommonDataKinds.Organization.TITLE, addressResult.getTitle())
				.build());
			
			byte[] photo = addressResult.getPhoto();
			if(photo!=null) {
				ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
						.withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
						.withValue(ContactsContract.Data.MIMETYPE,
							    ContactsContract.CommonDataKinds.Photo.CONTENT_ITEM_TYPE)
						.withValue(ContactsContract.CommonDataKinds.Photo.PHOTO,photo)
						.build());
			}
			
    		ContentProviderResult[] rs = context.getContentResolver().applyBatch(ContactsContract.AUTHORITY, ops);
    		//显示当前联系人信息
    		if (rs[0].uri != null) {
    			if (DebugUtils.DEBUG_ADD_CONTACT) Log.v(TAG, "rs[0].uri = " + rs[0].uri);
    			MyApplication.getInstance().showMessageAsync(R.string.result_create_contact);
    			long sid = ContentUris.parseId(rs[0].uri);
        		Cursor cursor = context.getContentResolver().query(ContactsContract.RawContacts.CONTENT_URI, 
        				new String[]{ContactsContract.RawContacts.CONTACT_ID},
        				ContactsContract.RawContacts._ID +'='+ sid, null, null);
        		if (cursor != null && cursor.moveToFirst()) {
        			long id = cursor.getLong(cursor.getColumnIndex(ContactsContract.RawContacts.CONTACT_ID));
        			if (DebugUtils.DEBUG_ADD_CONTACT) {
        				Log.v(TAG, "new RawContacts with id " + id);
        				Log.v(TAG, "new RawContacts with uri " + Uri.parse(ContactsContract.Contacts.CONTENT_URI + "/" +id));
        			}
        			return Uri.parse(ContactsContract.Contacts.CONTENT_URI + "/" +id);
        		}
    		} else {
    			Log.e(TAG,"createContactEntry failed ");
    			MyApplication.getInstance().showMessageAsync(R.string.result_create_contact_failed);
    		}
    		
		} catch (Exception e) {
			// Log exception
			e.printStackTrace();
			Log.e(TAG,"Exceptoin encoutered while inserting contact: "+ e);
		}
		return null;
 
    }
	
	/**
	 * 我们查询的是phone_lookup表，该表主要是raw_contact_id字段
	 * @param context
	 * @param phoneNumber
	 * @return
	 */
	 public static final Cursor queryContactEntry(Context context, String phoneNumber) {
		 Uri lookupUri = Uri.withAppendedPath(PhoneLookup.CONTENT_FILTER_URI, Uri.encode(phoneNumber));
		 return context.getContentResolver().query(lookupUri, new String[]{PhoneLookup.DISPLAY_NAME, PhoneLookup.NUMBER}, null, null, null);
	 }
}
