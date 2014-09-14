/*
 * Copyright (C) 2008 ZXing authors
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

package com.bestjoy.app.haierstartservice.ui;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;

/**
 * The main settings activity.
 * 
 * @author dswitkin@google.com (Daniel Switkin)
 */
public final class PreferencesActivity extends PreferenceActivity {
	private final String TOKEN = PreferencesActivity.class.getName();
	/**程序第一次启动*/
	public static final String KEY_FIRST_STARTUP = "preferences_first_startup";
	
	public static final String KEY_DECODE_1D = "preferences_decode_1D";
	public static final String KEY_DECODE_QR = "preferences_decode_QR";
	public static final String KEY_DECODE_DATA_MATRIX = "preferences_decode_Data_Matrix";

	public static final String KEY_AUTO_REDIRECT = "preferences_auto_redirect";
	static final String KEY_VCF_SAVE = "preferences_vcf_save";
	public static final String KEY_CUSTOM_PRODUCT_SEARCH = "preferences_custom_product_search";

	static final String KEY_PLAY_BEEP = "preferences_play_beep";
	static final String KEY_VIBRATE = "preferences_vibrate";

	public static final String KEY_NOT_OUR_RESULTS_SHOWN = "preferences_not_out_results_shown";

	public static final String KEY_COLOR_INDEX = "preferences_color_index";
	public static final String KEY_FONT_SIZE = "preferences_font_size";

	public static final String KEY_LATEST_VERSION = "preferences_latest_version";
	public static final String KEY_LATEST_VERSION_CODE_NAME = "preferences_latest_version_code_name";
	public static final String KEY_LATEST_VERSION_INSTALL = "preferences_latest_version_install";
	public static final String KEY_LATEST_VERSION_LEVEL = "preferences_latest_version_level";

	private CheckBoxPreference decode1D;
	private CheckBoxPreference decodeQR;
	private CheckBoxPreference decodeDataMatrix;

	private SharedPreferences sp;

	// privacy module
	public static final String KEY_PRIVACY_OUTGOING_CALL = "preferences_privacy_outgoing_call";
	public static final String KEY_PRIVACY_SMS = "preferences_privacy_mms";
	public static final String KEY_PRIVACY_INCOMING_CALL = "preferences_privacy_incoming_call";

	public static final String KEY_PRIVACY_AREA_CODE = "preferences_privacy_area_code";
	public static final String KEY_PRIVACY_PHONE_NUMBER = "preferences_privacy_phone_number";

	public static final String KEY_ACCOUNT_SETTING = "preference_account_setting";
	public static final String KEY_CARD_SETTING = "preference_card_setting";

	public static final String KEY_MOBILE_CONFIRM_IGNORE = "preferences_mobile_confirm_ignore";

	@Override
	protected void onCreate(Bundle icicle) {
		super.onCreate(icicle);
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
	}

	@Override
	public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen,
			Preference preference) {
		return super.onPreferenceTreeClick(preferenceScreen, preference);
	}
}
