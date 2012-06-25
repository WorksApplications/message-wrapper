/* 
 * Copyright (C) 2012 Works Applications Co., Ltd. 
 *            http://www.worksap.co.jp/
 * 
 * Licensed under the MIT License:
 *      http://www.opensource.org/licenses/mit-license.php
 * 
 */

package jp.co.worksap.message.util;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

public class CharsetUtility {
	
	private static final Map<String, String> CHARSET_MAP = new HashMap<String, String>() {
		private static final long serialVersionUID = 3242501372820040804L;
	{
		put("iso-2022-jp", "X-WINDOWS-ISO2022JP");
		put("iso2022jp", "X-WINDOWS-ISO2022JP");
		put("shift_jis","MS932");
		put("cp932", "MS932");
		put("gb2312", "GB18030");
		put("iso-8859-1", "Windows-1252");
	}};

	public static String getMappingCharSet(String text) {
		String source = getValidCharset(text);
		if (source.isEmpty()) {
			return "";
		}
		if (CHARSET_MAP.containsKey(source)) {
			return CHARSET_MAP.get(source);
		} else {
			return source;
		}
	}
	
	public static String getValidCharset(String text) {
		for (String charSet : Encoding.VALID_CHARSETS) {
			if (text.contains(charSet)) {
				return charSet;
			}
		}
		return "";
	}
	
	public static boolean needsMapping(String text) {
		for (Entry<String, String> entry : CHARSET_MAP.entrySet()) {
			if (text.contains(entry.getKey())) {
				return true;
			}
		}
		return false;
	}

	public static Map<String, String> getCharsetMap() {
		return CHARSET_MAP;
	}
}
