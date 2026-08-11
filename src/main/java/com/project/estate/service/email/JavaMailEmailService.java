package com.project.estate.service.email;

import com.project.estate.enums.ErrorCode;
import com.project.estate.exception.AppException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * Primary low-level SMTP MailService implementation using Spring JavaMailSender.
 * Fully implements all transport methods (Text, HTML, Attachments) .
 */
@Primary
@Service
@RequiredArgsConstructor
@Slf4j
public class JavaMailEmailService implements MailService {

    @Value("${spring.mail.from:vuduy250524@gmail.com}")
    private String fromEmail;

    private final JavaMailSender mailSender;

    @Override
    public void sendText(String to, String subject, String content) {
        log.info("[JAVA_MAIL] Sending plain text email to {}", to);
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, StandardCharsets.UTF_8.name());
            setRecipientsAndSender(helper, to, subject);
            helper.setText(content, false);
            mailSender.send(message);
            log.info("[JAVA_MAIL] Plain text email sent successfully to {}", to);
        } catch (Exception e) {
            log.error("[JAVA_MAIL] Failed to send plain text email to {}: {}", to, e.getMessage(), e);
            throw new AppException(ErrorCode.EMAIL_SENDING_FAILED);
        }
    }

    @Override
    public void sendHtml(String to, String subject, String htmlContent) {
        log.info("[JAVA_MAIL] Sending HTML email to {}", to);
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED, StandardCharsets.UTF_8.name());
            setRecipientsAndSender(helper, to, subject);
            helper.setText(htmlContent, true);
            mailSender.send(message);
            log.info("[JAVA_MAIL] HTML email sent successfully to {}", to);
        } catch (Exception e) {
            log.error("[JAVA_MAIL] Failed to send HTML email to {}: {}", to, e.getMessage(), e);
            throw new AppException(ErrorCode.EMAIL_SENDING_FAILED);
        }
    }

    @Override
    public void sendWithAttachments(String to, String subject, String content, MultipartFile[] files) {
        log.info("[JAVA_MAIL] Sending email with attachments to {}", to);
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());
            setRecipientsAndSender(helper, to, subject);
            helper.setText(content, true);

            if (files != null) {
                for (MultipartFile file : files) {
                    if (file != null && !file.isEmpty()) {
                        helper.addAttachment(Objects.requireNonNull(file.getOriginalFilename()), file);
                    }
                }
            }

            mailSender.send(message);
            log.info("[JAVA_MAIL] Email with attachments sent successfully to {}", to);
        } catch (Exception e) {
            log.error("[JAVA_MAIL] Failed to send email with attachments to {}: {}", to, e.getMessage(), e);
            throw new AppException(ErrorCode.EMAIL_SENDING_FAILED);
        }
    }

    private void setRecipientsAndSender(MimeMessageHelper helper, String to, String subject) throws Exception {
        helper.setFrom(fromEmail, "Duy Vũ");
        if (to.contains(",")) {
            helper.setTo(InternetAddress.parse(to));
        } else {
            helper.setTo(to);
        }
        helper.setSubject(subject);
    }
}
