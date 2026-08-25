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
      "1. Login Success: AccessToken in JSON Body and HttpOnly estate_refresh_token Cookie")
  void step1_loginSuccessWithHttpOnlyCookie() throws Exception {
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

    assertNotNull(accessToken);
    assertNotNull(refreshCookie);
    assertTrue(refreshCookie.isHttpOnly());
    assertEquals("/api/v1/auth/refresh", refreshCookie.getPath());
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
  @DisplayName("3. Token Rotation: Refresh Token via Cookie generates new token pair")
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

    assertNotNull(rotatedAccessToken);
    assertNotNull(rotatedRefreshCookie);
    assertNotEquals(refreshCookie.getValue(), rotatedRefreshCookie.getValue());
  }

  @Test
  @Order(4)
  @DisplayName(
      "4. Reuse Attack Detection: Attempting to reuse old Refresh Token revokes all sessions")
  void step4_reuseDetectionRevokesAllSessions() throws Exception {
    // Kẻ gian cố tình gửi lại refreshCookie cũ đã bị rotate ở bước 3
    mockMvc
        .perform(
            post("/v1/auth/refresh").cookie(refreshCookie).contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isUnauthorized());
  }

  @Test
  @Order(5)
  @DisplayName("5. Logout: Blacklist Access Token in Redis and clear HttpOnly Cookie (Max-Age=0)")
  void step5_logoutAndClearCookie() throws Exception {
    MvcResult result =
        mockMvc
            .perform(
                post("/v1/auth/logout")
                    .header("Authorization", "Bearer " + rotatedAccessToken)
                    .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(header().exists(HttpHeaders.SET_COOKIE))
            .andReturn();

    Cookie clearedCookie = result.getResponse().getCookie("estate_refresh_token");
    assertNotNull(clearedCookie);
    assertEquals(0, clearedCookie.getMaxAge());
  }

  @Test
  @Order(6)
  @DisplayName(
      "6. Blacklist Enforcement: Accessing Protected API with Logged-Out Token must return 401")
  void step6_loggedOutTokenMustBeBlocked() throws Exception {
    mockMvc
        .perform(
            get("/v1/auth/me")
                .header("Authorization", "Bearer " + rotatedAccessToken)
                .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isUnauthorized());
  }
}
