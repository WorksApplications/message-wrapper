/* 
 * Copyright (C) 2012 Works Applications Co., Ltd. 
 *            http://www.worksap.co.jp/
 * 
 * Licensed under the MIT License:
 *      http://www.opensource.org/licenses/mit-license.php
 * 
 */

package jp.co.worksap.message.wrapper;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

import javax.mail.Address;
import javax.mail.Message;
import javax.mail.Message.RecipientType;
import javax.mail.MessagingException;
import javax.mail.internet.InternetAddress;

import jp.co.worksap.message.wrapper.MessageWrapper;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class MessageWrapperAddressTest {

	private final static String ADDRESS_TEST_PATH = "address/address_%s.txt";
	
	private final static int ADDRESS_FROM = 0;
	private final static int ADDRESS_TO = 1;
	private final static int ADDRESS_CC = 2;
	private final static int ADDRESS_REPLY_TO = 3;

	private final String fileName;
	private final String expectedPerson;
	private final String expectedAddress;
	private final int addressType;

	@Parameters
	public static List<Object[]> prepareMailSource() {
		Object[][] data = new Object[][] {
				//ISO-2022-JP; Not begin with "=?"
				{ "001", "[Test the address] Test the address", "dummy@mail.com", ADDRESS_FROM },
				//GB2312 dose not have the basic character "岡"
				{ "002", "Test a special character岡", "dummy@mail.com", ADDRESS_FROM },
				//SHIFT_JIS can not handle the character "﨑"
				{ "003", "Test a special character﨑", "dummy@mail.com", ADDRESS_TO },
				//iso-2022-jp; A space is added after the name with MimeMessage
				{ "004", "テスト", "dummy@mail.com", ADDRESS_TO },
				//utf-8; “ー” is garbled without MessageWrapper
				{ "005", "Test a special character ー", "dummy@mail.com", ADDRESS_FROM },
				//utf-8; “ペ” is garbled without MessageWrapper
				{ "005", "Test a special character ペ", "dummy@mail.com", ADDRESS_REPLY_TO } };
		return Arrays.asList(data);
	}

	public MessageWrapperAddressTest(String fileName, String expectedPerson,
			String expectedAddress, int addressType) {
		this.fileName = fileName;
		this.expectedPerson = expectedPerson;
		this.expectedAddress = expectedAddress;
		this.addressType = addressType;
	}

	@Test
	public void testAddress() throws MessagingException {
		Message m = createMessage(String.format(ADDRESS_TEST_PATH, fileName));
		Address actualAddress;
		switch (addressType) {
		case ADDRESS_FROM:
			actualAddress = m.getFrom()[0];
			break;
		case ADDRESS_TO:
			actualAddress = m.getRecipients(RecipientType.TO)[0];
			break;
		case ADDRESS_CC:
			actualAddress = m.getRecipients(RecipientType.CC)[0];
			break;
		case ADDRESS_REPLY_TO:
			actualAddress = m.getReplyTo()[0];
			break;
		default:
			throw new IllegalArgumentException("Illegal Address Type");
		}
		assertAddress(actualAddress, expectedPerson, expectedAddress);
	}

	// -----------------------------------------------------------------

	private void assertAddress(Address a, String expectedPerson,
			String expectedAddress) {
		InternetAddress addr = (InternetAddress) a;

		assertThat(addr.getAddress(), is(expectedAddress));
		assertThat(addr.getPersonal(), is(expectedPerson));
	}

	private static Message createMessage(String resourceName)
			throws MessagingException {
		InputStream is = MessageWrapperAddressTest.class.getClassLoader()
				.getResourceAsStream(resourceName);
		return new MessageWrapper(null, is);
	}
}