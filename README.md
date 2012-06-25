MessageWrapper
--------------

MessageWrapper is a tool to parse and decode the mail sources which are received by [JavaMail].
MessageWrapper could solve the problem of parsing and decoding the mail resources which have errors or illegal formats.

The JavaMail API provides a platform-independent and protocol-independent framework to build mail and messaging applications.

[javamail]:http://www.oracle.com/technetwork/java/javamail/index.html

####Features####

Some features are provided by this tool:

1. Guess the char-set when decoding a text which has no char-set information.
   * When the subject, address or attachment file name has no char-set information in the encoded string, try to guess a char-set from other fields of the message. For example,

     > Abnormal `Subject: $B$4CmJ8>&IJ$N%@;(B`  
     > Normal  `Subject: =?iso-2022-jp?B?GyRCO0hNUSVVJSElJCVrMGxNdxsoQg==?`
   * When the content(text/plain) has no "char-set" field, try to guess a char-set from other fields of the message. For example,

     > Abnormal `Content-Type: text/plain`  
     > Normal `Content-Type: text/plain; charset="iso-2022-jp"`

2. Decode the subject, addresses, content or attachment file name.
   * When the char-set information can not be figured out directly from the filed, try to decode it with the guessed char-set. For example,

     > `Subject: $B$4CmJ8>&IJ$N%@;(B`
   * Decode the encoded string token which begins with an unencoded string. For example,

     > Abnormal `Subject: [test]=?SHIFT_JIS?B?+/yLtCCU/JXbjnE=?=`  
     > Normal `Subject: =?SHIFT_JIS?B?+/yLtCCU/JXbjnE=?=`

3. Decode the mail content correctly when it has an illegal Content-Transfer-Encoding.
   * Illegal Content-Transfer-Encoding refer to those who are not "7bit", "8bit", "binary", "quoted-printable" or "base64". For example,

     > `Content-Transfer-Encoding: ISO-8859-1`

4. Decode the mail content correctly when it has a wrong format content-type.
   * The correct format(with char-set) of content-type must be like: 

     > Correct `Content-Type: text/plain; charset="UTF-8"`  
     > Incorrect `Content-Type: text/plain; charset="UTF-8`
     
####How to use####
MessageWrapper extends javax.mail.Message and users could create it with an instance of javax.mail.Session or a mail source InputStream just like creating an instance of javax.mail.internet.MimeMessage.

    Message message = new MessageWrapper(session, inputStream);   //The "session" could be null;
    or
    Message message = new MessageWrapper(session);
    
Since MessageWrapper focuses on the problem of decoding, it overrides the following methods,

  * public Address[] getReplyTo()
  * public Object getContent()
  * public String getFileName()
  * public InternetAddress[] getFrom()
  * public String getSubject()
  * public InternetAddress[] getRecipients(RecipientType type)
