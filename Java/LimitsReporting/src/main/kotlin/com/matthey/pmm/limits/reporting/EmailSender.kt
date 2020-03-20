package com.matthey.pmm.limits.reporting

import org.slf4j.LoggerFactory
import java.util.*
import javax.mail.Message
import javax.mail.Multipart
import javax.mail.Session
import javax.mail.Transport
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeBodyPart
import javax.mail.internet.MimeMessage
import javax.mail.internet.MimeMultipart

class EmailSender() {

    private val logger = LoggerFactory.getLogger(javaClass)
    private val host = System.getenv("AB_EMAIL_SMTP_HOST")
    private val fromAddress = "eod.limits.reporting@matthey.com"

    init {
        logger.info("email host: $host; 'From' address for emails: $fromAddress")
    }

    fun send(subject: String, html: String, emails: Set<String>) {
        val properties: Properties = System.getProperties()
        properties.setProperty("mail.smtp.host", host)
        val message = MimeMessage(Session.getDefaultInstance(properties))
        message.setFrom(InternetAddress(fromAddress))
        message.addRecipients(Message.RecipientType.TO, InternetAddress.parse(emails.joinToString()))
        message.subject = subject
        val mimeBodyPart = MimeBodyPart()
        mimeBodyPart.setContent(html, "text/html")
        val multipart: Multipart = MimeMultipart()
        multipart.addBodyPart(mimeBodyPart)
        message.setContent(multipart)
        Transport.send(message)
    }
}