package com.matthey.pmm.metal.rentals.document;

import org.springframework.stereotype.Component;

import javax.mail.Message;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import java.io.File;
import java.util.Properties;

@Component
public class EmailSender {

    private static final String SMTP_HOST = System.getenv("EMAIL_SMTP_HOST");
    private static final String FROM_ADDRESS = "metal.rentals@matthey.com";

    public void send(String subject, String content, String attachmentPath, String emailAddress) {
        try {
            Properties properties = System.getProperties();
            properties.setProperty("mail.smtp.host", SMTP_HOST);

            var message = new MimeMessage(Session.getDefaultInstance(properties));
            message.setFrom(new InternetAddress(FROM_ADDRESS));
            message.addRecipients(Message.RecipientType.TO, InternetAddress.parse(emailAddress));
            message.setSubject(subject);

            var mimeBodyPart = new MimeBodyPart();
            mimeBodyPart.setContent(content, "text/plain");


            var multipart = new MimeMultipart();
            multipart.addBodyPart(mimeBodyPart);
            if (attachmentPath != null) {
                MimeBodyPart attachmentBodyPart = new MimeBodyPart();
                attachmentBodyPart.attachFile(new File(attachmentPath));
                multipart.addBodyPart(attachmentBodyPart);
            }
            message.setContent(multipart);
            Transport.send(message);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
