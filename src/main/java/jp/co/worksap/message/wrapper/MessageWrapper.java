/* 
 * Copyright (C) 2012 Works Applications Co., Ltd. 
 *            http://www.worksap.co.jp/
 * 
 * Licensed under the MIT License:
 *      http://www.opensource.org/licenses/mit-license.php
 * 
 */

package jp.co.worksap.message.wrapper;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;
import java.util.Map.Entry;

import javax.activation.DataHandler;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.mail.Address;
import javax.mail.Flags;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import jp.co.worksap.message.decoder.ContentDecoder;
import jp.co.worksap.message.decoder.HeaderDecoder;
import jp.co.worksap.message.parser.AddressParser;
import jp.co.worksap.message.parser.AttachedFileParser;
import jp.co.worksap.message.parser.ContentParser;
import jp.co.worksap.message.util.CharsetUtility;
import jp.co.worksap.message.util.Encoding;
import jp.co.worksap.message.util.StringValidator;

public class MessageWrapper extends Message {

	static {
		System.setProperty("sun.nio.cs.map", "x-windows-iso2022jp/ISO-2022-JP");
		System.setProperty("mail.mime.contenttypehandler", "jp.co.worksap.message.util.CharsetMap");
	}

	private static final String REPLY_TO = "Reply-To";

	@Nonnull
	private final Message instance;

	@Nonnull
	private String UID;

	private boolean isFileNameFixed = false;

	public MessageWrapper(@Nullable Session session, @Nonnull InputStream is) {
		checkNotNull(is);
		try {
			this.instance = new MimeMessage(session, is);
		} catch (MessagingException e) {
			throw new RuntimeException("Failed to crate MessageWrapper", e);
		}
	}
	
	public MessageWrapper(@Nonnull Session session) {
		checkNotNull(session);
		this.instance = new MimeMessage(session);
	}

	@Override
	public void addHeader(String headerName, String headerValue)
			throws MessagingException {
		instance.addHeader(headerName, headerValue);
	}

	@Override
	public Enumeration<?> getAllHeaders() throws MessagingException {
		return instance.getAllHeaders();
	}

	@Override
	public Address[] getAllRecipients() throws MessagingException {
		return instance.getAllRecipients();
	}

	@Override
	public void addRecipient(RecipientType type, Address address)
			throws MessagingException {
		instance.addRecipient(type, address);
	}

	@Override
	public Address[] getReplyTo() throws MessagingException {
		String[] replyTo = getHeader(REPLY_TO);
		AddressParser parser = new AddressParser();
		if (parser.isBlankStrings(replyTo)) {
			return getFrom();
		}

		List<Address> addresses = new ArrayList<Address>();
		for (String t : replyTo) {
			String[] splitted = t.split(",");
			for (String s : splitted) {
				addresses.add(parser.getAddressFromRawString(s));
			}
		}

		if (addresses.size() == 0) {
			return getFrom();
		}

		return addresses.toArray(new Address[0]);
	}

	@Override
	public Object getContent() throws IOException, MessagingException {
		if (instance instanceof MimeMessage) {
			ContentParser parser = new ContentParser((MimeMessage) instance);
			if (parser.isMimeMessageIncludingNoCharset()) {
				CharsetGuesser guesser = new CharsetGuesser();
				String charset = guesser.guessCharset(instance.getAllHeaders(),
						((MimeMessage) instance).getRawInputStream());
				return parser.parseContent(charset);
			}
			if (parser.isQuotedPrintableShiftJisContent()) {
				return parser.parseContent(Encoding.MS932);
			}
		}

		Object content = null;
		try {
			content = getContentFromMessage();
		} catch (IOException e) {
			// Message.getContent() throws IOException if the content was
			// broken.
			// This method tries to decode it by ignoring
			// Content-Transfer-Encoding.

			// If the instance is MimeMessage, the method tries to decode it.
			// If succeeds, the method returns decoded string and
			// don't throw any exceptions.
			if (instance instanceof MimeMessage) {
				ContentParser parser = new ContentParser((MimeMessage) instance);
				ContentDecoder decoder = new ContentDecoder();
				return decoder.decodeContent(
						((MimeMessage) instance).getRawInputStream(),
						parser.getCharset(), "8bit");
			}

			// if these trial is failed, throw exception.
			throw e;
		}

		if (!(content instanceof Multipart)) {
			return instance.getContent();
		}

		if (!isFileNameFixed) {
			// This is multipart and attached file name is often broken.
			// This method repairs them and repackage them.
			AttachedFileParser parser = new AttachedFileParser(instance);
			parser.fixFileName((Multipart) content);
			isFileNameFixed = true;
		}
		return content;
	}

	@Override
	public String getContentType() throws MessagingException {
		return instance.getContentType();
	}

	@Override
	public DataHandler getDataHandler() throws MessagingException {
		return instance.getDataHandler();
	}

	@Override
	public String getDescription() throws MessagingException {
		return instance.getDescription();
	}

	@Override
	public String getDisposition() throws MessagingException {
		return instance.getDisposition();
	}

	@Override
	public String getFileName() throws MessagingException {
		if (instance instanceof MimeMessage
				&& instance.isMimeType("application/*")) {
			String[] disposition = instance.getHeader("Content-Disposition");
			String s = null;
			if (disposition != null && disposition.length > 0) {
				s = "\r\ncontent-disposition: " + disposition[0];
			}
			if (s == null) {
				s = instance.getContentType();
			}
			if (s != null) {
				if (isFileNameFixed) {
					return instance.getFileName();
				}
				return (new HeaderDecoder()).decodeFileName(s);
			}
		}

		return instance.getFileName();
	}

	@Override
	public String[] getHeader(String headerName) throws MessagingException {
		return instance.getHeader(headerName);
	}

	@Override
	public InputStream getInputStream() throws IOException, MessagingException {
		return instance.getInputStream();
	}

	@Override
	public int getLineCount() throws MessagingException {
		return instance.getLineCount();
	}

	@Override
	public Enumeration<?> getMatchingHeaders(String[] headerName)
			throws MessagingException {
		return instance.getMatchingHeaders(headerName);
	}

	@Override
	public Enumeration<?> getNonMatchingHeaders(String[] headerName)
			throws MessagingException {
		return instance.getNonMatchingHeaders(headerName);
	}

	@Override
	public int getSize() throws MessagingException {
		return instance.getSize();
	}

	@Override
	public boolean isMimeType(String mimeType) throws MessagingException {
		return instance.isMimeType(mimeType);
	}

	@Override
	public void removeHeader(String headerName) throws MessagingException {
		instance.removeHeader(headerName);
	}

	@Override
	public void setContent(Multipart mp) throws MessagingException {
		instance.setContent(mp);
	}

	@Override
	public void setContent(Object obj, String type) throws MessagingException {
		instance.setContent(obj, type);
	}

	@Override
	public void setDataHandler(DataHandler dh) throws MessagingException {
		instance.setDataHandler(dh);
	}

	@Override
	public void setDescription(String description) throws MessagingException {
		instance.setDescription(description);
	}

	@Override
	public void setDisposition(String disposition) throws MessagingException {
		instance.setDisposition(disposition);
	}

	@Override
	public void setFileName(String fileName) throws MessagingException {
		instance.setFileName(fileName);
	}

	@Override
	public void setHeader(String headerName, String headerValue)
			throws MessagingException {
		instance.setHeader(headerName, headerValue);
	}

	@Override
	public void setText(String text) throws MessagingException {
		instance.setText(text);
	}

	@Override
	public void writeTo(OutputStream os) throws IOException, MessagingException {
		instance.writeTo(os);
	}

	@Override
	public void addFrom(Address[] addresses) throws MessagingException {
		instance.addFrom(addresses);
	}

	@Override
	public void addRecipients(RecipientType type, Address[] addresses)
			throws MessagingException {
		instance.addRecipients(type, addresses);
	}

	@Override
	public Flags getFlags() throws MessagingException {
		return instance.getFlags();
	}

	/**
	 * return "from" field. MessageWraper return Address[0] when the field is
	 * null. because no one need to distinguish "empty address field" and
	 * "no address field".
	 * 
	 * FYI, rfc2822 says "from" field appears just 1 time and no value is
	 * unacceptable.
	 */
	@Override
	public InternetAddress[] getFrom() throws MessagingException {
		// rfc2822 says "from" field appears just 1 time.
		// but Message class returns null when "from" field does not appear
		Address[] garble = instance.getFrom();
		String[] constructable = getHeader("from");

		try {
			AddressParser parser = new AddressParser();
			return parser.fixAddress(garble, constructable);
		} catch (MessagingException e) {
			return cast(garble);
		}
	}

	/**
	 * MessageWrapper.getSubject() don't distinguish "no subject field" and
	 * "no sentence subject"
	 */
	@Override
	public String getSubject() throws MessagingException {
		String[] subject = getHeader("subject");
		if ((subject == null) || (subject[0] == null)) {
			// rfc2822 says "subject field appears 0 or 1 time"
			return "";
		}

		// don't care lower case or upper
		String lower = subject[0].toLowerCase();
		if (CharsetUtility.needsMapping(lower)) {
			HeaderDecoder decoder = new HeaderDecoder();
			String decoded = decoder.decodeSubject(subject[0]);
			return decoded.replaceAll("\\r\\n", "");
		}

		String decoded = instance.getSubject();

		// try to decode if it contains invalid char
		if (!StringValidator.isValid(decoded)) {
			CharsetGuesser guesser = new CharsetGuesser();
			String charset = guesser.guessCharset(instance.getAllHeaders(),
					new ByteArrayInputStream(subject[0].getBytes()));
			if (!charset.isEmpty()) {
				try {
					return new String(subject[0].getBytes(), charset);
				} catch (UnsupportedEncodingException e) {
					throw new MessagingException("Unsupported encoding", e);
				}
			}
		}

		return decoded;
	}

	@Override
	public Date getReceivedDate() throws MessagingException {
		return instance.getReceivedDate();
	}

	/**
	 * return "to" or "cc" or "bcc" field. MessageWraper return Address[0] when
	 * the field is null. because no one need to distinguish
	 * "empty address field" and "no address field".
	 * 
	 * FYI, rfc2822 says "to" and/or "cc" field appears 0 or 1 time.
	 */
	@Override
	public InternetAddress[] getRecipients(RecipientType type)
			throws MessagingException {
		Address[] garble = instance.getRecipients(type);
		String[] constructable = getHeader(type.toString());
		if (garble == null || constructable == null) {
			return new InternetAddress[0];
		}

		try {
			AddressParser parser = new AddressParser();
			return parser.fixAddress(garble, constructable);
		} catch (MessagingException e) {
			return cast(garble);
		}

	}

	@Override
	public Date getSentDate() throws MessagingException {
		return instance.getSentDate();
	}

	@Override
	public Message reply(boolean replyToAll) throws MessagingException {
		return instance.reply(replyToAll);
	}

	@Override
	public void saveChanges() throws MessagingException {
		instance.saveChanges();
	}

	@Override
	public void setFlags(Flags flag, boolean set) throws MessagingException {
		instance.setFlags(flag, set);
	}

	@Override
	public void setFrom() throws MessagingException {
		instance.setFrom();
	}

	@Override
	public void setFrom(Address address) throws MessagingException {
		instance.setFrom(address);
	}

	@Override
	public void setRecipients(RecipientType type, Address[] addresses)
			throws MessagingException {
		instance.setRecipients(type, addresses);
	}

	@Override
	public void setSentDate(Date date) throws MessagingException {
		instance.setSentDate(date);
	}

	@Override
	public void setSubject(@Nullable String subject) throws MessagingException {
		if (subject == null) {
			instance.setSubject("");
		} else {
			instance.setSubject(subject);
		}
	}

	@Nonnull
	public String getUID() {
		if (UID == null) {
			throw new IllegalStateException("UID is not set yet.");
		}
		return UID;
	}

	public void setUID(@Nonnull String uid) {
		this.UID = checkNotNull(uid);
	}

	private Object getContentFromMessage() throws IOException,
			MessagingException {
		try {
			return instance.getContent();
		} catch (UnsupportedEncodingException e) {
			// fix the format of the "charset" field
			String wrongCharset = e.getMessage().toLowerCase();
			for (Entry<String, String> entry : CharsetUtility.getCharsetMap()
					.entrySet()) {
				if (wrongCharset.contains(entry.getKey())) {
					String contentType = getContentType();
					setHeader("Content-Type", contentType.split(";")[0]
							+ "; charset=" + entry.getValue());
					return instance.getContent();
				}
			}

			throw e;
		}
	}

	private InternetAddress[] cast(Address[] garble) {
		InternetAddress[] casted = new InternetAddress[garble.length];
		for (int i = 0; i < casted.length; ++i) {
			casted[i] = (InternetAddress) garble[i];
		}
		return casted;
	}
}