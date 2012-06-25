/* 
 * Copyright (C) 2012 Works Applications Co., Ltd. 
 *            http://www.worksap.co.jp/
 * 
 * Licensed under the MIT License:
 *      http://www.opensource.org/licenses/mit-license.php
 * 
 */

package jp.co.worksap.message.util;

public final class Encoding {
	public static final String ISO2022JP = "iso-2022-jp";
	public static final String SHIFT_JIS = "shift_jis";
	public static final String UTF8 = "utf-8";
	public static final String MS932 = "ms932";
	public static final String GB2312 = "gb2312";
	public static final String GB18030 = "gb18030";
	public static final String ISO8859_1 = "iso-8859-1";
	public static final String CP932 = "cp932";

	public static final String[] VALID_CHARSETS = { "iso-2022-jp", "shift_jis",
			"utf-8", "ms932", "gb2312", "gb18030", "iso-8859-1", "cp932" };

	// use instead of ISO-8859-1
	public static final String Windows_1252 = "windows-1252";
	// use instead of SHIFT_JIS
	public static final String WINDOWS_31J = "windows-31j";
	// use instead of ISO-2022-JP
	public static final String X_WINDOWS_ISO2022JP = "x-windows-iso2022jp";
}
