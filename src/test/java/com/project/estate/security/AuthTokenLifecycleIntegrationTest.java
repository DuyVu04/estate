package com.project.estate.security;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.estate.dto.request.LoginRequest;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AuthTokenLifecycleIntegrationTest {

  @Autowired private WebApplicationContext webApplicationContext;
  private final ObjectMapper objectMapper = new ObjectMapper();

  private MockMvc mockMvc;

  private static String accessToken;
  private static Cookie refreshCookie;
  private static Cookie sessionHintCookie;
  private static String rotatedAccessToken;
  private static Cookie rotatedRefreshCookie;

  @BeforeEach
  void setUp() {
    this.mockMvc =
        MockMvcBuilders.webAppContextSetup(webApplicationContext)
            .apply(SecurityMockMvcConfigurers.springSecurity())
            .build();
  }

  @Test
  @Order(1)
  @DisplayName(
      "1. Login Success: AccessToken in JSON Body, estate_refresh_token (Path=/api/v1/auth/refresh) and estate_session_hint (Path=/) Cookies")
  void step1_loginSuccessWithCookies() throws Exception {
    LoginRequest loginRequest = new LoginRequest("admin", "admin");

    MvcResult result =
        mockMvc
            .perform(
                post("/v1/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(loginRequest)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.result.accessToken").exists())
            .andExpect(header().exists(HttpHeaders.SET_COOKIE))
            .andReturn();

    JsonNode jsonNode = objectMapper.readTree(result.getResponse().getContentAsString());
    accessToken = jsonNode.path("result").path("accessToken").asText();
    refreshCookie = result.getResponse().getCookie("estate_refresh_token");
    sessionHintCookie = result.getResponse().getCookie("estate_session_hint");

    assertNotNull(accessToken);
    assertNotNull(refreshCookie);
    assertNotNull(sessionHintCookie);

    // Refresh token: HttpOnly=true, Path=/api/v1/auth/refresh
    assertTrue(refreshCookie.isHttpOnly());
    assertEquals("/api/v1/auth/refresh", refreshCookie.getPath());

    // Session hint token: HttpOnly=false, Path=/, Value="1"
    assertFalse(sessionHintCookie.isHttpOnly());
    assertEquals("/", sessionHintCookie.getPath());
    assertEquals("1", sessionHintCookie.getValue());
  }

  @Test
  @Order(2)
  @DisplayName("2. Access Protected API: Verify /v1/auth/me with In-Memory Access Token")
  void step2_accessProtectedApi() throws Exception {
    mockMvc
        .perform(
            get("/v1/auth/me")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.result.username").value("admin"));
  }

  @Test
  @Order(3)
  @DisplayName(
      "3. Token Rotation: Refresh Token via Cookie generates new token pair and session hint")
  void step3_refreshTokenRotationViaCookie() throws Exception {
    MvcResult result =
        mockMvc
            .perform(
                post("/v1/auth/refresh")
                    .cookie(refreshCookie)
                    .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.result.accessToken").exists())
            .andExpect(header().exists(HttpHeaders.SET_COOKIE))
            .andReturn();

    JsonNode jsonNode = objectMapper.readTree(result.getResponse().getContentAsString());
    rotatedAccessToken = jsonNode.path("result").path("accessToken").asText();
    rotatedRefreshCookie = result.getResponse().getCookie("estate_refresh_token");
    Cookie rotatedHintCookie = result.getResponse().getCookie("estate_session_hint");

    assertNotNull(rotatedAccessToken);
    assertNotNull(rotatedRefreshCookie);
    assertNotNull(rotatedHintCookie);
    assertNotEquals(refreshCookie.getValue(), rotatedRefreshCookie.getValue());
    assertEquals("/", rotatedHintCookie.getPath());
    assertEquals("1", rotatedHintCookie.getValue());
  }

  @Test
  @Order(4)
  @DisplayName(
      "4a. Reuse Attack Detection: Attempting to reuse old Refresh Token returns 401 and revokes all sessions")
  void step4a_reuseDetectionReturns401() throws Exception {
    // Kẻ gian cố tình gửi lại refreshCookie cũ (đã xoay vòng ở step 3)
    mockMvc
        .perform(
            post("/v1/auth/refresh").cookie(refreshCookie).contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value(1109)); // REFRESH_TOKEN_REVOKED
  }

  @Test
  @Order(5)
  @DisplayName(
      "4b. Full Session Invalidation Proof: After reuse attack, even the active rotated token is revoked")
  void step4b_activeRotatedTokenMustAlsoBeRevokedAfterReuse() throws Exception {
    // Chứng minh: Sau khi vụ tấn công tái sử dụng ở step 4a bị phát hiện,
    // toàn bộ session của user (kể cả rotatedRefreshCookie mới nhất) ĐÃ BỊ HỦY HOÀN TOÀN TRONG
    // REDIS!
    mockMvc
        .perform(
            post("/v1/auth/refresh")
                .cookie(rotatedRefreshCookie)
                .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isUnauthorized());
  }

  @Test
  @Order(6)
  @DisplayName(
      "5. Fresh Login & Logout: Blacklist Access Token in Redis and clear both Cookies (Max-Age=0)")
  void step5_freshLoginAndLogout() throws Exception {
    // Đăng nhập lại phiên mới
    LoginRequest loginRequest = new LoginRequest("admin", "admin");
    MvcResult loginResult =
        mockMvc
            .perform(
                post("/v1/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(loginRequest)))
            .andExpect(status().isOk())
            .andReturn();

    JsonNode jsonNode = objectMapper.readTree(loginResult.getResponse().getContentAsString());
    String freshAccessToken = jsonNode.path("result").path("accessToken").asText();

    // Logout
    MvcResult logoutResult =
        mockMvc
            .perform(
                post("/v1/auth/logout")
                    .header("Authorization", "Bearer " + freshAccessToken)
                    .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(header().exists(HttpHeaders.SET_COOKIE))
            .andReturn();

    Cookie clearedRefreshCookie = logoutResult.getResponse().getCookie("estate_refresh_token");
    Cookie clearedHintCookie = logoutResult.getResponse().getCookie("estate_session_hint");

    assertNotNull(clearedRefreshCookie);
    assertNotNull(clearedHintCookie);
    assertEquals(0, clearedRefreshCookie.getMaxAge());
    assertEquals(0, clearedHintCookie.getMaxAge());
    assertEquals("/", clearedHintCookie.getPath());

    // Thử dùng lại access token vừa logout -> Phải bị 401 do dính Blacklist
    mockMvc
        .perform(
            get("/v1/auth/me")
                .header("Authorization", "Bearer " + freshAccessToken)
                .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isUnauthorized());
  }
}
