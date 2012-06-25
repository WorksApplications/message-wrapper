/* 
 * Copyright (C) 2012 Works Applications Co., Ltd. 
 *            http://www.worksap.co.jp/
 * 
 * Licensed under the MIT License:
 *      http://www.opensource.org/licenses/mit-license.php
 * 
 */

package jp.co.worksap.message.decoder;

import java.io.UnsupportedEncodingException;

import jp.co.worksap.message.util.Encoding;
import jp.co.worksap.message.util.CharsetUtility;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.net.QuotedPrintableCodec;
import org.apache.commons.codec.net.URLCodec;

public final class HeaderDecoder {
	private static final String SOFT_BREAK = "\\r\\n\\s*";
	private static final String STR_WITH_SPACE = "\\?=\\s=\\?";
	private static final String STR_WITHOUT_SPACE = "\\?==\\?";

	public String decodeAddress(String encodedAddress) {
		String parsedAddress = parse(encodedAddress);
		return decode(parsedAddress);
	}

	public String decodeSubject(String edcodedSubject) {
		String parsedSubject = parse(edcodedSubject);
		return decode(parsedSubject);
	}

	public String decodeFileName(String mimeHeader) {
		if (mimeHeader.toLowerCase().contains("filename")) {
			return getFileNameFromFileNameParameter(mimeHeader);
		} else if (mimeHeader.toLowerCase().contains("name")) {
			// "name" parameter is already removed, but it's used
			return getFileNameFromNameParameter(mimeHeader);
		}
		return "";
	}

	private String parse(String text) {
		// get the transfer-encoding.
		String transferEncoding = getTransferEncoding(text);
		// trim the text and remove the soft break.
		String textWithoutSoftBreak = replace(text.trim(), SOFT_BREAK, "");

		// remove spaces within the text.
		String textWithoutSpace = replace(textWithoutSoftBreak, STR_WITH_SPACE,
				STR_WITHOUT_SPACE);
		
		// decode the text which has Base64(b) encoding.
		if (transferEncoding == "b") {
			return textWithoutSpace;
		}

		// decode the text which has QuotedPrintable(q) encoding.
		String regex = createEncodingRegex();
		return replace(textWithoutSpace, regex, "");
	}

	private String decode(String text) {
		if (!text.contains("=?")) {
			return text;
		}
			
		String before = getBeforeFirstEncodedPart(text);
		String encoded = getFirstEncodedPart(text);
		String after = getAfterFirstEncodedPart(text);
		String decoded = "";
		try {
			decoded = decodeHeader(getEncodedText(encoded),
					CharsetUtility.getMappingCharSet(encoded
							.toLowerCase()), getTransferEncoding(encoded));
		} catch (UnsupportedEncodingException e) {
			// can not be decoded, return it.
			return text;
		} catch (DecoderException e) {
			// can not be decoded, return it.
			return text;
		}

		if (!CharsetUtility.getValidCharset(after.toLowerCase())
				.isEmpty()) {
			after = decode(after);
		}

		return before + decoded + after;
	}

	private String getFileNameFromFileNameParameter(String mimeHeader) {
		String disposition = getContentDisposition(mimeHeader);

		if (disposition.contains("filename")) {
			if (disposition.toLowerCase().contains("=?")) {
				// it's wrong impl in RFC2231, but it's used
				String encoded = getFileNameByBase64(disposition);
				return encoded;
			}
			return getFileNameByRfc2231(disposition);
		}

		// getContentDisposition() may mistake to get Content-Disposition value
		return "";
	}

	/**
	 * "name" parameter is removed in RFC. but it is used.
	 * 
	 * @param mimeHeader
	 * @return
	 */
	private String getFileNameFromNameParameter(String mimeHeader) {
		if (mimeHeader.toLowerCase().contains("name")) {
			String[] splitted = mimeHeader.split("\\r\\n");
			for (String s : splitted) {
				if (s.contains("name=")) {
					int begin = s.indexOf("name=");
					begin += "name=".length();
					String substring = s.substring(begin);
					return trimDoubleQuotations(substring);
				}
			}
		}

		return "";
	}

	private String getContentDisposition(String mimeHeader) {
		// search "content-disposition" header. header must start no white space
		String disposition = "\r\ncontent-disposition";
		int begin = mimeHeader.toLowerCase().indexOf(disposition);
		if (begin < 0) {
			return "";
		}
		begin += 2; // remove \r\n

		int end = mimeHeader.toLowerCase().indexOf("\r\ncontent",
				begin + disposition.length());
		if (end < 0) {
			return mimeHeader.substring(begin);
		}

		return mimeHeader.substring(begin, end);
	}

	private String getFileNameByBase64(String contentDisposition) {
		String target = "filename=";
		int begin = contentDisposition.indexOf(target);
		if (begin < 0) {
			return "";
		}
		begin += target.length();

		String substring = null;
		int end = contentDisposition.indexOf(";", begin);
		if (end < 0) {
			substring = contentDisposition.substring(begin);
		} else {
			substring = contentDisposition.substring(begin, end);
		}

		String noQuotation = trimDoubleQuotations(substring);
		String noSoftBreak = replace(noQuotation, SOFT_BREAK, "");

		return decode(noSoftBreak);
	}

	private String getFileNameByRfc2231(String contentDisposition) {
		// contentDisposition have multi lines or don't
		if (contentDisposition.contains("*0")) {
			String unitedFileName = getUnitedFileName(contentDisposition);

			// file name is encoded or not
			if (contentDisposition.contains("*=")) {
				return decodeUrlEncodedFileName(unitedFileName);
			} else {
				return unitedFileName;
			}
		} else {
			// file name is encoded or not
			if (contentDisposition.contains("*=")) {
				return decodeUrlEncodedFileName(contentDisposition);
			} else {
				return getUnencodedFileName(contentDisposition);
			}
		}
	}

	private String getUnencodedFileName(String contentDisposition) {
		String trimmed = contentDisposition.trim();

		String target = "filename=";
		int begin = trimmed.indexOf(target);
		if (begin < 0) {
			return "";
		}
		begin += target.length();

		String fileName = trimmed.substring(begin);
		return trimDoubleQuotations(fileName);
	}

	private String getUnitedFileName(String multiLine) {
		// "multiLine" is filename*0=xxx;\r\nfilename*1=yyy;
		String starEqual = "*=";
		String semicolon = ";";

		String connected = "";

		int begin = 0;
		int end = 0;

		while (true) {
			begin = multiLine.indexOf(starEqual, end);
			if (begin < 0) {
				return connected;
			}
			begin += starEqual.length();

			end = multiLine.indexOf(semicolon, begin);
			if (end < 0) {
				return connected + multiLine.substring(begin).trim();
			}

			connected = connected + multiLine.substring(begin, end);
		}
	}

	private String decodeUrlEncodedFileName(String encodedFileName) {
		String trimmed = encodedFileName.trim();
		String encoding = CharsetUtility.getValidCharset(trimmed
				.toLowerCase());

		int indexOfFirstQuotation = trimmed.indexOf("'");
		if (indexOfFirstQuotation < 0) {
			return "";
		}

		int indexOfSecondQuotation = trimmed.indexOf("'",
				indexOfFirstQuotation + 1);
		if (indexOfSecondQuotation < 0) {
			return "";
		}

		String encoded = trimmed.substring(indexOfSecondQuotation + 1);

		try {
			return decodeTextByUrl(encoded,
					CharsetUtility.getMappingCharSet(encoding
							.toLowerCase()));
		} catch (UnsupportedEncodingException e) {
			return "";
		} catch (DecoderException e) {
			return "";
		}
	}

	private String trimDoubleQuotations(String text) {
		String trimmed = text.trim();
		if (trimmed.length() == 0) {
			return "";
		}
		if ((trimmed.charAt(0) == '\"')
				&& (trimmed.charAt(trimmed.length() - 1) == '\"')) {
			return trimmed.substring(1, trimmed.length() - 1);
		}
		return text;
	}

	private String replace(String text, String regex, String replacement) {
		return text.replaceAll(regex, replacement);
	}

	private String createEncodingRegex() {
		StringBuilder builder = new StringBuilder();
		String[] charSets = Encoding.VALID_CHARSETS;
		builder.append("\\?==\\?(");
		for (int i = 0; i < charSets.length; i++) {
			builder.append("(").append(charSets[i]).append(")");
			builder.append("|");
		}
		// delete the last "|"
		builder.deleteCharAt(builder.lastIndexOf("|"));
		builder.append(")\\?.?\\?");
		// finally, this regex may look like
		// "\\?==\\?((utf-8)|(iso-2022-jp)|(shift_jis))\\?.?\\?"
		return builder.toString();
	}

	private String getTransferEncoding(String text) {
		String lowerCaseText = text.toLowerCase();
		if (lowerCaseText.contains("?b?")) {
			return "b";
		} else if (lowerCaseText.contains("?q?")) {
			return "q";
		}
		return "";
	}

	private String getEncodedText(String text) {
		// "text" must have ?B? or ?Q?
		String lowerCaseText = text.toLowerCase();
		int begin = lowerCaseText.indexOf("?b?");
		if (begin < 0) {
			begin = lowerCaseText.indexOf("?q?");
		}
		// "text" must be incorrect
		if (begin < 0) {
			return "";
		}
		begin += 3; // after ?B? or ?Q?

		// "text" must be incorrect
		int end = text.lastIndexOf("?=");
		if ((begin > end) || end > text.length()) {
			return "";
		}

		return text.substring(begin, end);
	}

	private String getAfterFirstEncodedPart(String text) {
		int begin = text.indexOf("?=");
		if (begin < 0) {
			return "";
		}
		begin += 2; // after "?="

		// Quoted-printable often get ?Q?=
		String lowerCaseText = text.toLowerCase();
		int wrong = lowerCaseText.indexOf("?q?=") + 4; // after "?Q?="
		if (begin == wrong) {
			begin = lowerCaseText.indexOf("?=", wrong) + 2; // after "?="
		}

		// "text" must be wrong
		if (begin > text.length()) {
			return "";
		}

		return text.substring(begin);
	}

	private String getFirstEncodedPart(String text) {
		int begin = text.indexOf("=?");
		int end = text.indexOf("?=") + 2; // after "?="

		// Quoted-printable often get ?Q?=
		int wrong = text.toLowerCase().indexOf("?q?=") + 4; // after "?Q?="
		if (end == wrong) {
			end = text.indexOf("?=", wrong) + 2; // after "?="
		}

		// they can't be found in "text" or "text" must be wrong
		if ((begin < 0) || end > text.length() || begin > end) {
			return "";
		}

		return text.substring(begin, end);
	}

	private String getBeforeFirstEncodedPart(String text) {
		int end = text.indexOf("=?");

		// "text" must be wrong
		if (end > text.length()) {
			return "";
		}

		return text.substring(0, end);
	}

	private String decodeTextByBase64(String encodedText, String charset)
			throws UnsupportedEncodingException {
		byte[] byteArray = encodedText.getBytes(charset);
		byte[] unbase64 = Base64.decodeBase64(byteArray);
		return new String(unbase64, charset);
	}

	private String decodeTextByQuotedPrintable(String encodedText,
			String charset) throws UnsupportedEncodingException,
			DecoderException {
		// QuotedPrintableCodec version 1.3 don't implement to replace "_" to
		// " ". the rule is in header.
		String grepped = encodedText.replaceAll("_", " ");
		QuotedPrintableCodec codec = new QuotedPrintableCodec();

		return codec.decode(grepped, charset);
	}

	private String decodeTextByUrl(String encodedText, String charset)
			throws UnsupportedEncodingException, DecoderException {
		URLCodec codec = new URLCodec();
		return codec.decode(encodedText, charset);
	}

	/**
	 * decode text encoded by transfer-encoding and charset. "transfer-encoding"
	 * will have "B" or "Q". these are Base64 and Quoted-Printable. "charset"
	 * will have
	 * 
	 * @param encodedText
	 * @param charset
	 *            is "B" or "Q". these means Base64 and Quoted-Printable.
	 * @param encoding
	 *            is "iso-2022-jp", "shift_jis", "iso-8859-1".
	 * @return
	 * @throws UnsupportedEncodingException
	 * @throws DecoderException
	 */
	private String decodeHeader(String encodedText, String charset,
			String encoding) throws UnsupportedEncodingException,
			DecoderException {
		String lowerCaseEncoding = encoding.toLowerCase();
		if (lowerCaseEncoding.equals("b")) {
			return decodeTextByBase64(encodedText, charset);
		} else if (lowerCaseEncoding.equals("q")) {
			return decodeTextByQuotedPrintable(encodedText, charset);
		} else if (lowerCaseEncoding.equals("url")) {
			return decodeTextByUrl(encodedText, charset);
		} else if (lowerCaseEncoding.equals("none")) {
			return new String(encodedText.getBytes(), charset);
		} else {
			return "";
		}
	}
}