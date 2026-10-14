package com.project.estate.modules.reservation.mapper;

import com.project.estate.modules.identity.mapper.UserMapper;
import com.project.estate.modules.property.mapper.PropertyMapper;
import com.project.estate.modules.reservation.dto.response.ReservationResponse;
import com.project.estate.modules.reservation.entity.Reservation;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(
    componentModel = "spring",
    uses = {UserMapper.class, PropertyMapper.class})
public interface ReservationMapper {

  @Mapping(source = "user", target = "customer")
  ReservationResponse toResponse(Reservation reservation);
}
