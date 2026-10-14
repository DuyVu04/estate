package com.project.estate.modules.payment.mapper;

import com.project.estate.modules.payment.dto.response.PaymentResponse;
import com.project.estate.modules.payment.entity.Payment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface PaymentMapper {

  @Mapping(source = "reservation.id", target = "reservationId")
  PaymentResponse toResponse(Payment payment);
}
