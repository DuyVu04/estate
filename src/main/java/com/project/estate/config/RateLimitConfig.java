package com.project.estate.config;

import io.github.bucket4j.distributed.proxy.ProxyManager;
import io.github.bucket4j.redis.lettuce.cas.LettuceBasedProxyManager;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.codec.ByteArrayCodec;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.codec.StringCodec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RateLimitConfig {
  @Value("${spring.data.redis.host:localhost}")
  private String redisHost;

  @Value("${spring.data.redis.port:6380}")
  private int redisPort;

  @Bean
  @org.springframework.context.annotation.Lazy
  public RedisClient bucket4jRedisClient() {
    RedisURI redisURI = RedisURI.builder().withHost(redisHost).withPort(redisPort).build();
    return RedisClient.create(redisURI);
  }

  @Bean
  @org.springframework.context.annotation.Lazy
  public ProxyManager<String> bucket4jProxyManager(
      @org.springframework.context.annotation.Lazy RedisClient bucket4jRedisClient) {
    StatefulRedisConnection<String, byte[]> connection =
        bucket4jRedisClient.connect(RedisCodec.of(StringCodec.UTF8, ByteArrayCodec.INSTANCE));

    return LettuceBasedProxyManager.builderFor(connection).build();
  }
}
