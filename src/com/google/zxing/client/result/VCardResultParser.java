/*
 * Copyright 2008 ZXing authors
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

import java.util.ArrayList;
import java.util.List;

import com.google.zxing.QuotedPrintableDecoder;
import com.google.zxing.Result;
import com.shwy.bestjoy.utils.Base64;
import com.shwy.bestjoy.utils.DebugUtils;

/**
 * Parses contact information formatted according to the VCard (2.1) format.
 * This is not a complete implementation but should parse information as
 * commonly encoded in 2D barcodes.
 * 
 * @author Sean Owen
 * @author yeluosuifeng2005@gmail.com
 */
public final class VCardResultParser extends ResultParser {

	private final static String TAG = "VCardResultParser";

	private enum CodeFormat {
		QUOTED_PRINTABLE, BASE64, NONE
	}

	private static CodeFormat codeFormat = CodeFormat.NONE;

	public AddressBookParsedResult parse(Result result) {
		// Although we should insist on the raw text ending with "END:VCARD",
		// there's no reason
		// to throw out everything else we parsed just because this was omitted.
		// In fact, Eclair
		// is doing just that, and we can't parse its contacts without this
		// leniency.
		String rawText = result.getText();
		if (rawText == null || !rawText.startsWith("BEGIN:VCARD")) {
			return null;
		}
		rawText = rawText.replace("\r\n", "\n");
		String[] names = matchVCardPrefixedField("FN", rawText, true);
		if (names == null) {
			// If no display names found, look for regular name fields and
			// format them
			names = matchVCardPrefixedField("N", rawText, true);
			formatNames(names);
		}
		DebugUtils.logVcardParse(TAG, "finish names");
		String[] phoneNumbers = matchVCardPrefixedField("TEL", rawText, true);
		String bid = matchSingleVCardPrefixedField("X-BM", rawText, true);

		String category = matchSingleVCardPrefixedField("CATEGORIES", rawText,true);
		
		String[] emails = matchVCardPrefixedField("EMAIL", rawText, true);
		String note = matchSingleVCardPrefixedField("NOTE", rawText, false);
		
		String[] addresses = matchVCardPrefixedField("ADR", rawText, true);
		if (addresses != null) {
			for (int i = 0; i < addresses.length; i++) {
				addresses[i] = formatAddress(addresses[i]);
			}
		}

		String org = matchSingleVCardPrefixedField("ORG", rawText, true);
		String birthday = matchSingleVCardPrefixedField("BDAY", rawText, true);
		if (!isLikeVCardDate(birthday)) {
			birthday = null;
		}
		String title = matchSingleVCardPrefixedField("TITLE", rawText, true);
		DebugUtils.logVcardParse(TAG, "finish title");
		String[] urls = matchVCardPrefixedField("URL", rawText, true);
		String photo = matchSingleVCardPrefixedField("PHOTO", rawText,true);
		byte[] rawPhoto = null;
		if(photo!=null) {
			try {
				rawPhoto = Base64.decode(photo, Base64.DEFAULT);
				DebugUtils.logD(TAG, "photo!=null");
				DebugUtils.logD(TAG, "rawPhoto.length:"+rawPhoto.length);
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
			}
		} else {
			DebugUtils.logE(TAG, "photo==null");
		}
		String tag = matchSingleVCardPrefixedField("TAG", rawText,true);
		return new AddressBookParsedResult(names, null, phoneNumbers, emails,
				note, addresses, org, birthday, title, urls, rawPhoto, bid, category, tag);
	}

	 static String[] matchVCardPrefixedField(String prefix, String rawText, boolean trim) {
		List<String> matches = null;
		int i = 0;
		codeFormat = CodeFormat.NONE;

		int max = rawText.length();
		while (i < max) {
			i = rawText.indexOf(prefix, i);
			if (i < 0) {
				break;
			}
			if (i > 0 && rawText.charAt(i - 1) != '\n') {
				// then this didn't start a new token, we matched in the middle
				// of something
				i++;
				continue;
			}
			i += prefix.length(); // Skip past this prefix we found to start
			if (rawText.charAt(i) != ':' && rawText.charAt(i) != ';') {
				continue;
			} else if(rawText.charAt(i) == ';') {
				i++;
				int bound = rawText.indexOf(":", i);
				String foobar = rawText.substring(i, bound);//
				int isHasCodeFormat = foobar.indexOf("ENCODING=");
				if(isHasCodeFormat>=0) {
					int codeFormatStart = isHasCodeFormat + 9; // codeString start
					int codeFormatEnd = foobar.indexOf(';', codeFormatStart);
					if(codeFormatEnd<0)codeFormatEnd=foobar.length();
					DebugUtils.logVcardParse(TAG, codeFormatStart+"-" +codeFormatEnd);
					DebugUtils.logVcardParse(TAG, foobar.length()+"");
					String match = foobar.substring(codeFormatStart, codeFormatEnd);
					if(match.equals("BASE64") || match.equals("B"))codeFormat = CodeFormat.BASE64;
					else if(match.equals("QUOTED-PRINTABLE"))codeFormat = CodeFormat.QUOTED_PRINTABLE;
					else codeFormat = CodeFormat.NONE;
					DebugUtils.logVcardParse(TAG, "CodeFormat:" + codeFormat);
				}
				i = bound;
				
			} 
				
			i++; // skip colon

			int start = i; // Found the start of a match here
			if (matches == null) {
				matches = new ArrayList<String>(3); // lazy init
			}
			switch(codeFormat) {
			case QUOTED_PRINTABLE:
				i = rawText.indexOf("\n", i); 
				if (i<0) {
					i = rawText.length();
				} else i++;
//				char ch = rawText.charAt(i-1);
//				while(ch =='=') {
//					i = rawText.indexOf("\r\n", i+2); 
//					ch = rawText.charAt(i-1);
//				}
				String qp = rawText.substring(start, i);
				DebugUtils.logVcardParse(TAG, "parsing QUOTED_PRINTABLE");
				qp = QuotedPrintableDecoder.DecodeQuoted(qp);
				DebugUtils.logVcardParse(TAG, qp);
				if (trim) {
					qp = qp.trim();
				}
				matches.add(qp);
				break;
			case BASE64:
				i = rawText.indexOf("\n", i); 
				if (i<0) {
					i = rawText.length();
				} else i++;
				DebugUtils.logVcardParse(TAG, "pos:"+i);
				char c = rawText.charAt(i);
				DebugUtils.logVcardParse(TAG, "charAt:"+c+"crlf");
				while(c !='\n') {//&& i!=-1
					DebugUtils.logVcardParse(TAG, "pos:"+i);
					i = rawText.indexOf("\n", i); 
					if (i == -1) {
						return null;
					}
					i++;
					c = rawText.charAt(i);
					DebugUtils.logVcardParse(TAG, "charAt:"+c+"crlf");
				}
				String bs64 = rawText.substring(start, i).replaceAll(" ", "");
				if (trim) {
					bs64 = bs64.trim();
				}
				DebugUtils.logVcardParse(TAG, "base64 CharSequence");
				DebugUtils.logVcardParse(TAG, bs64);
				matches.add(bs64);
				break;
			case NONE:
				i = rawText.indexOf("\r\n", i); // Really, ends in \r\n
				if(i<0)i = rawText.indexOf("\n", start);
				if(i<0) {
					DebugUtils.logE(TAG, "no find endchar of the prefix" + prefix + ", so we just use rawText.length() as endPosition.");
					i = rawText.length();
				}
				// found a match
				DebugUtils.logVcardParse(TAG, prefix);
				DebugUtils.logVcardParse(TAG, "start = " + start + "i=" + i);
				String none = rawText.substring(start, i);
				DebugUtils.logVcardParse(TAG, none);
				if (trim) {
					none = none.trim();
				}
				matches.add(none);
			}
			i++;
		
		}
		if (matches == null || matches.isEmpty()) {
			return null;
		}
		return matches.toArray(new String[matches.size()]);
	}

	static String matchSingleVCardPrefixedField(String prefix, String rawText,
			boolean trim) {
		String[] values = matchVCardPrefixedField(prefix, rawText, trim);
		return values == null ? null : values[0];
	}

	static String matchSingleVCardPrefixedFieldNoNull(String prefix, String rawText, boolean trim) {
		String[] values = matchVCardPrefixedField(prefix, rawText, trim);
		return values == null ? "" : values[0];
	}

	private static boolean isLikeVCardDate(String value) {
		if (value == null) {
			return true;
		}
		// Not really sure this is true but matches practice
		// Mach YYYYMMDD
		if (isStringOfDigits(value, 8)) {
			return true;
		}
		// or YYYY-MM-DD
		return value.length() == 10 && value.charAt(4) == '-'
				&& value.charAt(7) == '-' && isSubstringOfDigits(value, 0, 4)
				&& isSubstringOfDigits(value, 5, 2)
				&& isSubstringOfDigits(value, 8, 2);
	}

	private static String formatAddress(String address) {
		if (address == null) {
			return null;
		}
		int length = address.length();
		StringBuffer newAddress = new StringBuffer(length);
		for (int j = 0; j < length; j++) {
			char c = address.charAt(j);
			if (c == ';') {
				newAddress.append(' ');
			} else {
				newAddress.append(c);
			}
		}
		return newAddress.toString().trim();
	}

	/**
	 * Formats name fields of the form "Public;John;Q.;Reverend;III" into a form
	 * like "Reverend John Q. Public III".
	 * 
	 * @param names
	 *            name values to format, in place
	 */
	private static void formatNames(String[] names) {
		if (names != null) {
			for (int i = 0; i < names.length; i++) {
				String name = names[i];
				String[] components = new String[5];
				int start = 0;
				int end;
				int componentIndex = 0;
				while ((end = name.indexOf(';', start)) > 0) {
					components[componentIndex] = name.substring(start, end);
					componentIndex++;
					start = end + 1;
				}
				components[componentIndex] = name.substring(start);
				StringBuffer newName = new StringBuffer(100);
				maybeAppendComponent(components, 3, newName);
				maybeAppendComponent(components, 1, newName);
				maybeAppendComponent(components, 2, newName);
				maybeAppendComponent(components, 0, newName);
				maybeAppendComponent(components, 4, newName);
				names[i] = newName.toString().trim();
			}
		}
	}

	private static void maybeAppendComponent(String[] components, int i,
			StringBuffer newName) {
		if (components[i] != null) {
			newName.append(' ');
			newName.append(components[i]);
		}
	}

}
