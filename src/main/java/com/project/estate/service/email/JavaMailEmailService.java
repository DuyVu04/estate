package com.project.estate.service.email;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Slf4j
public class JavaMailEmailService implements MailService {

    @Value("${spring.mail.from}")
    private String fromEmail;

    @Value("${spring.mail.password}")
    private String fromPassword;

    private final JavaMailSender mailSender;

    private final SpringTemplateEngine templateEngine;

    public String send(String recipients, String subject, String content, MultipartFile[] files) throws MessagingException, UnsupportedEncodingException {
        log.info("Sending email to {}", recipients);

        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        helper.setFrom(fromEmail,"Duy Vũ");
        if(recipients.contains(",")){
            helper.setTo(InternetAddress.parse(recipients));
        }else {
            helper.setTo(recipients);
        }

        if(files != null ){
            for (MultipartFile file : files) {
                helper.addAttachment(Objects.requireNonNull(file.getOriginalFilename()),file);
            }
        }
        helper.setSubject(subject);
        helper.setText(content, true);
        mailSender.send(message);
        log.info("Email sent successfully, recipients: {}", recipients);
        return "sent";
    }


    @Override
    public void send(String to, String subject, String html) {

    }

    @Override
    public void sendConfirmLink(String to, String token) throws MessagingException, UnsupportedEncodingException {
        log.info("Sending confirmation link to {}", to);

        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED, StandardCharsets.UTF_8.name());

        Context context = new Context();
        // Tạo confirmation link thật từ id và secretKey
        String confirmationLink =
                "http://localhost:8080/api/v1/auth/verify?token=" + token;
        Map<String,Object> properties = new HashMap<>();
        properties.put("confirmationLink", confirmationLink);
        context.setVariables(properties);

        helper.setFrom(fromEmail, "Duy Vũ");
        helper.setTo(to);
        helper.setSubject("Verify your account");

        String htmlContent = templateEngine.process("confirm-email.html", context);
        helper.setText(htmlContent, true);
        mailSender.send(message);
        log.info("Confirmation link sent successfully to {}", to);

    }
}
