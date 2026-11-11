package com.project.estate.dto.response;

import java.math.BigDecimal;
import lombok.Builder;

@Builder
public record AdminDashboardStatsResponse(
    long totalProperties,
    long availableProperties,
    long activeReservations,
    long depositPaidReservations,
    long completedReservations,
    BigDecimal totalDepositRevenue) {}
