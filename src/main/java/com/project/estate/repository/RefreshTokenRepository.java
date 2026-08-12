package com.project.estate.repository;

import com.project.estate.entity.RefreshToken;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, String> {
  Optional<RefreshToken> findByToken(String token);

  void deleteByUser_Id(String userId);

  List<RefreshToken> findAllByUserIdAndRevokedFalse(String userId);
}
