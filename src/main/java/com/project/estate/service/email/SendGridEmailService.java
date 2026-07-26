package com.project.estate.service.email;

import com.project.estate.enums.ErrorCode;
import com.project.estate.exception.AppException;
import com.sendgrid.Method;
import com.sendgrid.Request;
import com.sendgrid.Response;
import com.sendgrid.SendGrid;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Email;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
@RequiredArgsConstructor
@Slf4j
public class SendGridEmailService implements MailService {

    private final SendGrid sendGrid;

    @Value("${spring.sendgrid.from-email}")
    private String fromEmail;

    @Override
    public void send(String to, String subject, String body) {
        Email from = new Email(fromEmail);
        Email toEmail = new Email(to);
        Content content= new Content("text/plain", body);

        Mail mail = new Mail(from, subject, toEmail, content);
        Request request = new Request();
        try {
            request.setMethod(Method.POST);
            request.setEndpoint("mail/send");
            request.setBody(mail.build());

            Response response = sendGrid.api(request);
            if (response.getStatusCode() == 202) {
                log.info("Email sent successfully to {}", to);
            } else {
                log.error("Failed to send email to {}. Status code: {}, Body: {}", to, response.getStatusCode(), response.getBody());
                throw new AppException(ErrorCode.EMAIL_SENDING_FAILED);
            }

        } catch (IOException e) {
            log.error("Error sending email to {}: {}", to, e.getMessage());
            throw new AppException(ErrorCode.EMAIL_SENDING_FAILED);
        }

    }

    @Override
    public void sendConfirmLink(String to, String secretKey) {

    }

}
