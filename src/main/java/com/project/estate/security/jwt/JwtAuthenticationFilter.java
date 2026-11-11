package com.project.estate.security.jwt;

import com.project.estate.service.redis.TokenBlacklistService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

  private final JwtService jwtService;
  private final UserDetailsService userDetailsService;
  private final TokenBlacklistService tokenBlacklistService;

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    String authHeader = request.getHeader("Authorization");

    if (authHeader != null && authHeader.startsWith("Bearer ")) {
      try {
        String jwt = authHeader.substring(7);

        // Kiểm tra xem token có nằm trong Redis Blacklist (đã logout) hay không
        if (tokenBlacklistService.isBlacklisted(jwt)) {
          log.warn("Access token has been revoked/blacklisted: {}", jwt);
          filterChain.doFilter(request, response);
          return;
        }

        String username = jwtService.extractUsername(jwt);

        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
          UserDetails userDetails = userDetailsService.loadUserByUsername(username);
          if (jwtService.validateToken(jwt, userDetails)) {
            List<String> rolesInClaims = jwtService.extractRoles(jwt);
            Collection<? extends GrantedAuthority> authorities =
                (rolesInClaims != null && !rolesInClaims.isEmpty())
                    ? rolesInClaims.stream().map(SimpleGrantedAuthority::new).toList()
                    : userDetails.getAuthorities();

            UsernamePasswordAuthenticationToken authenticationToken =
                new UsernamePasswordAuthenticationToken(userDetails, null, authorities);
            authenticationToken.setDetails(
                new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authenticationToken);
          }
        }
      } catch (Exception e) {
        log.warn("JWT authentication failed: {}", e.getMessage());
      }
    }

    filterChain.doFilter(request, response);
  }
}
