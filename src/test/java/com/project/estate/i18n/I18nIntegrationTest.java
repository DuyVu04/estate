package com.project.estate.i18n;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.project.estate.common.response.ApiResponse;
import com.project.estate.config.LocaleConfig;
import com.project.estate.enums.ErrorCode;
import com.project.estate.exception.AppException;
import com.project.estate.exception.GlobalExceptionHandler;
import java.util.Locale;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver;

class I18nIntegrationTest {

  private MessageSource messageSource;
  private GlobalExceptionHandler exceptionHandler;
  private LocaleConfig localeConfig;

  @BeforeEach
  void setUp() {
    localeConfig = new LocaleConfig();
    messageSource = localeConfig.messageSource();
    exceptionHandler = new GlobalExceptionHandler(messageSource);
  }

  @AfterEach
  void tearDown() {
    LocaleContextHolder.resetLocaleContext();
  }

  @Test
  @DisplayName("Should configure default locale as Vietnamese in AcceptHeaderLocaleResolver")
  void shouldConfigureDefaultLocaleAsVietnamese() {
    LocaleResolver resolver = localeConfig.localeResolver();
    MockHttpServletRequest request = new MockHttpServletRequest();
    assertEquals(Locale.of("vi"), resolver.resolveLocale(request));

    if (resolver instanceof AcceptHeaderLocaleResolver acceptHeaderResolver) {
      assertTrue(acceptHeaderResolver.getSupportedLocales().contains(Locale.of("vi")));
      assertTrue(acceptHeaderResolver.getSupportedLocales().contains(Locale.ENGLISH));
    }
  }

  @Test
  @DisplayName("AcceptHeaderLocaleResolver should resolve English when Accept-Language is en")
  void shouldResolveEnglishFromAcceptHeader() {
    LocaleResolver resolver = localeConfig.localeResolver();
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader("Accept-Language", "en");
    assertEquals(Locale.ENGLISH, resolver.resolveLocale(request));
  }

  @Test
  @DisplayName("Should resolve Vietnamese messages for ErrorCodes when locale is vi")
  void shouldResolveVietnameseMessages() {
    LocaleContextHolder.setLocale(Locale.of("vi"));

    String userNotFoundMsg =
        messageSource.getMessage(ErrorCode.USER_NOT_FOUND.name(), null, Locale.of("vi"));
    assertEquals("Không tìm thấy người dùng", userNotFoundMsg);

    String propertyNotAvailableMsg =
        messageSource.getMessage(ErrorCode.PROPERTY_NOT_AVAILABLE.name(), null, Locale.of("vi"));
    assertEquals("Bất động sản hiện không khả dụng", propertyNotAvailableMsg);

    String successMsg = messageSource.getMessage(ErrorCode.SUCCESS.name(), null, Locale.of("vi"));
    assertEquals("Thành công", successMsg);
  }

  @Test
  @DisplayName("Should resolve English messages for ErrorCodes when locale is en")
  void shouldResolveEnglishMessages() {
    LocaleContextHolder.setLocale(Locale.ENGLISH);

    String userNotFoundMsg =
        messageSource.getMessage(ErrorCode.USER_NOT_FOUND.name(), null, Locale.ENGLISH);
    assertEquals("User not found", userNotFoundMsg);

    String propertyNotAvailableMsg =
        messageSource.getMessage(ErrorCode.PROPERTY_NOT_AVAILABLE.name(), null, Locale.ENGLISH);
    assertEquals("Property not available", propertyNotAvailableMsg);

    String successMsg = messageSource.getMessage(ErrorCode.SUCCESS.name(), null, Locale.ENGLISH);
    assertEquals("Success", successMsg);
  }

  @Test
  @DisplayName(
      "GlobalExceptionHandler should return localized message in Vietnamese when locale is vi")
  void exceptionHandlerShouldReturnVietnameseResponse() {
    LocaleContextHolder.setLocale(Locale.of("vi"));

    AppException ex = new AppException(ErrorCode.USER_NOT_FOUND);
    ResponseEntity<ApiResponse<Void>> response = exceptionHandler.handleAppException(ex);

    assertNotNull(response.getBody());
    assertEquals(1100, response.getBody().code());
    assertEquals("Không tìm thấy người dùng", response.getBody().message());
  }

  @Test
  @DisplayName(
      "GlobalExceptionHandler should return localized message in English when locale is en")
  void exceptionHandlerShouldReturnEnglishResponse() {
    LocaleContextHolder.setLocale(Locale.ENGLISH);

    AppException ex = new AppException(ErrorCode.USER_NOT_FOUND);
    ResponseEntity<ApiResponse<Void>> response = exceptionHandler.handleAppException(ex);

    assertNotNull(response.getBody());
    assertEquals(1100, response.getBody().code());
    assertEquals("User not found", response.getBody().message());
  }

  @Test
  @DisplayName("Should fallback to Vietnamese default messages when unsupported locale requested")
  void shouldFallbackToVietnameseWhenUnsupportedLocale() {
    Locale unsupportedLocale = Locale.of("fr");
    String msg =
        messageSource.getMessage(
            ErrorCode.PROPERTY_ALREADY_RESERVED.name(), null, unsupportedLocale);
    assertEquals("Bất động sản đã có người đặt cọc trước", msg);
  }
}
