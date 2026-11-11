package com.project.estate.service.email;

import com.project.estate.enums.ErrorCode;
import com.project.estate.exception.AppException;
import com.sendgrid.Method;
import com.sendgrid.Request;
import com.sendgrid.Response;
import com.sendgrid.SendGrid;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Attachments;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Email;
import java.io.IOException;
import java.util.Base64;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * Secondary low-level SendGrid MailService implementation using SendGrid REST API. Fully implements
 * all transport methods (Text, HTML, Attachments) .
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SendGridEmailService implements MailService {

  private final SendGrid sendGrid;

  @Value("${spring.sendgrid.from-email:vuduy250524@gmail.com}")
  private String fromEmail;

  @Override
  public void sendText(String to, String subject, String content) {
    log.info("[SENDGRID] Sending plain text email to {}", to);
    sendSendGridMail(to, subject, new Content("text/plain", content), null);
  }

  @Override
  public void sendHtml(String to, String subject, String htmlContent) {
    log.info("[SENDGRID] Sending HTML email to {}", to);
    sendSendGridMail(to, subject, new Content("text/html", htmlContent), null);
  }

  @Override
  public void sendWithAttachments(
      String to, String subject, String content, MultipartFile[] files) {
    log.info("[SENDGRID] Sending email with attachments to {}", to);
    sendSendGridMail(to, subject, new Content("text/html", content), files);
  }

  private void sendSendGridMail(String to, String subject, Content content, MultipartFile[] files) {
    Email from = new Email(fromEmail);
    Email toEmail = new Email(to);
    Mail mail = new Mail(from, subject, toEmail, content);

    if (files != null) {
      for (MultipartFile file : files) {
        if (file != null && !file.isEmpty()) {
          try {
            Attachments attachments = new Attachments();
            attachments.setContent(Base64.getEncoder().encodeToString(file.getBytes()));
            attachments.setType(file.getContentType());
            attachments.setFilename(file.getOriginalFilename());
            attachments.setDisposition("attachment");
            mail.addAttachments(attachments);
          } catch (IOException e) {
            log.error(
                "[SENDGRID] Failed to attach file {}: {}",
                file.getOriginalFilename(),
                e.getMessage());
          }
        }
      }
    }

    Request request = new Request();
    try {
      request.setMethod(Method.POST);
      request.setEndpoint("mail/send");
      request.setBody(mail.build());

      Response response = sendGrid.api(request);
      if (response.getStatusCode() == 202) {
        log.info("[SENDGRID] Email sent successfully to {}", to);
      } else {
        log.error(
            "[SENDGRID] Failed to send email to {}. Status: {}, Body: {}",
            to,
            response.getStatusCode(),
            response.getBody());
        throw new AppException(ErrorCode.EMAIL_SENDING_FAILED);
      }
    } catch (IOException e) {
      log.error("[SENDGRID] Error sending email to {}: {}", to, e.getMessage(), e);
      throw new AppException(ErrorCode.EMAIL_SENDING_FAILED);
    }
  }
}
