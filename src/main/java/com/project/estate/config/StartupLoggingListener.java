package com.project.estate.config;

import java.net.InetAddress;
import java.net.UnknownHostException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class StartupLoggingListener implements ApplicationListener<ApplicationReadyEvent> {

  private final Environment environment;

  @Value("${server.port:8080}")
  private String serverPort;

  @Value("${server.servlet.context-path:/api}")
  private String contextPath;

  public StartupLoggingListener(Environment environment) {
    this.environment = environment;
  }

  @Override
  public void onApplicationEvent(ApplicationReadyEvent event) {
    String protocol = "http";
    String hostAddress = "localhost";
    try {
      hostAddress = InetAddress.getLocalHost().getHostAddress();
    } catch (UnknownHostException e) {
      log.warn("The host name could not be determined, using `localhost` as fallback");
    }

    String activeProfiles = String.join(", ", environment.getActiveProfiles());
    if (activeProfiles.isBlank()) {
      activeProfiles = "default";
    }

    String dbUrl = environment.getProperty("spring.datasource.url", "N/A");
    String redisHost = environment.getProperty("spring.data.redis.host", "localhost");
    String redisPort = environment.getProperty("spring.data.redis.port", "6379");
    String rabbitHost = environment.getProperty("spring.rabbitmq.host", "localhost");
    String rabbitPort = environment.getProperty("spring.rabbitmq.port", "5672");

    log.info(
        """

        -----------------------------------------------------------------------------------------
        \t🚀 Application 'Estate Backend Service' is running successfully!
        \t🔑 Active Profile(s)  : [{}]
        \t🌐 Local Access        : {}://localhost:{}{}
        \t📡 External Access     : {}://{}:{}{}
        \t📚 Swagger UI (Docs)   : {}://localhost:{}{}/swagger-ui/index.html
        \t📊 Prometheus Metrics  : {}://localhost:{}{}/actuator/prometheus
        \t🗄️ PostgreSQL Database : {}
        \t⚡ Redis In-Memory     : {}:{}
        \t🐰 RabbitMQ Broker     : {}:{}
        -----------------------------------------------------------------------------------------
        """,
        activeProfiles,
        protocol,
        serverPort,
        contextPath,
        protocol,
        hostAddress,
        serverPort,
        contextPath,
        protocol,
        serverPort,
        contextPath,
        protocol,
        serverPort,
        contextPath,
        dbUrl,
        redisHost,
        redisPort,
        rabbitHost,
        rabbitPort);
  }
}
