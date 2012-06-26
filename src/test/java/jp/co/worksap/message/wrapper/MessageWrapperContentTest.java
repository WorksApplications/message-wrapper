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
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Collection;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Part;

import jp.co.worksap.message.wrapper.MessageWrapper;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.google.common.io.Resources;

@RunWith(Parameterized.class)
public class MessageWrapperContentTest {
	private static final String sourceFilePath = "content/content_%s.txt";
	private static final String expectedFilePath = "content/expected/expected_content_%s.txt";
	private static final int EQUAL_TO = 1;
	private static final int STARTS_WITH = 2;
	private static final int CONTAINS = 3;
	private final static String BREAK_CD = System.getProperty("line.separator");

	private final String fileNum;
	private final String encoding;
	private final int checkCondition;

	public MessageWrapperContentTest(String fileNum, String encoding,
			int checkCondition) {
		this.fileNum = fileNum;
		this.encoding = encoding;
		this.checkCondition = checkCondition;
	}

	@Parameters
	public static Collection<Object[]> data() {
		Object[][] data = new Object[][] {
				// charset is not specified (actually ISO-2022-JP)
				{ "001", null, EQUAL_TO },
				// contains special character "﨑"
				{ "002", null, EQUAL_TO },
				// content-type and charset is not specified(actually ISO-2022-JP)
				{ "003", null, EQUAL_TO },
				// "－" is showed as "−"
				{ "004", null, EQUAL_TO },
				// the specified charset is GB2312 but in fact it is GB18030
				{ "005", "GB18030", EQUAL_TO },
				// the charset is CP932
				{ "006", null, EQUAL_TO },
				// contains special character "﨑", "髙"
				{ "007", null, CONTAINS },
				// contains special character "髙" and the specified charset is Shift_JIS but in fact it is MS932
				{ "008", null, STARTS_WITH },
				// the specified charset is Shift_JIS but in fact it is MS932
				{ "009", null, EQUAL_TO },
				// the specified charset is Shift_JIS but in fact it is MS932
				{ "010", null, EQUAL_TO },
				// uses wrong Content-Transfer-Encoding ISO-8859-1
				{ "011", null, EQUAL_TO } };
		return Arrays.asList(data);
	}

	@Test
	public void test() throws MessagingException, IOException {

		Message message = createMessage(String.format(sourceFilePath, fileNum));
		String body;
		if (message.getContentType().startsWith("multipart")) {
			Multipart mp = (Multipart) message.getContent();
			body = getText(mp);
		} else {
			body = (String) message.getContent();
		}
		assertBody(String.format(expectedFilePath, fileNum), body, encoding,
				checkCondition);
	}

	private String getText(Multipart mp) throws MessagingException, IOException {
		Part childPart = mp.getBodyPart(0);
		if (childPart.getContentType().startsWith("text/plain")) {
			return (String) childPart.getContent();
		} else {
			return getText((Multipart) childPart.getContent());
		}
	}

	private static Message createMessage(String resourceName)
			throws MessagingException {
		InputStream is = MessageWrapperContentTest.class.getClassLoader()
				.getResourceAsStream(resourceName);
		return new MessageWrapper(null, is);
	}

	private void assertBody(String expected, String result, String encoding,
			int checkCondition) throws IOException {
		String _expectedBody = replaceLineBreakCode(loadTextFile(expected,
				encoding));
		String _resultBody = replaceLineBreakCode(result);
		switch (checkCondition) {
		case EQUAL_TO:
			assertThat(_resultBody, equalTo(_expectedBody));
			break;
		case STARTS_WITH:
			assertThat(_resultBody.startsWith(_resultBody), is(true));
			break;
		case CONTAINS:
			assertThat(_resultBody.contains(_resultBody), is(true));
			break;
		default:
			break;
		}
	}

	private String loadTextFile(String resourceName, String requiredEncoding)
			throws IOException {
		String encoding = "UTF-8";
		if (requiredEncoding != null) {
			encoding = requiredEncoding;
		}
		URL resourcePath = Resources.getResource(resourceName);
		return Resources.toString(resourcePath, Charset.forName(encoding));
	}

	private String replaceLineBreakCode(String str) {
		return str == null || str.length() == 0 ? str : str.replaceAll(
				"(\\r\\n|\\r|\\n)", BREAK_CD);
	}
}
