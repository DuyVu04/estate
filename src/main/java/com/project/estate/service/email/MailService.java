package com.project.estate.service.email;

import jakarta.mail.MessagingException;

import java.io.UnsupportedEncodingException;

public interface MailService {
    void send(String to, String subject, String html);

    void sendConfirmLink(String to, String secretKey) throws MessagingException, UnsupportedEncodingException;
}
