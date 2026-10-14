package com.project.estate.modules.reservation.entity;

import com.project.estate.modules.identity.entity.User;
import com.project.estate.modules.property.entity.Property;
import com.project.estate.modules.reservation.enums.ReservationStatus;
import com.project.estate.shared.entity.AbstractAuditEntity;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "reservations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Reservation extends AbstractAuditEntity {

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "property_id", nullable = false)
  private Property property;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private ReservationStatus status;

  @Column(precision = 15, scale = 2)
  private java.math.BigDecimal depositAmount;

  @Column(nullable = false)
  private LocalDateTime expiresAt;
}
