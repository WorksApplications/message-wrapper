/* 
 * Copyright (C) 2012 Works Applications Co., Ltd. 
 *            http://www.worksap.co.jp/
 * 
 * Licensed under the MIT License:
 *      http://www.opensource.org/licenses/mit-license.php
 * 
 */

package jp.co.worksap.message.parser;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Enumeration;

import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.internet.MimeBodyPart;

import jp.co.worksap.message.decoder.HeaderDecoder;
import jp.co.worksap.message.util.StringValidator;
import jp.co.worksap.message.wrapper.CharsetGuesser;

import com.google.common.base.Strings;

public class AttachedFileParser {
	private static final String breakLine = "\r\n";
	private static final String NO_FILE_NAME = "no file name";
	
	private final Message message;

	public AttachedFileParser(Message message) {
		this.message = message;
	}

	public String decodeFileName(BodyPart body) throws MessagingException {
		StringBuilder fileHeader = new StringBuilder();
		if (!(body instanceof MimeBodyPart)) {
			return "";
		}

		Enumeration<?> enums = ((MimeBodyPart) body)
				.getNonMatchingHeaderLines(null);
		while (enums.hasMoreElements()) {
			Object o = enums.nextElement();
			if (!(o instanceof String)) {
				continue;
			}

			String temp = ((String) o).toLowerCase();
			if (temp.contains("multipart/")) {
				Object content = null;
				try {
					content = body.getContent();
				} catch (IOException e) {
					throw new MessagingException(
							"Can not get the mail content", e);
				}
				if (content instanceof BodyPart) {
					try {
						return decodeFileName((BodyPart) body.getContent());
					} catch (IOException e) {
						throw new MessagingException(
								"Can not decode the file name", e);
					}
				}
			} else if (temp.contains("message/")) {
				try {
					if (Strings.isNullOrEmpty(body.getFileName())) {
						return NO_FILE_NAME;
					}
				} catch (MessagingException e) {
					throw e;
				}
			}

			fileHeader.append(o);
			fileHeader.append(breakLine);
		}

		HeaderDecoder decoder = new HeaderDecoder();
		String fileName = decoder.decodeFileName(fileHeader.toString());
		return fileName;
	}

	public void fixFileName(Multipart mp) throws MessagingException,
			IOException {
		for (int i = 0; i < mp.getCount(); i++) {
			BodyPart body = mp.getBodyPart(i);
			Object bodyContent = getBodyContent(body);
			if (body.getContent() instanceof Multipart) {
				fixFileName((Multipart) bodyContent);
			}

			String fileName = decodeFileName(body);
			if (fileName == null || fileName.equals("")) {
				continue;
			}

			if (!StringValidator.isValid(fileName)) {
				CharsetGuesser guesser = new CharsetGuesser();
				String charset = guesser.guessCharset(message.getAllHeaders(),
						new ByteArrayInputStream(fileName.getBytes()));
				if (!charset.isEmpty()) {
					try {
						fileName = new String(fileName.getBytes(), charset);
					} catch (UnsupportedEncodingException e) {
					}
				}
			}

			try {
				body.setFileName(fileName);
			} catch (MessagingException e) {
				// retry to set file name.
				// the file name rarely have one-side quotation; "hoge.txt , so
				// check it.
				if (!fileName.startsWith("\"")) {
					fileName = "\"" + fileName;
				}
				if (!fileName.endsWith("\"")) {
					fileName += "\"";
				}
				body.setHeader("Content-Disposition", "attachment; filename="
						+ fileName);
			}
			mp.removeBodyPart(i);
			mp.addBodyPart(body, i);
		}
	}

	private Object getBodyContent(BodyPart p) throws IOException,
			MessagingException {
		try {
			return p.getContent();
		} catch (IOException e) {
			if (e.getMessage().equals("Unknown encoding: 8-bit")) {
				p.setHeader("Content-Transfer-Encoding", "7bit");
				return p.getContent();
			}
			throw e;
		}
	}
}
