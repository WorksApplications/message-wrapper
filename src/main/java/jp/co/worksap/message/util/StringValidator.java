/* 
 * Copyright (C) 2012 Works Applications Co., Ltd. 
 *            http://www.worksap.co.jp/
 * 
 * Licensed under the MIT License:
 *      http://www.opensource.org/licenses/mit-license.php
 * 
 */

package jp.co.worksap.message.util;

public class StringValidator {
	public static boolean isValid(String str) {
		for (int i = 0; i < str.length(); i++) {
			char c = str.charAt(i);

			if (!isPrintableCharacter(c)) {
				return false;
			}
		}

		return true;
	}

	private static boolean isPrintableCharacter(char c) {
		// check control char
		// NULL ... US
		// expect LF
		if (c <= 31 && c != 10) {
			return false;
		}

		// check control char
		// DEL ... NBSP
		if ((c >= 127) && (c <= 160)) {
			return false;
		}

		// check control char
		// SHY
		if (c == 173) {
			return false;
		}

		return true;
	}
}
