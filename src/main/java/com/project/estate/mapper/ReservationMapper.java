package com.project.estate.mapper;

import com.project.estate.dto.response.ReservationResponse;
import com.project.estate.entity.Reservation;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ReservationMapper {
  ReservationResponse toResponse(Reservation reservation);
}
