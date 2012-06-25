package jp.co.worksap.message.util;

import java.util.Map.Entry;

import javax.mail.internet.MimePart;

public class CharsetMap {
	public static String cleanContentType(MimePart mp, String contentType) {
		String result = contentType;
		for (Entry<String, String> entry : CharsetUtility.getCharsetMap().entrySet()) {
			String template1 = "charset=" + entry.getKey();
			String template2 = "charset=\"" + entry.getKey();
			if (result.toLowerCase().contains(template1)) {
				result = result.replaceAll("(?i)" + template1, "charset=" + entry.getValue());
			} else if (result.toLowerCase().contains(template2)) {
				result = result.replaceAll("(?i)" + template2, "charset=\"" + entry.getValue());
			}
		}
		return result;
	}
}
