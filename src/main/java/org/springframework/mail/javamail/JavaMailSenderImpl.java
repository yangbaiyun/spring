/*
 * The Spring Framework is published under the terms
 * of the Apache Software License.
 */

package org.springframework.mail.javamail;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.mail.AuthenticationFailedException;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.NoSuchProviderException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.MailException;
import org.springframework.mail.MailParseException;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;

/**
 * Implementation of the JavaMailSender interface.
 * Can also be used as plain MailSender implementation.
 *
 * <p>Allows for defining all settings locally as bean properties.
 * Alternatively, a pre-configured JavaMail Session can be specified,
 * possibly pulled from an application server's JNDI environment.
 *
 * <p>Non-default properties in this object will always override the settings
 * in the JavaMail Session. Note that if overriding all values locally, there
 * is no value in setting a pre-configured Session.
 *
 * @author Dmitriy Kopylenko
 * @author Juergen Hoeller
 * @see JavaMailSender
 * @see org.springframework.mail.MailSender
 * @version $Id: JavaMailSenderImpl.java,v 1.7 2003/11/07 21:39:24 jhoeller Exp $
 */
public class JavaMailSenderImpl implements JavaMailSender {

	public static final String DEFAULT_PROTOCOL = "smtp";

	public static final int DEFAULT_PORT = -1;

	protected final Log logger = LogFactory.getLog(getClass());

	private Session session = Session.getInstance(new Properties());

	private String protocol = DEFAULT_PROTOCOL;

	private String host;

	private int port = DEFAULT_PORT;

	private String username;

	private String password;

	/**
	 * Set the JavaMail Session, possibly pulled from JNDI. Default is an new Session
	 * without defaults, i.e. completely configured via this object's properties.
	 * <p>If using a pre-configured Session, non-default properties in this object
	 * will override the settings in the Session.
	 */
	public void setSession(Session session) {
		if (session == null) {
			throw new IllegalArgumentException("Cannot work with a null Session");
		}
		this.session = session;
	}

	/**
	 * Set the mail protocol. Default is SMTP.
	 */
	public void setProtocol(String protocol) {
		this.protocol = protocol;
	}

	/**
	 * Set the mail server host, typically an SMTP host.
	 */
	public void setHost(String host) {
		this.host = host;
	}

	/**
	* Set the mail server port. Default is 25.
	*/
	public void setPort(int port) {
		this.port = port;
	}

	/**
	 * Set the username for the account at the mail host, if any.
	 */
	public void setUsername(String username) {
		this.username = username;
	}

	/**
	 * Set the password for the account at the mail host, if any.
	 */
	public void setPassword(String password) {
		this.password = password;
	}

	public void send(SimpleMailMessage simpleMessage) throws MailException {
		send(new SimpleMailMessage[] { simpleMessage });
	}

	public void send(SimpleMailMessage[] simpleMessages) throws MailException {
		try {
			List mimeMessages = new ArrayList();
			for (int i = 0; i < simpleMessages.length; i++) {
				SimpleMailMessage simpleMessage = simpleMessages[i];
				if (logger.isDebugEnabled())
					logger.debug("Sending email using the following mail properties [" + simpleMessage + "]");
				MimeMessage mimeMessage = createMimeMessage();
				if (simpleMessage.getFrom() != null) {
					mimeMessage.setFrom(new InternetAddress(simpleMessage.getFrom()));
				}
				if (simpleMessage.getTo() != null) {
					mimeMessage.addRecipient(Message.RecipientType.TO, new InternetAddress(simpleMessage.getTo()));
				}
				if (simpleMessage.getCc() != null) {
					for (int j = 0; j < simpleMessage.getCc().length; j++) {
						mimeMessage.addRecipient(Message.RecipientType.CC, new InternetAddress(simpleMessage.getCc()[j]));
					}
				}
				if (simpleMessage.getBcc() != null) {
					for (int j = 0; j < simpleMessage.getBcc().length; j++) {
						mimeMessage.addRecipient(Message.RecipientType.BCC, new InternetAddress(simpleMessage.getBcc()[j]));
					}
				}
				if (simpleMessage.getSubject() != null) {
					mimeMessage.setSubject(simpleMessage.getSubject());
				}
				if (simpleMessage.getText() != null) {
					mimeMessage.setText(simpleMessage.getText());
				}
				mimeMessages.add(mimeMessage);
			}
			send((MimeMessage[]) mimeMessages.toArray(new MimeMessage[mimeMessages.size()]), simpleMessages);
		}
		catch (MessagingException ex) {
			throw new MailParseException(ex);
		}
	}

	public MimeMessage createMimeMessage() {
		return new MimeMessage(this.session);
	}

	public void send(MimeMessage mimeMessage) throws MailException {
		send(new MimeMessage[] { mimeMessage });
	}

	public void send(MimeMessage[] mimeMessages) throws MailException {
		send(mimeMessages, null);
	}

	public void send(MimeMessage[] mimeMessages, Object[] originalMessages) throws MailException {
		Map failedMessages = new HashMap();
		try {
			Transport transport = getTransport(this.session);
			transport.connect(this.host, this.port, this.username, this.password);
			try {
				for (int i = 0; i < mimeMessages.length; i++) {
					MimeMessage mimeMessage = mimeMessages[i];
					try {
						mimeMessage.saveChanges();
						transport.sendMessage(mimeMessage, mimeMessage.getAllRecipients());
					}
					catch (MessagingException ex) {
						Object original = (originalMessages != null) ? originalMessages[i] : mimeMessage;
						failedMessages.put(original, ex);
					}
				}
			}
			finally {
				transport.close();
			}
		}
		catch (AuthenticationFailedException ex) {
			throw new MailAuthenticationException(ex);
		}
		catch (MessagingException ex) {
			throw new MailSendException("Mail server connection failed", ex);
		}
		if (!failedMessages.isEmpty()) {
			throw new MailSendException(failedMessages);
		}
	}

	public void send(MimeMessagePreparator mimeMessagePreparator) throws MailException {
		send(new MimeMessagePreparator[] { mimeMessagePreparator });
	}

	public void send(MimeMessagePreparator[] mimeMessagePreparators) throws MailException {
		try {
			List mimeMessages = new ArrayList();
			for (int i = 0; i < mimeMessagePreparators.length; i++) {
				MimeMessage mimeMessage = createMimeMessage();
				mimeMessagePreparators[i].prepare(mimeMessage);
				mimeMessages.add(mimeMessage);
			}
			send((MimeMessage[])mimeMessages.toArray(new MimeMessage[mimeMessages.size()]));
		}
		catch (MessagingException ex) {
			throw new MailParseException(ex);
		}
	}

	protected Transport getTransport(Session session) throws NoSuchProviderException {
		return session.getTransport(this.protocol);
	}

}
