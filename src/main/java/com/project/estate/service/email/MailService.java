package com.project.estate.service.email;

import org.springframework.web.multipart.MultipartFile;

/**
 * Clean low-level email sender contract (Interface Segregation & Liskov Substitution).
 * Focuses purely on transport delivery (Text, HTML, Attachments) without domain template coupling.
 */
public interface MailService {

    void sendText(String to, String subject, String content);

    void sendHtml(String to, String subject, String htmlContent);

    void sendWithAttachments(String to, String subject, String content, MultipartFile[] files);
}
