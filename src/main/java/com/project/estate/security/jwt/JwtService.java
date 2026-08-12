package com.project.estate.security.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.security.Key;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

  @Value("${jwt.secret}")
  private String secret;

  @Value("${jwt.expiration}")
  private Long expiration;

  public String generateToken(UserDetails userDetails) {
    Map<String, Object> claims = new HashMap<>();
    List<String> roles =
        userDetails.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList();
    claims.put("roles", roles);
    return createToken(claims, userDetails.getUsername());
  }

  private String createToken(Map<String, Object> claims, String subject) {
    Instant now = Instant.now();
    Instant expirationTime = now.plusMillis(expiration);

    return Jwts.builder()
        .claims(claims)
        .subject(subject)
        .issuedAt(Date.from(now))
        .expiration(Date.from(expirationTime))
        .signWith(getSigningKey())
        .compact();
  }

  private Key getSigningKey() {
    byte[] keyBytes = Base64.getDecoder().decode(secret);
    return Keys.hmacShaKeyFor(keyBytes);
  }

  public String extractUsername(String token) {
    return extractClaims(token, Claims::getSubject);
  }

  @SuppressWarnings("unchecked")
  public List<String> extractRoles(String token) {
    return extractClaims(token, claims -> claims.get("roles", List.class));
  }

  public <T> T extractClaims(String token, Function<Claims, T> claimsResolver) {
    final Claims claims = extractAllClaims(token);
    return claimsResolver.apply(claims);
  }

  private Claims extractAllClaims(String token) {
    return Jwts.parser()
        .verifyWith((SecretKey) getSigningKey())
        .build()
        .parseSignedClaims(token)
        .getPayload();
  }

  public Boolean validateToken(String token, UserDetails userDetails) {
    final String username = extractUsername(token);
    return (username.equals(userDetails.getUsername()) && !isTokenExpired(token));
  }

  public Boolean isTokenValid(String token) {
    return !isTokenExpired(token);
  }

  private Boolean isTokenExpired(String token) {
    Date expirationAt = extractClaims(token, Claims::getExpiration);
    return expirationAt.before(Date.from(Instant.now()));
  }
}
