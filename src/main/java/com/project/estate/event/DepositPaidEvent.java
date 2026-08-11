package com.project.estate.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.math.BigDecimal;

@Getter
public class DepositPaidEvent extends ApplicationEvent {

    private final String reservationId;
    private final String userEmail;
    private final String propertyTitle;
    private final BigDecimal amount;
    private final String transactionRef;

    public DepositPaidEvent(Object source, String reservationId, String userEmail, String propertyTitle, BigDecimal amount, String transactionRef) {
        super(source);
        this.reservationId = reservationId;
        this.userEmail = userEmail;
        this.propertyTitle = propertyTitle;
        this.amount = amount;
        this.transactionRef = transactionRef;
    }
}
