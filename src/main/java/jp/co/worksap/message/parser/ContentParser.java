/* 
 * Copyright (C) 2012 Works Applications Co., Ltd. 
 *            http://www.worksap.co.jp/
 * 
 * Licensed under the MIT License:
 *      http://www.opensource.org/licenses/mit-license.php
 * 
 */

package jp.co.worksap.message.parser;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;

import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.internet.MimeMessage;

import jp.co.worksap.message.decoder.ContentDecoder;
import jp.co.worksap.message.util.Encoding;

public class ContentParser {
	
	private MimeMessage instance;
	
	public ContentParser(MimeMessage instance) {
		this.instance = instance;
	}
	
	public Object parseContent(String charset)
			throws MessagingException, IOException {
		InputStream in = instance.getRawInputStream();
		String encoding = instance.getEncoding();
		try {
			if (!charset.isEmpty()) {
				ContentDecoder decoder = new ContentDecoder();
				return decoder.decodeContent(in, charset, encoding);
			} else {
				return instance.getContent();
			}
		} catch (UnsupportedEncodingException e) {
			throw new MessagingException(
					"Can not decode the content because of an unsupported ecoding.",
					e);
		}
	}
	
	public boolean isMimeMessageIncludingNoCharset()
			throws MessagingException, IOException {
		if (!getCharset().isEmpty()) {
			return false;
		}

		if (instance.getContent() instanceof Multipart) {
			return false;
		}

		return true;
	}
	
	// instance.getContent() cannot decode special case that the content is
	// encoded by quoted-printable and shift_jis
	public boolean isQuotedPrintableShiftJisContent()
			throws MessagingException {
		String encoding = instance.getEncoding();
		if ((encoding == null)
				|| !encoding.equalsIgnoreCase("quoted-printable")) {
			return false;
		}
		String charset = getCharset();
		if ((charset == null)
				|| (!charset.equalsIgnoreCase(Encoding.SHIFT_JIS))) {
			return false;
		}
		return true;
	}
	
	public String getCharset() {
		String targetKey = "charset=";

		String charset = "";
		String contentType;
		try {
			contentType = instance.getContentType();
		} catch (MessagingException e) {
			return charset;
		}
		String[] elems = contentType.split(";");
		for (String elem : elems) {
			if (elem.trim().startsWith(targetKey)) {
				charset = elem.trim().substring(targetKey.length());
			}
		}

		charset = trimDoubleQuotations(charset);
		return charset;
	}
	
	/**
	 * remove double quotations at the both ends.
	 * 
	 * @param quotedText
	 * @return
	 */
	private String trimDoubleQuotations(String quotedText) {
		String text = quotedText;
		if (text != null) {
			if (text.startsWith("\"")) {
				text = text.substring(1);
			}
			if (text.endsWith("\"")) {
				text = text.substring(0, text.length() - 1);
			}
		}

		return text;
	}
}
