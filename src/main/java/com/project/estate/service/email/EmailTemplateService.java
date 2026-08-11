package com.project.estate.service.email;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * Responsible for rendering Thymeleaf HTML templates and delegating
 * transport delivery to the low-level MailService interface.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailTemplateService {

    private final MailService mailService;
    private final SpringTemplateEngine templateEngine;

    public void sendConfirmLink(String to, String token) {
        log.info("[TEMPLATE_SERVICE] Rendering confirm email template for {}", to);

        Context context = new Context();
        String confirmationLink = "http://localhost:8080/api/v1/auth/verify?token=" + token;
        Map<String, Object> properties = new HashMap<>();
        properties.put("confirmationLink", confirmationLink);
        context.setVariables(properties);

        String htmlContent = templateEngine.process("confirm-email.html", context);
        mailService.sendHtml(to, "Verify your account", htmlContent);
    }

    public void sendDepositPaidEmail(String to, String reservationId, String propertyTitle, BigDecimal amount, String transactionRef) {
        log.info("[TEMPLATE_SERVICE] Rendering deposit paid template for reservationId={}, email={}", reservationId, to);

        Context context = new Context();
        Map<String, Object> properties = new HashMap<>();
        properties.put("reservationId", reservationId);
        properties.put("propertyTitle", propertyTitle);
        properties.put("amount", amount != null ? amount : BigDecimal.ZERO);
        properties.put("transactionRef", transactionRef != null ? transactionRef : "N/A");
        context.setVariables(properties);

        String htmlContent = templateEngine.process("deposit-paid-email.html", context);
        mailService.sendHtml(to, "Xác nhận đặt cọc thành công - Dự án Estate", htmlContent);
    }
}
