package com.project.estate.modules.payment.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.project.estate.modules.identity.entity.User;
import com.project.estate.modules.payment.dto.request.InitiatePaymentRequest;
import com.project.estate.modules.payment.dto.request.PaymentWebhookRequest;
import com.project.estate.modules.payment.dto.response.PaymentResponse;
import com.project.estate.modules.payment.entity.Payment;
import com.project.estate.modules.payment.enums.PaymentMethod;
import com.project.estate.modules.payment.enums.PaymentStatus;
import com.project.estate.modules.payment.event.DepositPaidEvent;
import com.project.estate.modules.payment.mapper.PaymentMapper;
import com.project.estate.modules.payment.repository.PaymentRepository;
import com.project.estate.modules.property.entity.Property;
import com.project.estate.modules.property.enums.PropertyStatus;
import com.project.estate.modules.reservation.entity.Reservation;
import com.project.estate.modules.reservation.enums.ReservationStatus;
import com.project.estate.modules.reservation.repository.ReservationRepository;
import com.project.estate.modules.reservation.service.ReservationService;
import com.project.estate.shared.exception.AppException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

  @Mock private PaymentRepository paymentRepository;
  @Mock private ReservationRepository reservationRepository;
  @Mock private PaymentMapper paymentMapper;
  @Mock private ReservationService reservationService;
  @Mock private ApplicationEventPublisher eventPublisher;

  @InjectMocks private PaymentService paymentService;

  private User customer;
  private Property property;
  private Reservation activeReservation;

  @BeforeEach
  void setUp() {
    customer = User.builder().id("user-001").email("customer@example.com").build();

    property =
        Property.builder()
            .id("prop-001")
            .title("Luxury Villa")
            .status(PropertyStatus.RESERVED)
            .build();

    activeReservation =
        Reservation.builder()
            .id("res-001")
            .user(customer)
            .property(property)
            .depositAmount(BigDecimal.valueOf(50000000))
            .status(ReservationStatus.ACTIVE)
            .expiresAt(LocalDateTime.now().plusMinutes(15))
            .build();
  }

  @Test
  @DisplayName("Should initiate payment successfully for ACTIVE reservation")
  void initiatePayment_Success() {
    InitiatePaymentRequest request =
        InitiatePaymentRequest.builder()
            .reservationId("res-001")
            .paymentMethod(PaymentMethod.BANK_TRANSFER)
            .amount(BigDecimal.valueOf(50000000))
            .build();

    Payment savedPayment =
        Payment.builder()
            .id("pay-001")
            .reservation(activeReservation)
            .amount(request.amount())
            .paymentMethod(PaymentMethod.BANK_TRANSFER)
            .status(PaymentStatus.PENDING)
            .idempotencyKey("PAY-RES-res-001-12345678")
            .build();

    PaymentResponse expectedResponse =
        PaymentResponse.builder()
            .id("pay-001")
            .reservationId("res-001")
            .amount(request.amount())
            .paymentMethod(PaymentMethod.BANK_TRANSFER)
            .status(PaymentStatus.PENDING)
            .build();

    when(reservationRepository.findById("res-001")).thenReturn(Optional.of(activeReservation));
    when(paymentRepository.save(any(Payment.class))).thenReturn(savedPayment);
    when(paymentMapper.toResponse(any(Payment.class))).thenReturn(expectedResponse);

    PaymentResponse response = paymentService.initiatePayment(request);

    assertNotNull(response);
    assertEquals("pay-001", response.id());
    assertEquals(PaymentStatus.PENDING, response.status());
    assertNotNull(response.checkoutUrl());
    verify(paymentRepository, times(1)).save(any(Payment.class));
  }

  @Test
  @DisplayName(
      "Should process payment webhook, update payment to SUCCESS, trigger payDeposit workflow, and publish event")
  void processWebhook_Success() {
    PaymentWebhookRequest webhookRequest =
        PaymentWebhookRequest.builder()
            .reservationId("res-001")
            .amount(BigDecimal.valueOf(50000000))
            .paymentMethod(PaymentMethod.VNPAY)
            .transactionRef("VNPAY-TRANS-999")
            .idempotencyKey("IDEM-KEY-001")
            .build();

    PaymentResponse expectedResponse =
        PaymentResponse.builder()
            .id("pay-001")
            .reservationId("res-001")
            .amount(webhookRequest.amount())
            .status(PaymentStatus.SUCCESS)
            .transactionRef("VNPAY-TRANS-999")
            .build();

    when(paymentRepository.findByIdempotencyKey("IDEM-KEY-001")).thenReturn(Optional.empty());
    when(reservationRepository.findById("res-001")).thenReturn(Optional.of(activeReservation));
    when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));
    when(paymentMapper.toResponse(any(Payment.class))).thenReturn(expectedResponse);

    PaymentResponse response = paymentService.processWebhook(webhookRequest);

    assertNotNull(response);
    assertEquals(PaymentStatus.SUCCESS, response.status());

    verify(paymentRepository, times(1)).save(any(Payment.class));
    verify(reservationService, times(1)).payDeposit("res-001");
    verify(eventPublisher, times(1)).publishEvent(any(DepositPaidEvent.class));
  }

  @Test
  @DisplayName("Should handle duplicate webhook gracefully (Idempotency protection)")
  void processWebhook_DuplicateIdempotencyKey_ReturnsExistingWithoutReprocessing() {
    PaymentWebhookRequest webhookRequest =
        PaymentWebhookRequest.builder()
            .reservationId("res-001")
            .amount(BigDecimal.valueOf(50000000))
            .paymentMethod(PaymentMethod.VNPAY)
            .transactionRef("VNPAY-TRANS-999")
            .idempotencyKey("IDEM-KEY-DUPLICATE")
            .build();

    Payment existingSuccessPayment =
        Payment.builder()
            .id("pay-001")
            .reservation(activeReservation)
            .amount(webhookRequest.amount())
            .paymentMethod(PaymentMethod.VNPAY)
            .status(PaymentStatus.SUCCESS)
            .idempotencyKey("IDEM-KEY-DUPLICATE")
            .build();

    PaymentResponse expectedResponse =
        PaymentResponse.builder()
            .id("pay-001")
            .reservationId("res-001")
            .status(PaymentStatus.SUCCESS)
            .build();

    when(paymentRepository.findByIdempotencyKey("IDEM-KEY-DUPLICATE"))
        .thenReturn(Optional.of(existingSuccessPayment));
    when(paymentMapper.toResponse(existingSuccessPayment)).thenReturn(expectedResponse);

    PaymentResponse response = paymentService.processWebhook(webhookRequest);

    assertNotNull(response);
    assertEquals(PaymentStatus.SUCCESS, response.status());
    verify(reservationService, never()).payDeposit(anyString());
    verify(eventPublisher, never()).publishEvent(any());
  }

  @Test
  @DisplayName("Should throw exception when initiating payment for non-existent reservation")
  void initiatePayment_ReservationNotFound_ThrowsException() {
    InitiatePaymentRequest request =
        InitiatePaymentRequest.builder()
            .reservationId("non-existent")
            .paymentMethod(PaymentMethod.BANK_TRANSFER)
            .build();

    when(reservationRepository.findById("non-existent")).thenReturn(Optional.empty());

    assertThrows(AppException.class, () -> paymentService.initiatePayment(request));
  }
}
