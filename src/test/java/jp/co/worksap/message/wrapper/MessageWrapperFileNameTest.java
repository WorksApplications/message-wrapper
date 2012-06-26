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

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Part;

import jp.co.worksap.message.wrapper.MessageWrapper;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class MessageWrapperFileNameTest {
	
	private final static String FILENAME_TEST_PATH = "filename/filename_%s.txt";

	private final String fileName;
	private final String expectedFileName;
	
	@Parameters
	public static List<Object[]> prepareMailSource() {
		Object[][] datas = new Object[][] {
				//base64; ISO-2022-JP; Can be fixed by setting "mail.mime.decodefilename" as true
				{ "001", "file%test【special characterⅡ】.ods" },
				//base64; iso-2022-jp, URLEncode;
				{ "002", "画テスト.svg" },
				//base64; iso-2022-jp, URLEncode;
				{ "003", "テスト①②③④⑤⑥⑦⑧⑨testⅠⅡⅢⅣⅤⅥⅦⅧⅨtest髙閒塚德﨑彅弴燁珉鄧テスト㈱.txt" },
				//Declared as base64, is 7bit; No char-set, is ISO-2022-JP 
				{ "004", "テストファイル.xls" },
				//base64; ISO-2022-JP; Not begin with "=?"
				{ "005", "1234_Test file name.xls" },
		};
		return Arrays.asList(datas);
	}
	
	public MessageWrapperFileNameTest(String fileName, String expectedFileName) {
		this.fileName = fileName;
		this.expectedFileName = expectedFileName;
	}
	
	@Test
	public void testFileName() throws MessagingException, IOException {
		Message m = createMessage(String.format(FILENAME_TEST_PATH, fileName));
		Multipart mp = (Multipart) m.getContent();
		Part file = mp.getBodyPart(1);
		assertThat(file.getFileName(), is(expectedFileName));
	}
	
	private static Message createMessage(String resourceName) throws MessagingException {
		InputStream is = MessageWrapperFileNameTest.class.getClassLoader().getResourceAsStream(resourceName);
		return new MessageWrapper(null, is);
	}
}
