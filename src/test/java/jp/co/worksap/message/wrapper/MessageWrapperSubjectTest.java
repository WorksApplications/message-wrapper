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
import java.util.Collection;

import javax.mail.Message;
import javax.mail.MessagingException;

import jp.co.worksap.message.wrapper.MessageWrapper;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class MessageWrapperSubjectTest {

	private static final String filePath = "subject/subject_%s.txt";
	private final String expectedSubject;
	private final String fileName;

	public MessageWrapperSubjectTest(String expectedSubject, String fileName) {
		this.expectedSubject = expectedSubject;
		this.fileName = fileName;

	}

	@Parameters
	public static Collection<Object[]> data() {
		Object[][] data = new Object[][] {
				//Contains special character "Ⅱ"
				{ "Test special character Ⅱ", "001"},
				//Charset is not specified
				{ "Test no char set", "002"},
				//Subject is partly encoded.
				{ "[Test partly encoded] Test partly encoded", "003"}, 
				//Contains special character "～"
				{ "Test special character ～", "004"},
				//Subject is partly encoded and some sections start with not-encoded character.
				{ "テストテスト(テスト2/9(テスト)テスト18:00テスト", "005"},
				//Contains special character "髙"
				{ "Test special character 髙", "006"}	
		};
		return Arrays.asList(data);
	}

	private static Message createMessage(String resourceName) throws MessagingException {
		InputStream is = MessageWrapperSubjectTest.class.getClassLoader()
				.getResourceAsStream(resourceName);
		return new MessageWrapper(null, is);
	}

	@Test
	public void test() throws MessagingException {
		Message message = createMessage(String.format(filePath, fileName));
		assertThat(message.getSubject(), is(expectedSubject));
	}
}
