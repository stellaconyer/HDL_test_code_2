package com.humdynlog;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.*;
import javax.activation.*;
import javax.mail.*;
import javax.mail.internet.*;

public class Mail extends javax.mail.Authenticator
{
	String email = "dummyaccount@gmail.com", password = "magicbeans%",
		smtpHost = "smtp.gmail.com", smtpPort = "465",
		toField = "dummyaccount@gmail.com";

	public Mail()
	{
		// There is something wrong with MailCap, javamail can not find a handler for the multipart/mixed part, so this bit needs to be added.
		MailcapCommandMap mc = (MailcapCommandMap)CommandMap.getDefaultCommandMap();
		mc.addMailcap("text/html;; x-java-content-handler=com.sun.mail.handlers.text_html");
		mc.addMailcap("text/xml;; x-java-content-handler=com.sun.mail.handlers.text_xml");
		mc.addMailcap("text/plain;; x-java-content-handler=com.sun.mail.handlers.text_plain");
		mc.addMailcap("multipart/*;; x-java-content-handler=com.sun.mail.handlers.multipart_mixed");
		mc.addMailcap("message/rfc822;; x-java-content-handler=com.sun.mail.handlers.message_rfc822");
		CommandMap.setDefaultCommandMap(mc);
	}

	public String sendEmailSubmission(String subjectText, String msgBodyText, File attach)
	{
		Properties props = new Properties();

		props.put("mail.debug","true");
//		props.put("mail.smtp.debug","true");
		props.put("mail.smtp.user", email);
		props.put("mail.smtp.host", smtpHost);
		props.put("mail.smtp.port", smtpPort);
		props.put("mail.smtp.starttls.enable", "true");
		props.put("mail.smtp.auth", "true");
		props.put("mail.smtp.socketFactory.port", smtpPort);
		props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
		props.put("mail.smtp.socketFactory.fallback", "false");

		javax.mail.Authenticator auth = new SMTPAuthenticator();
		Session session = Session.getInstance(props, auth);
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		session.setDebugOut(new PrintStream(baos));

		try
		{
			MimeMessage msg = new MimeMessage(session);
			msg.setSubject(subjectText);
			msg.setFrom(new InternetAddress(email));
			msg.addRecipient(MimeMessage.RecipientType.TO, new InternetAddress(toField));
			msg.setSentDate(new Date());

			BodyPart msgBodyPart = new MimeBodyPart();
			msgBodyPart.setText(msgBodyText);
			Multipart multipart = new MimeMultipart();
			multipart.addBodyPart(msgBodyPart);

            msgBodyPart = new MimeBodyPart();
            DataSource source = new FileDataSource(attach);
            msgBodyPart.setDataHandler(new DataHandler(source));
            msgBodyPart.setFileName(attach.getName());
            multipart.addBodyPart(msgBodyPart);

			msg.setContent(multipart);
			msg.saveChanges();

			Transport.send(msg);
		}
		catch (Exception e)
		{
			// Something went wrong while trying to email: report exception back as string
			StringWriter sw = new StringWriter();
			PrintWriter pw = new PrintWriter(sw);
			e.printStackTrace(pw);
			String[] lines = sw.toString().split("\\r?\\n");
			return lines[0];
//			return baos.toString();		// Report full SMTP debug info as string
		}
//		return baos.toString();		// Report full SMTP debug info as string
		return "";
	}

	private class SMTPAuthenticator extends javax.mail.Authenticator
	{
		public javax.mail.PasswordAuthentication getPasswordAuthentication()
		{
			return new javax.mail.PasswordAuthentication(email, password);
		}
	}
}
