package com.project.estate.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RedissonConfig {

  @Value("${spring.data.redis.host:localhost}")
  private String redisHost;

  @Value("${spring.data.redis.port:6379}")
  private int redisPort;

  @Value("${spring.data.redis.password:}")
  private String redisPassword;

  @Bean(destroyMethod = "shutdown")
  public RedissonClient redissonClient() {
    Config config = new Config();
    String address = String.format("redis://%s:%d", redisHost, redisPort);

    var singleServerConfig =
        config
            .useSingleServer()
            .setAddress(address)
            .setConnectTimeout(5000)
            .setTimeout(3000)
            .setRetryAttempts(3);

    if (redisPassword != null && !redisPassword.isBlank()) {
      singleServerConfig.setPassword(redisPassword);
    }

    return Redisson.create(config);
  }
}
