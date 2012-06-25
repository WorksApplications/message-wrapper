/* 
 * Copyright (C) 2012 Works Applications Co., Ltd. 
 *            http://www.worksap.co.jp/
 * 
 * Licensed under the MIT License:
 *      http://www.opensource.org/licenses/mit-license.php
 * 
 */

package jp.co.worksap.message.decoder;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;

import javax.mail.MessagingException;

import com.sun.mail.util.BASE64DecoderStream;
import com.sun.mail.util.QPDecoderStream;

public class ContentDecoder {
	
	private Reader reader = null;
	
	public String decodeContent(InputStream in, String charset, String encoding) throws MessagingException {
		if (in == null) {
			return null;
		}
		if (encoding == null) {
			return decodeQPContent(in, charset);
		}
		String lowerCaseEncoding = encoding.toLowerCase();
		if (lowerCaseEncoding.equals("base64")) {
			return decodeBase64Content(in, charset);
		} else if (lowerCaseEncoding.equals("quoted-printable")) {
			return decodeQPContent(in, charset);
		} else {
			return decode8BitContent(in, charset);
		}
	}
	
	private String decodeBase64Content(InputStream in, String charset)
			throws MessagingException {
		if (in == null) {
			return null;
		}
		try {
			reader = new InputStreamReader(new BASE64DecoderStream(in), charset);
		} catch (UnsupportedEncodingException e) {
			throw new MessagingException(
					"Failed to decode because of an unsupported encoding", e);
		}
		return readContent(reader);
	}
	
	private String decodeQPContent(InputStream in, String charset)
			throws MessagingException {
		if (in == null) {
			return null;
		}
		try {
			reader = new InputStreamReader(new QPDecoderStream(in), charset);
		} catch (UnsupportedEncodingException e) {
			throw new MessagingException(
					"Failed to decode because of an unsupported encoding", e);
		}
		return readContent(reader);
	}
	
	public String decode8BitContent(InputStream in, String charset)
			throws MessagingException {
		if (in == null) {
			return null;
		}
		try {
			reader = new InputStreamReader(in, charset);
		} catch (UnsupportedEncodingException e) {
			throw new MessagingException(
					"Failed to decode because of an unsupported encoding", e);
		}
		return readContent(reader);
	}

	private String readContent(Reader reader)
			throws MessagingException {
		BufferedReader br = null;
		StringBuilder sb = null;
		try {
			String line;
			br =  new BufferedReader(reader);
			sb = new StringBuilder();
			while ((line = br.readLine()) != null) {
				sb.append(line);
				sb.append("\r\n");
			}
			// delete the last "\r\n"
			sb.delete(sb.length() - 2, sb.length());
			return sb.toString();
		} catch (IOException e) {
			throw new MessagingException("Failed to read data from the source",
					e);
		} finally {
			if (br != null) {
				try {
					br.close();
				} catch (IOException e) {
				}
			}
			if (sb != null) {
				try {
					br.close();
				} catch (IOException e) {
				}
			}
		}
	}
}