package com.project.estate.service;

import com.project.estate.dto.request.InitiatePaymentRequest;
import com.project.estate.dto.request.PaymentWebhookRequest;
import com.project.estate.dto.response.PaymentResponse;
import com.project.estate.entity.Payment;
import com.project.estate.entity.Property;
import com.project.estate.entity.Reservation;
import com.project.estate.entity.User;
import com.project.estate.enums.PaymentMethod;
import com.project.estate.enums.PaymentStatus;
import com.project.estate.enums.ReservationStatus;
import com.project.estate.event.DepositPaidEvent;
import com.project.estate.mapper.PaymentMapper;
import com.project.estate.repository.PaymentRepository;
import com.project.estate.repository.ReservationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private ReservationService reservationService;

    @Mock
    private PaymentMapper paymentMapper;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private com.project.estate.messaging.producer.EmailProducer emailProducer;

    @InjectMocks
    private PaymentService paymentService;

    private Reservation activeReservation;

    @BeforeEach
    void setUp() {
        User user = User.builder().id("user-01").email("user@example.com").build();
        Property property = Property.builder().id("prop-01").title("Luxury Villa").build();
        activeReservation = Reservation.builder()
                .id("res-001")
                .user(user)
                .property(property)
                .status(ReservationStatus.ACTIVE)
                .expiresAt(LocalDateTime.now().plusMinutes(15))
                .build();
    }

    @Test
    @DisplayName("Should successfully initiate payment for active reservation")
    void initiatePayment_Success() {
        InitiatePaymentRequest request = InitiatePaymentRequest.builder()
                .reservationId("res-001")
                .paymentMethod(PaymentMethod.BANK_TRANSFER)
                .amount(BigDecimal.valueOf(50000000))
                .build();

        Payment savedPayment = Payment.builder()
                .id("pay-001")
                .reservation(activeReservation)
                .amount(request.getAmount())
                .paymentMethod(PaymentMethod.BANK_TRANSFER)
                .status(PaymentStatus.PENDING)
                .idempotencyKey("PAY-RES-res-001-12345678")
                .build();

        PaymentResponse expectedResponse = PaymentResponse.builder()
                .id("pay-001")
                .reservationId("res-001")
                .amount(request.getAmount())
                .paymentMethod(PaymentMethod.BANK_TRANSFER)
                .status(PaymentStatus.PENDING)
                .build();

        when(reservationRepository.findById("res-001")).thenReturn(Optional.of(activeReservation));
        when(paymentRepository.save(any(Payment.class))).thenReturn(savedPayment);
        when(paymentMapper.toResponse(any(Payment.class))).thenReturn(expectedResponse);

        PaymentResponse response = paymentService.initiatePayment(request);

        assertNotNull(response);
        assertEquals("pay-001", response.getId());
        assertEquals(PaymentStatus.PENDING, response.getStatus());
        assertNotNull(response.getCheckoutUrl());
        verify(paymentRepository, times(1)).save(any(Payment.class));
    }

    @Test
    @DisplayName("Should process payment webhook, update payment to SUCCESS, trigger payDeposit workflow, and publish event")
    void processWebhook_Success() {
        PaymentWebhookRequest webhookRequest = PaymentWebhookRequest.builder()
                .reservationId("res-001")
                .amount(BigDecimal.valueOf(50000000))
                .paymentMethod(PaymentMethod.VNPAY)
                .transactionRef("VNPAY-TRANS-999")
                .idempotencyKey("IDEM-KEY-001")
                .build();

        PaymentResponse expectedResponse = PaymentResponse.builder()
                .id("pay-001")
                .reservationId("res-001")
                .amount(webhookRequest.getAmount())
                .status(PaymentStatus.SUCCESS)
                .transactionRef("VNPAY-TRANS-999")
                .build();

        when(paymentRepository.findByIdempotencyKey("IDEM-KEY-001")).thenReturn(Optional.empty());
        when(reservationRepository.findById("res-001")).thenReturn(Optional.of(activeReservation));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));
        when(paymentMapper.toResponse(any(Payment.class))).thenReturn(expectedResponse);

        PaymentResponse response = paymentService.processWebhook(webhookRequest);

        assertNotNull(response);
        assertEquals(PaymentStatus.SUCCESS, response.getStatus());

        verify(paymentRepository, times(1)).save(any(Payment.class));
        verify(reservationService, times(1)).payDeposit("res-001");
        verify(eventPublisher, times(1)).publishEvent(any(DepositPaidEvent.class));
    }

    @Test
    @DisplayName("Should handle duplicate webhook gracefully (Idempotency protection)")
    void processWebhook_DuplicateIdempotencyKey_ReturnsExistingWithoutReprocessing() {
        PaymentWebhookRequest webhookRequest = PaymentWebhookRequest.builder()
                .reservationId("res-001")
                .amount(BigDecimal.valueOf(50000000))
                .paymentMethod(PaymentMethod.VNPAY)
                .transactionRef("VNPAY-TRANS-999")
                .idempotencyKey("IDEM-KEY-DUPLICATE")
                .build();

        Payment existingSuccessPayment = Payment.builder()
                .id("pay-001")
                .reservation(activeReservation)
                .amount(webhookRequest.getAmount())
                .paymentMethod(PaymentMethod.VNPAY)
                .status(PaymentStatus.SUCCESS)
                .idempotencyKey("IDEM-KEY-DUPLICATE")
                .build();

        PaymentResponse expectedResponse = PaymentResponse.builder()
                .id("pay-001")
                .reservationId("res-001")
                .status(PaymentStatus.SUCCESS)
                .build();

        when(paymentRepository.findByIdempotencyKey("IDEM-KEY-DUPLICATE")).thenReturn(Optional.of(existingSuccessPayment));
        when(paymentMapper.toResponse(existingSuccessPayment)).thenReturn(expectedResponse);

        PaymentResponse response = paymentService.processWebhook(webhookRequest);

        assertNotNull(response);
        assertEquals(PaymentStatus.SUCCESS, response.getStatus());

        verify(paymentRepository, never()).save(any(Payment.class));
        verify(reservationService, never()).payDeposit(anyString());
        verify(eventPublisher, never()).publishEvent(any());
    }
}
