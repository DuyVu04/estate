package com.project.estate.service;

import static org.junit.jupiter.api.Assertions.*;

import com.project.estate.dto.response.SystemHealthResponse;
import com.project.estate.enums.HealthStatus;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SystemHealthServiceTest {

  private MeterRegistry meterRegistry;
  private SystemHealthService systemHealthService;

  @BeforeEach
  void setUp() {
    meterRegistry = new SimpleMeterRegistry();
    systemHealthService = new SystemHealthService(meterRegistry, Optional.empty());
  }

  @Test
  @DisplayName("Should collect healthy basic stats when metrics are within safe thresholds")
  void testCollectHealth_HealthyMetrics() {
    // 1. Basic Stats
    meterRegistry.gauge("process.uptime", 3600.0);
    meterRegistry.gauge("process.start.time", 1700000000.0);
    meterRegistry.gauge("system.cpu.count", 8);

    AtomicLong heapUsed = new AtomicLong(100 * 1024 * 1024L); // 100MB
    AtomicLong heapMax = new AtomicLong(1024 * 1024 * 1024L); // 1GB
    Gauge.builder("jvm.memory.used", heapUsed, AtomicLong::get)
        .tag("area", "heap")
        .tag("id", "G1 Eden Space")
        .register(meterRegistry);
    Gauge.builder("jvm.memory.max", heapMax, AtomicLong::get)
        .tag("area", "heap")
        .tag("id", "G1 Eden Space")
        .register(meterRegistry);

    // 2. CPU
    meterRegistry.gauge("system.cpu.usage", 0.15); // 15%
    meterRegistry.gauge("process.cpu.usage", 0.05); // 5%

    // 3. Threads
    meterRegistry.gauge("jvm.threads.live", 50);
    meterRegistry.gauge("jvm.threads.daemon", 20);

    // 4. HikariCP
    AtomicInteger activeConn = new AtomicInteger(2);
    AtomicInteger totalConn = new AtomicInteger(10);
    Gauge.builder("hikaricp.connections.active", activeConn, AtomicInteger::get)
        .register(meterRegistry);
    Gauge.builder("hikaricp.connections", totalConn, AtomicInteger::get).register(meterRegistry);

    // 5. HTTP requests
    Timer timer =
        Timer.builder("http.server.requests")
            .tag("uri", "/v1/properties")
            .tag("method", "GET")
            .tag("status", "200")
            .register(meterRegistry);
    timer.record(50, TimeUnit.MILLISECONDS);
    timer.record(70, TimeUnit.MILLISECONDS);

    // 6. Logback events
    Counter.builder("logback.events").tag("level", "info").register(meterRegistry).increment(10);
    Counter.builder("logback.events").tag("level", "warn").register(meterRegistry).increment(2);
    Counter.builder("logback.events").tag("level", "error").register(meterRegistry).increment(0);

    // Act
    SystemHealthResponse response = systemHealthService.collectHealth();

    // Assert
    assertNotNull(response);
    assertEquals(HealthStatus.HEALTHY, response.overallStatus());
    assertNotNull(response.statusMessage());
    assertNotNull(response.collectedAt());

    // Basic Stats checks
    assertNotNull(response.basicStats());
    assertEquals(3600L, response.basicStats().uptime().value());
    assertEquals(HealthStatus.HEALTHY, response.basicStats().uptime().status());
    assertTrue(response.basicStats().uptime().displayValue().contains("1 giờ"));
    assertEquals(8, response.basicStats().cpuCoreCount().value());
    assertEquals(HealthStatus.HEALTHY, response.basicStats().heapUsage().status());

    // Memory Pools
    assertFalse(response.memoryPools().isEmpty());
    assertEquals("G1 Eden Space", response.memoryPools().get(0).name());
    assertEquals("Vùng nhớ Eden (Heap)", response.memoryPools().get(0).label());

    // Database pool
    assertNotNull(response.databasePool());
    assertEquals(2, response.databasePool().activeConnections().value());
    assertEquals("2 / 10", response.databasePool().activeConnections().displayValue());
    assertEquals(HealthStatus.HEALTHY, response.databasePool().activeConnections().status());

    // HTTP stats
    assertNotNull(response.httpStats());
    assertEquals(2, response.httpStats().totalRequests().value());
    assertEquals(2, response.httpStats().http2xxCount().value());
    assertEquals(0, response.httpStats().http5xxCount().value());
    assertFalse(response.httpStats().topEndpoints().isEmpty());
    assertEquals("/v1/properties", response.httpStats().topEndpoints().get(0).uri());

    // Log stats
    assertNotNull(response.logStats());
    assertEquals(10, response.logStats().infoCount().value());
    assertEquals(0, response.logStats().errorCount().value());
    assertEquals(HealthStatus.HEALTHY, response.logStats().errorCount().status());

    // Infrastructure fallback
    assertNotNull(response.infrastructure());
    assertEquals("PostgreSQL", response.infrastructure().database().name());
    assertEquals(HealthStatus.HEALTHY, response.infrastructure().database().status());
  }

  @Test
  @DisplayName("Should flag CRITICAL when high error logs or 5xx or heap overload detected")
  void testCollectHealth_CriticalStatus() {
    // Overload heap to 95%
    AtomicLong heapUsed = new AtomicLong(950 * 1024 * 1024L);
    AtomicLong heapMax = new AtomicLong(1000 * 1024 * 1024L);
    Gauge.builder("jvm.memory.used", heapUsed, AtomicLong::get)
        .tag("area", "heap")
        .tag("id", "G1 Old Gen")
        .register(meterRegistry);
    Gauge.builder("jvm.memory.max", heapMax, AtomicLong::get)
        .tag("area", "heap")
        .tag("id", "G1 Old Gen")
        .register(meterRegistry);

    // 5xx errors
    Timer timer =
        Timer.builder("http.server.requests")
            .tag("uri", "/v1/properties")
            .tag("method", "POST")
            .tag("status", "500")
            .register(meterRegistry);
    timer.record(200, TimeUnit.MILLISECONDS);

    SystemHealthResponse response = systemHealthService.collectHealth();

    assertNotNull(response);
    assertEquals(HealthStatus.CRITICAL, response.overallStatus());
    assertEquals(HealthStatus.CRITICAL, response.basicStats().heapUsage().status());
    assertEquals(HealthStatus.CRITICAL, response.httpStats().http5xxCount().status());
  }
}
