package com.project.estate.dto.response;

import com.project.estate.enums.HealthStatus;
import java.time.Instant;
import java.util.List;
import lombok.Builder;

@Builder
public record SystemHealthResponse(
    HealthStatus overallStatus,
    String statusMessage,
    Instant collectedAt,
    BasicStats basicStats,
    List<MemoryPoolStats> memoryPools,
    ThreadStats threads,
    GcStats gc,
    DatabasePoolStats databasePool,
    HttpStats httpStats,
    LogStats logStats,
    InfrastructureHealth infrastructure) {

  public record LabeledValue<T>(String label, T value) {}

  public record StatusMetric<T>(String label, T value, String displayValue, HealthStatus status) {}

  public record MemoryMetric(
      String label,
      long usedBytes,
      long maxBytes,
      double percent,
      String displayValue,
      HealthStatus status) {}

  public record PercentMetric(
      String label, double percent, String displayValue, HealthStatus status) {}

  @Builder
  public record BasicStats(
      StatusMetric<Long> uptime,
      LabeledValue<Instant> startTime,
      MemoryMetric heapUsage,
      MemoryMetric nonHeapUsage,
      PercentMetric systemCpuUsage,
      PercentMetric processCpuUsage,
      LabeledValue<Integer> cpuCoreCount) {}

  @Builder
  public record MemoryPoolStats(
      String name,
      String label,
      String area,
      long usedBytes,
      long committedBytes,
      long maxBytes,
      String displayUsed,
      String displayCommitted,
      String displayMax,
      Double usagePercent) {}

  @Builder
  public record ThreadStats(
      StatusMetric<Integer> liveThreads,
      LabeledValue<Integer> daemonThreads,
      LabeledValue<Integer> peakThreads,
      LabeledValue<Long> classesLoaded,
      LabeledValue<String> directBufferUsed,
      LabeledValue<String> mappedBufferUsed) {}

  @Builder
  public record GcStats(
      LabeledValue<Long> pauseCount,
      StatusMetric<Double> pauseTotalTimeMs,
      LabeledValue<Double> maxPauseTimeMs) {}

  @Builder
  public record DatabasePoolStats(
      LabeledValue<Integer> totalConnections,
      StatusMetric<Integer> activeConnections,
      LabeledValue<Integer> idleConnections,
      StatusMetric<Integer> pendingThreads,
      StatusMetric<Long> connectionTimeouts,
      StatusMetric<Double> avgCreationTimeMs,
      LabeledValue<Double> avgUsageTimeMs,
      StatusMetric<Double> avgAcquireTimeMs) {}

  @Builder
  public record EndpointStats(
      String uri, String method, long count, double avgDurationMs, double maxDurationMs) {}

  @Builder
  public record HttpStats(
      LabeledValue<Long> totalRequests,
      LabeledValue<Long> http2xxCount,
      StatusMetric<Long> http4xxCount,
      StatusMetric<Long> http5xxCount,
      StatusMetric<Double> avgResponseTimeMs,
      StatusMetric<Double> maxResponseTimeMs,
      List<EndpointStats> topEndpoints) {}

  @Builder
  public record LogStats(
      LabeledValue<Long> infoCount,
      StatusMetric<Long> warnCount,
      StatusMetric<Long> errorCount,
      LabeledValue<Long> debugCount,
      LabeledValue<Long> traceCount) {}

  @Builder
  public record ComponentHealth(String name, String label, HealthStatus status, String details) {}

  @Builder
  public record InfrastructureHealth(
      ComponentHealth database,
      ComponentHealth redis,
      ComponentHealth rabbitmq,
      ComponentHealth mail,
      ComponentHealth diskSpace) {}
}
