package com.project.estate.config;

import com.stripe.StripeClient;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration class for Stripe Payment Gateway. Initializes StripeClient bean and exposes
 * Stripe-specific properties.
 */
@Configuration
@Getter
public class StripeConfig {

  @Value("${stripe.api-key}")
  private String apiKey;

  @Value("${stripe.webhook-secret}")
  private String webhookSecret;

  @Value("${stripe.success-url}")
  private String successUrl;

  @Value("${stripe.cancel-url}")
  private String cancelUrl;

  @Bean
  public StripeClient stripeClient() {
    return new StripeClient(apiKey);
  }
}
