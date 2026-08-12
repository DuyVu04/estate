package com.project.estate.mapper;

import com.project.estate.dto.response.PaymentResponse;
import com.project.estate.entity.Payment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface PaymentMapper {

  @Mapping(source = "reservation.id", target = "reservationId")
  PaymentResponse toResponse(Payment payment);
}
