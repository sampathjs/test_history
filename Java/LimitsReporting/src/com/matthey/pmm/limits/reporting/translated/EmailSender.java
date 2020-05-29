package com.matthey.pmm.limits.reporting.translated;

import java.util.Properties;
import java.util.Set;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import com.olf.jm.logging.Logging;


public class EmailSender {
    private static final String host = System.getenv("AB_EMAIL_SMTP_HOST");
    private static final String fromAddress = "eod.limits.reporting@matthey.com";
    
    {
    	Logging.info("email host: " + host +";'From' address for emails: " + fromAddress);
    }
    
    public static void send (final String subject, final String html, final Set<String> emails) {
        StringBuilder emailsListCommaSeparated = createCommaSeparatedList(emails);
    	try {
            Properties properties = System.getProperties();
            properties.setProperty ("mail.smtp.host", host);
            MimeMessage message = new MimeMessage(Session.getDefaultInstance(properties));
            message.setFrom (new InternetAddress(fromAddress));
            message.addRecipients(Message.RecipientType.TO, InternetAddress.parse(emailsListCommaSeparated.toString()));
            message.setSubject (subject);
            MimeBodyPart mimeBodyPart = new MimeBodyPart();
            mimeBodyPart.setContent(html, "text/html");
            Multipart multipart = new MimeMultipart();
            multipart.addBodyPart(mimeBodyPart);
            message.setContent(multipart);
            Transport.send(message);    		
    	} catch (AddressException ex) {
    		Logging.error(ex.toString());
    		for (StackTraceElement ste : ex.getStackTrace()) {
    			Logging.error(ste.toString());
    		}
    		throw new RuntimeException ("Sending email: From Adress  '" + fromAddress + 
    				"' or To Address '" + emailsListCommaSeparated + "' illegal:");
    	} catch (MessagingException ex) {
    		Logging.error(ex.toString());
    		for (StackTraceElement ste : ex.getStackTrace()) {
    			Logging.error(ste.toString());
    		}
    		throw new RuntimeException ("Sending email: error " + ex.toString());
    	}
    }

	private static StringBuilder createCommaSeparatedList(final Set<String> emails) {
		StringBuilder emailsListCommaSeparated = new StringBuilder();
        boolean first = true;
        for (String email : emails) {
        	if (!first) {
        		emailsListCommaSeparated.append(", ");
        	} else {
        		first = false;
        	}
        	emailsListCommaSeparated.append(email);
        }
		return emailsListCommaSeparated;
	}
    
}
