/* 
 * Copyright (C) 2012 Works Applications Co., Ltd. 
 *            http://www.worksap.co.jp/
 * 
 * Licensed under the MIT License:
 *      http://www.opensource.org/licenses/mit-license.php
 * 
 */

package jp.co.worksap.message.parser;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.mail.Address;
import javax.mail.MessagingException;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;

import com.google.common.base.Strings;

import jp.co.worksap.message.decoder.HeaderDecoder;
import jp.co.worksap.message.util.CharsetUtility;
import jp.co.worksap.message.util.Encoding;

public class AddressParser {
	
	private static final Pattern IN_BRACKET_FILTER = Pattern.compile("<(.*)>",
			Pattern.CASE_INSENSITIVE);
	private static final Pattern IN_DOUBLE_QUOTATION_FILTER = Pattern.compile(
			"\"(.*)\"", Pattern.CASE_INSENSITIVE);
	private static final Pattern OUT_OF_BRACKET_FILTER = Pattern.compile(
			"(.*)<.*>", Pattern.CASE_INSENSITIVE);
	

	/**
	 * as possible as decoding the String value
	 * 
	 * @param garble
	 * @param constructable
	 * @return
	 * @throws MessagingException
	 */
	public InternetAddress[] fixAddress(Address[] garble,
			String[] constructable) throws MessagingException {
		// it returns an empty array instead of null when there is no recipient.
		if (garble == null || constructable == null) {
			return new InternetAddress[0];
		}

		String[] raw = splitAddressString(constructable);
		Address[] address = resplitAddress(garble);

		// these addresses must be wrong.
		if (raw.length != address.length) {
			InternetAddress[] fixed = new InternetAddress[raw.length];
			for (int i = 0; i < raw.length; i++) {
				fixed[i] = getAddressFromRawString(raw[i]);
			}
			return fixed;
		}

		// these addresses may be wrong
		InternetAddress[] fixed = new InternetAddress[raw.length];
		for (int i = 0; i < raw.length; i++) {
			// "undecodable" means Message class can't decode it
			String lowerCase = raw[i].toLowerCase();
			boolean undecodableUtf8 = lowerCase.contains(Encoding.UTF8)
					&& raw[i].contains("\r\n");

			if (undecodableUtf8 || CharsetUtility.needsMapping(lowerCase)) {
				fixed[i] = getAddressFromRawString(raw[i]);
			} else {
				fixed[i] = (InternetAddress) address[i];
				fixed[i].getPersonal();
			}
		}
		return fixed;
	}

	public InternetAddress getAddressFromRawString(String text)
			throws MessagingException {
		HeaderDecoder decoder = new HeaderDecoder();
		String decoded = decoder.decodeAddress(text);
		String email = getEmail(decoded);
		String personal = null;
		if (text.contains("\"")) {
			personal = getPersonal(decoded);
		} else {
			personal = getPersonalWithQuote(decoded);
		}

		if (personal.isEmpty()) {
			try {
				return new InternetAddress(email);
			} catch (AddressException e) {
				throw new MessagingException(
						"Can not get the address because its format is wrong",
						e);
			}
		} else {
			try {
				return new InternetAddress(email, personal);
			} catch (UnsupportedEncodingException e) {
				throw new MessagingException(
						"Can not get the address because of an unsupported encoding",
						e);
			}
		}
	}
	
	private String[] splitAddressString(String[] address) {
		List<String> list = new ArrayList<String>();
		for (String ad : address) {
			String[] splitted = ad.split(",");
			for (int i = 0; i < splitted.length; i++) {
				// to repair double-quoted comma
				if (isEvenNumberOfQuot(splitted[i])) {
					// this is closed quotations
					list.add(splitted[i]);
				} else {
					// this is opened quotations
					StringBuilder sb = new StringBuilder();
					sb.append(splitted[i]);
					i++;
					for (/* do nothing */; i < splitted.length; i++) {
						sb.append(",");
						sb.append(splitted[i]);
						if (!isEvenNumberOfQuot(splitted[i])) {
							// quotations become closed
							break;
						}
					}
					list.add(sb.toString());
				}
			}
		}
		return list.toArray(new String[0]);
	}
	
	private Address[] resplitAddress(Address[] garble)
			throws MessagingException {
		List<Address> addressList = new ArrayList<Address>();
		for (Address a : garble) {
			String[] splitted = a.toString().split(",");
			if (splitted.length == 1) {
				addressList.add(a);
				continue;
			}
			for (String s : splitted) {
				try {
					addressList.add(new InternetAddress(getEmail(s),
							getPersonal(s)));
				} catch (UnsupportedEncodingException e) {
					throw new MessagingException(
							"Can not split the address because of an unsupported exception",
							e);
				}
			}
		}
		return addressList.toArray(new InternetAddress[0]);
	}
	
	private String getEmail(String text) {
		Matcher matcher = IN_BRACKET_FILTER.matcher(text);

		if (!matcher.find()) {
			return text;
		}

		if (matcher.groupCount() < 1) {
			return text;
		}

		return matcher.group(1);
	}
	
	/**
	 * return PERSONAL. PERSONAL means String in double-quotations or out of
	 * angle-brackets. if there is not them, return empty string.
	 * 
	 * @param text
	 * @return
	 */
	private String getPersonal(String text) {
		// must be personal in quotations
		Matcher quotation = IN_DOUBLE_QUOTATION_FILTER.matcher(text);
		if (quotation.find()) {
			if (quotation.groupCount() >= 1) {
				return quotation.group(1);
			}
		}

		// must be email in brackets
		// must be personal out of brackets
		Matcher nonBracket = OUT_OF_BRACKET_FILTER.matcher(text);
		if (nonBracket.find()) {
			if (nonBracket.groupCount() >= 1) {
				return nonBracket.group(1).trim();
			}
		}

		// If no " " and < > found, "text" can be an address or person name
		try {
			new InternetAddress(text);
			return ""; // If it can be parsed to address, the person's name is
						// empty
		} catch (AddressException e) {
			return text; // If it's not an address, it's person's name
		}
	}

	// this is for encoded quote
	private String getPersonalWithQuote(String text) {
		// must be email in brackets
		// must be personal out of brackets
		Matcher nonBracket = OUT_OF_BRACKET_FILTER.matcher(text);
		if (nonBracket.find()) {
			if (nonBracket.groupCount() >= 1) {
				return nonBracket.group(1).trim();
			}
		}

		// If no " " and < > found, "text" can be an address or person name
		try {
			new InternetAddress(text);
			return ""; // If it can be parsed to address, the person's name is
						// empty
		} catch (AddressException e) {
			return text; // If it's not an address, it's person's name
		}
	}
	
	private boolean isEvenNumberOfQuot(String text) {
		int count = 0;
		int indexOfQuot = -1;
		while (true) {
			indexOfQuot = text.indexOf("\"", indexOfQuot + 1);
			if (indexOfQuot < 0) {
				break;
			}
			count++;
		}

		if (count % 2 == 0) {
			return true;
		}

		return false;
	}
	

	public boolean isBlankStrings(String[] array) {
		if (array == null || array.length == 0) {
			return true;
		}
		for (String s : array) {
			if (!Strings.isNullOrEmpty(s)) {
				return false;
			}
		}
		return true;
	}
}
