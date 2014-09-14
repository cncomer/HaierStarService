package com.shwy.bestjoy.contacts;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.provider.Contacts;
import android.provider.Contacts.Groups;
import android.provider.Contacts.Organizations;
import android.provider.Contacts.People;
import android.provider.Contacts.Phones;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.bestjoy.app.haierstartservice.R;
import com.bestjoy.app.haierstartservice.ui.PreferencesActivity;
import com.bestjoy.app.utils.BeepAndVibrate;
import com.google.zxing.client.result.AddressBookParsedResult;

/**
 * ��ȷ�����ͨѶ¼�˻���ʾѡ���System Group:My Contacts��
 * @author yeluosuifeng2005@gmail.com (�¿�)
 *
 */
public class AddrBookAddManagerEarly {

	private static final String TAG ="AddrBookAddManagerEarly";
	private Context mContext;
	private BeepAndVibrate media;
	public AddrBookAddManagerEarly(Context context, BeepAndVibrate media) {
		this.mContext = context;
		this.media = media;
	}
	
	/**
     * Takes the entered data and saves it to a new contact.
     * @hide
     */
    @SuppressWarnings("deprecation")
	public void createContactEntry(String[] names, String[] phoneNumbers,
			String[] emails, String note, String address, String org,
			String title, byte[] photo) {
        ContentValues values = new ContentValues();
        int numValues = 0;
        ContentResolver mResolver = mContext.getContentResolver();

        // Create the contact itself
        final String name = names[0];
        if (name != null && TextUtils.isGraphic(name)) {
            numValues++;
        }
        values.put(People.NAME, name);

        // Add the contact to the My Contacts group
        Uri contactUri = People.createPersonInMyContactsGroup(mResolver, values);
      
            // Check to see if we're not syncing everything and if so if My Contacts is synced.
            // If it isn't then the created contact can end up not in any groups that are
            // currently synced and end up getting removed from the phone, which is really bad.
            boolean syncingEverything = !"0".equals(Contacts.Settings.getSetting(mResolver, null,
                    Contacts.Settings.SYNC_EVERYTHING));
            if (!syncingEverything) {
                boolean syncingMyContacts = false;
                Cursor c = mResolver.query(Groups.CONTENT_URI, new String[] { Groups.SHOULD_SYNC },
                        Groups.SYSTEM_ID + "=?", new String[] { Groups.GROUP_MY_CONTACTS }, null);
                if (c != null) {
                    try {
                        if (c.moveToFirst()) {
                            syncingMyContacts = !"0".equals(c.getString(0));
                        }
                    } finally {
                        c.close();
                    }
                }

                if (!syncingMyContacts) {
                    // Not syncing My Contacts, so find a group that is being synced and stick
                    // the contact in there. We sort the list so at least all contacts
                    // will appear in the same group.
                    c = mResolver.query(Groups.CONTENT_URI, new String[] { Groups._ID },
                            Groups.SHOULD_SYNC + "!=0", null, Groups.DEFAULT_SORT_ORDER);
                    if (c != null) {
                        try {
                            if (c.moveToFirst()) {
                                People.addToGroup(mResolver, ContentUris.parseId(contactUri),c.getLong(0));
                            }
                        } finally {
                            c.close();
                        }
                    }
                }
            }


        // Handle the photo
        if (photo != null) {
            Contacts.People.setPhotoData(mResolver, contactUri, photo );
        }

        // Create the contact methods
        values.clear();
        long sid = ContentUris.parseId(contactUri);
        values.put(Phones.PERSON_ID, sid);
        values.put(Phones.NUMBER, phoneNumbers[0]);
        values.put(Phones.TYPE, Phones.TYPE_MOBILE);
        mResolver.insert(Contacts.Phones.CONTENT_URI, values);
        
        values.clear();
        values.put(Organizations.PERSON_ID, sid);
        values.put(Organizations.TYPE, Organizations.TYPE_WORK);
        values.put(Organizations.COMPANY, org);
        values.put(Organizations.TITLE, title);
        mResolver.insert(Organizations.CONTENT_URI, values);
        
        values.clear();
        if(emails!=null) {
        	values.put(Contacts.ContactMethods.PERSON_ID, sid);
            values.put(Contacts.ContactMethods.KIND, Contacts.KIND_EMAIL);
            values.put(Contacts.ContactMethods.TYPE, Contacts.ContactMethods.TYPE_WORK);
            values.put(Contacts.ContactMethods.DATA, emails[0]);
            mResolver.insert(Contacts.ContactMethods.CONTENT_URI, values);
        }
        
        values.clear();
        values.put(Contacts.ContactMethods.PERSON_ID, sid);
        values.put(Contacts.ContactMethods.KIND, Contacts.KIND_POSTAL);
        values.put(Contacts.ContactMethods.TYPE, Contacts.ContactMethods.TYPE_WORK);
        values.put(Contacts.ContactMethods.DATA, address);
        mResolver.insert(Contacts.ContactMethods.CONTENT_URI, values);
        
        // Update the contact with any straggling data, like notes
        values.clear();
        if (note != null && TextUtils.isGraphic(note)) {
                    values.put(People.NOTES, note);
                    mResolver.update(contactUri, values, null, null);
        }

        if (numValues == 0) {
            mResolver.delete(contactUri, null, null);
        } else {
        	
        	SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        	if(prefs.getBoolean(PreferencesActivity.KEY_AUTO_REDIRECT, true)) {
        		launchAddedContact(contactUri);
        	}else {
        		Toast.makeText(mContext, R.string.result_create_contact, Toast.LENGTH_LONG).show();
        	}
        	if(media!=null)media.playBeepSoundAndVibrate();
        }
    }
    
    /**
     * Takes the entered data and saves it to a new contact.
     */
    @SuppressWarnings("deprecation")
	public Uri createContactEntry(AddressBookParsedResult addressResult) {
        ContentValues values = new ContentValues();
        ContentResolver mResolver = mContext.getContentResolver();

        // Create the contact itself
        String[] addressResultValues = addressResult.getNames();
        if (addressResultValues != null && addressResultValues[0] != null) {
        	values.put(People.NAME, addressResultValues[0]);
        } else {
        	values.put(People.NAME, "");
        }
        

        // Add the contact to the My Contacts group
        Uri contactUri = People.createPersonInMyContactsGroup(mResolver, values);
        if (contactUri == null) {
        	Log.v(TAG, "createContactEntry createdPersonInMyContactsGroup " + contactUri);
        	return null;
        }
      
            // Check to see if we're not syncing everything and if so if My Contacts is synced.
            // If it isn't then the created contact can end up not in any groups that are
            // currently synced and end up getting removed from the phone, which is really bad.
            boolean syncingEverything = !"0".equals(Contacts.Settings.getSetting(mResolver, null,
                    Contacts.Settings.SYNC_EVERYTHING));
            if (!syncingEverything) {
                boolean syncingMyContacts = false;
                Cursor c = mResolver.query(Groups.CONTENT_URI, new String[] { Groups.SHOULD_SYNC },
                        Groups.SYSTEM_ID + "=?", new String[] { Groups.GROUP_MY_CONTACTS }, null);
                if (c != null) {
                    try {
                        if (c.moveToFirst()) {
                            syncingMyContacts = !"0".equals(c.getString(0));
                        }
                    } finally {
                        c.close();
                    }
                }

                if (!syncingMyContacts) {
                    // Not syncing My Contacts, so find a group that is being synced and stick
                    // the contact in there. We sort the list so at least all contacts
                    // will appear in the same group.
                    c = mResolver.query(Groups.CONTENT_URI, new String[] { Groups._ID },
                            Groups.SHOULD_SYNC + "!=0", null, Groups.DEFAULT_SORT_ORDER);
                    if (c != null) {
                        try {
                            if (c.moveToFirst()) {
                                People.addToGroup(mResolver, ContentUris.parseId(contactUri),c.getLong(0));
                            }
                        } finally {
                            c.close();
                        }
                    }
                }
            }


        // Handle the photo
        byte[] photo = addressResult.getPhoto();
        if (photo != null) {
            Contacts.People.setPhotoData(mResolver, contactUri, photo );
        }

        long sid = ContentUris.parseId(contactUri);
        
        // Create the contact methods
        addressResultValues = addressResult.getPhoneNumbers();
        if (addressResultValues != null) {
        	for(String number:addressResultValues) {
        		values.clear();
                values.put(Phones.PERSON_ID, sid);
                values.put(Phones.NUMBER, number);
                values.put(Phones.TYPE, Phones.TYPE_MOBILE);
                mResolver.insert(Contacts.Phones.CONTENT_URI, values);
        	}
        }
        
        //add org and title
        values.clear();
        values.put(Organizations.PERSON_ID, sid);
        values.put(Organizations.TYPE, Organizations.TYPE_WORK);
        values.put(Organizations.COMPANY, addressResult.getOrg());
        values.put(Organizations.TITLE, addressResult.getTitle());
        mResolver.insert(Organizations.CONTENT_URI, values);
        
        //add emails
        addressResultValues = addressResult.getEmails();
        if (addressResultValues != null) {
        	for (String email:addressResultValues) {
        		values.clear();
    	        if(email!=null) {
    	        	values.put(Contacts.ContactMethods.PERSON_ID, sid);
    	            values.put(Contacts.ContactMethods.KIND, Contacts.KIND_EMAIL);
    	            values.put(Contacts.ContactMethods.TYPE, Contacts.ContactMethods.TYPE_WORK);
    	            values.put(Contacts.ContactMethods.DATA, email);
    	            mResolver.insert(Contacts.ContactMethods.CONTENT_URI, values);
    	        }
        	}
        }
        
        
        values.clear();
        values.put(Contacts.ContactMethods.PERSON_ID, sid);
        values.put(Contacts.ContactMethods.KIND, Contacts.KIND_POSTAL);
        values.put(Contacts.ContactMethods.TYPE, Contacts.ContactMethods.TYPE_WORK);
        values.put(Contacts.ContactMethods.DATA, addressResult.getAddresses()[0]);
        mResolver.insert(Contacts.ContactMethods.CONTENT_URI, values);

        // Update the contact with any straggling data, like notes
        values.clear();
        String note = addressResult.getNote();
        if (note != null && TextUtils.isGraphic(note)) {
                    values.put(People.NOTES, note);
                    mResolver.update(contactUri, values, null, null);
        }
        return contactUri;
    }
    
    public void launchAddedContact(Uri contactUri) {
		Intent intent = new Intent();
		intent.setAction(Intent.ACTION_VIEW);
		//��ʾ��ǰ��ϵ����Ϣ
		intent.setData(contactUri);
		mContext.startActivity(intent);
    }
    
    public static final Cursor queryContactEntry(Context context, String phoneNumber) {
    	String selection = Phones.NUMBER + "=" + Uri.encode(phoneNumber);
		 return context.getContentResolver().query(Contacts.CONTENT_URI, null, selection, null, null);
	 }
}
