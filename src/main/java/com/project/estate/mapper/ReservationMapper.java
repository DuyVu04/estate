package com.project.estate.mapper;

import com.project.estate.dto.response.ReservationResponse;
import com.project.estate.entity.Reservation;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(
    componentModel = "spring",
    uses = {UserMapper.class, PropertyMapper.class})
public interface ReservationMapper {

  @Mapping(source = "user", target = "customer")
  ReservationResponse toResponse(Reservation reservation);
}
