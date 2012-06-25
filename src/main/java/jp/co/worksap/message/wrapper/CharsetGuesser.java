/* 
 * Copyright (C) 2012 Works Applications Co., Ltd. 
 *            http://www.worksap.co.jp/
 * 
 * Licensed under the MIT License:
 *      http://www.opensource.org/licenses/mit-license.php
 * 
 */

package jp.co.worksap.message.wrapper;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CoderResult;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;

import javax.mail.Header;

import jp.co.worksap.message.util.CharsetUtility;
import jp.co.worksap.message.util.Encoding;

public class CharsetGuesser {
	
	private static final int SIZE = 1024;

	public String guessCharset(Enumeration<?> e, InputStream input) {
		Set<String> charSets = guessCharset(e);
		for (String charset: charSets) {
			try {
				if (!charset.equals(Encoding.GB18030) && canDecode(input, Charset.forName(charset))) {
					return charset;
				}
			} catch (IOException ex) {
				continue;
			}
		}
		if (charSets.contains(Encoding.GB18030)) {
			return Encoding.GB18030;
		}
		return "";
	}
	
	
	private Set<String> guessCharset(Enumeration<?> e) {
		Set<String> charSets = new HashSet<String>();
		while (e.hasMoreElements()) {
			Object o = e.nextElement();
			if (o instanceof Header) {
				String value = ((Header) o).getValue().toLowerCase();
				String charSet = CharsetUtility.getMappingCharSet(value);
				if (charSet.isEmpty()) {
					continue;
				} else {
					charSets.add(charSet);
				}
			}
		}

		return charSets;
	}
	
	private boolean canDecode(InputStream input, Charset charset)
			throws IOException {
		ReadableByteChannel channel = Channels.newChannel(input);
		CharsetDecoder decoder = charset.newDecoder();

		ByteBuffer byteBuffer = ByteBuffer.allocate(SIZE * 2);
		CharBuffer charBuffer = CharBuffer.allocate(SIZE);

		boolean endOfInput = false;
		while (!endOfInput) {
			int n = channel.read(byteBuffer);
			byteBuffer.flip();

			endOfInput = (n == -1);
			CoderResult coderResult = decoder.decode(byteBuffer, charBuffer,
					endOfInput);
			if (coderResult.isError()) {
				return false;
			}
			charBuffer.clear();
			while (coderResult == CoderResult.OVERFLOW) {
				coderResult = decoder
						.decode(byteBuffer, charBuffer, endOfInput);
				charBuffer.clear();
			}

			byteBuffer.compact();
		}
		CoderResult coderResult;
		while ((coderResult = decoder.flush(charBuffer)) == CoderResult.OVERFLOW) {
			charBuffer.clear();
		}
		if (coderResult.isError()) {
			return false;
		}

		return true;
	}
}
