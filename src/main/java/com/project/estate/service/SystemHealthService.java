package com.project.estate.service;

import com.project.estate.dto.response.SystemHealthResponse;
import com.project.estate.dto.response.SystemHealthResponse.*;
import com.project.estate.enums.HealthStatus;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.health.actuate.endpoint.CompositeHealthDescriptor;
import org.springframework.boot.health.actuate.endpoint.HealthDescriptor;
import org.springframework.boot.health.actuate.endpoint.HealthEndpoint;
import org.springframework.boot.health.actuate.endpoint.IndicatedHealthDescriptor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class SystemHealthService {

  private final MeterRegistry meterRegistry;
  private final Optional<HealthEndpoint> healthEndpoint;

  public SystemHealthResponse collectHealth() {
    BasicStats basicStats = collectBasicStats();
    List<MemoryPoolStats> memoryPools = collectMemoryPools();
    ThreadStats threads = collectThreadStats();
    GcStats gc = collectGcStats();
    DatabasePoolStats dbPool = collectDatabasePoolStats();
    HttpStats httpStats = collectHttpStats();
    LogStats logStats = collectLogStats();
    InfrastructureHealth infra = collectInfrastructureHealth();

    HealthStatus overallStatus =
        evaluateOverallStatus(basicStats, threads, dbPool, httpStats, logStats, infra);
    String statusMessage = buildStatusMessage(overallStatus, dbPool, httpStats, logStats);

    return SystemHealthResponse.builder()
        .overallStatus(overallStatus)
        .statusMessage(statusMessage)
        .collectedAt(Instant.now())
        .basicStats(basicStats)
        .memoryPools(memoryPools)
        .threads(threads)
        .gc(gc)
        .databasePool(dbPool)
        .httpStats(httpStats)
        .logStats(logStats)
        .infrastructure(infra)
        .build();
  }

  // ==========================================
  // 1. Basic Stats
  // ==========================================
  private BasicStats collectBasicStats() {
    double uptimeSec = getGaugeValue("process.uptime");
    long uptimeLong = (long) uptimeSec;
    HealthStatus uptimeStatus =
        uptimeLong < 300
            ? HealthStatus.WARNING
            : HealthStatus.HEALTHY; // Vừa khởi động dưới 5p thì WARNING nhẹ

    double startTimeSec = getGaugeValue("process.start.time");
    Instant startTime =
        startTimeSec > 0
            ? Instant.ofEpochMilli((long) (startTimeSec * 1000))
            : Instant.now().minusSeconds(uptimeLong);

    // Heap
    long heapUsed = sumGauges("jvm.memory.used", "area", "heap");
    long heapMax = sumGauges("jvm.memory.max", "area", "heap");
    if (heapMax <= 0) {
      heapMax = sumGauges("jvm.memory.committed", "area", "heap");
    }
    double heapPercent = heapMax > 0 ? (double) heapUsed / heapMax * 100.0 : 0.0;
    HealthStatus heapStatus =
        heapPercent > 85.0
            ? HealthStatus.CRITICAL
            : (heapPercent > 70.0 ? HealthStatus.WARNING : HealthStatus.HEALTHY);

    MemoryMetric heapMetric =
        new MemoryMetric(
            "Bộ nhớ Heap đã dùng",
            heapUsed,
            heapMax,
            roundTwoDecimals(heapPercent),
            String.format(
                "%s / %s (%.1f%%)", formatBytes(heapUsed), formatBytes(heapMax), heapPercent),
            heapStatus);

    // Non-Heap: Chuẩn SRE đặt mốc an toàn 1 GiB cho Non-Heap tổng hợp (Metaspace + CodeCache +
    // Compressed Class)
    long nonHeapUsed = sumGauges("jvm.memory.used", "area", "nonheap");
    long nonHeapThreshold = 1024L * 1024 * 1024; // 1 GiB
    double nonHeapPercent = (double) nonHeapUsed / nonHeapThreshold * 100.0;
    HealthStatus nonHeapStatus =
        nonHeapUsed > 800L * 1024 * 1024
            ? HealthStatus.CRITICAL
            : (nonHeapUsed > 500L * 1024 * 1024 ? HealthStatus.WARNING : HealthStatus.HEALTHY);

    MemoryMetric nonHeapMetric =
        new MemoryMetric(
            "Bộ nhớ Non-Heap đã dùng",
            nonHeapUsed,
            nonHeapThreshold,
            roundTwoDecimals(nonHeapPercent),
            String.format(
                "%s / %s (%.1f%%)",
                formatBytes(nonHeapUsed), formatBytes(nonHeapThreshold), nonHeapPercent),
            nonHeapStatus);

    // CPU
    double sysCpu = getGaugeValue("system.cpu.usage") * 100.0;
    double procCpu = getGaugeValue("process.cpu.usage") * 100.0;
    int cpuCores = (int) getGaugeValue("system.cpu.count");

    HealthStatus sysCpuStatus =
        sysCpu > 85.0
            ? HealthStatus.CRITICAL
            : (sysCpu > 65.0 ? HealthStatus.WARNING : HealthStatus.HEALTHY);
    HealthStatus procCpuStatus =
        procCpu > 80.0
            ? HealthStatus.CRITICAL
            : (procCpu > 50.0 ? HealthStatus.WARNING : HealthStatus.HEALTHY);

    return BasicStats.builder()
        .uptime(
            new StatusMetric<>(
                "Thời gian hoạt động", uptimeLong, formatDuration(uptimeLong), uptimeStatus))
        .startTime(new LabeledValue<>("Thời điểm khởi động", startTime))
        .heapUsage(heapMetric)
        .nonHeapUsage(nonHeapMetric)
        .systemCpuUsage(
            new PercentMetric(
                "CPU hệ thống",
                roundTwoDecimals(sysCpu),
                String.format(Locale.US, "%.1f%%", sysCpu),
                sysCpuStatus))
        .processCpuUsage(
            new PercentMetric(
                "CPU ứng dụng Java",
                roundTwoDecimals(procCpu),
                String.format(Locale.US, "%.2f%%", procCpu),
                procCpuStatus))
        .cpuCoreCount(new LabeledValue<>("Số lõi CPU", cpuCores))
        .build();
  }

  // ==========================================
  // 2. JVM Memory Pools (Eden, Old Gen, etc.)
  // ==========================================
  private List<MemoryPoolStats> collectMemoryPools() {
    List<MemoryPoolStats> pools = new ArrayList<>();
    var usedGauges = meterRegistry.find("jvm.memory.used").gauges();
    long heapMax = sumGauges("jvm.memory.max", "area", "heap");

    for (var gauge : usedGauges) {
      String poolId = gauge.getId().getTag("id");
      String area = gauge.getId().getTag("area");
      if (poolId == null) continue;

      long used = (long) gauge.value();
      long committed = (long) getGaugeValue("jvm.memory.committed", "id", poolId);
      long max = (long) getGaugeValue("jvm.memory.max", "id", poolId);

      Double percent = null;
      String displayMaxStr;

      if (max > 0) {
        percent = roundTwoDecimals((double) used / max * 100.0);
        displayMaxStr = formatBytes(max);
      } else if ("heap".equalsIgnoreCase(area)) {
        // Trong G1 GC, Eden và Survivor dùng chung tổng Heap dung lượng tối đa là heapMax
        long effectiveMax = heapMax > 0 ? heapMax : committed;
        percent = effectiveMax > 0 ? roundTwoDecimals((double) used / effectiveMax * 100.0) : 0.0;
        displayMaxStr =
            heapMax > 0 ? "Chia sẻ từ Heap (" + formatBytes(heapMax) + ")" : "Không giới hạn";
      } else if ("Metaspace".equalsIgnoreCase(poolId)) {
        // Metaspace không giới hạn, chuẩn an toàn ứng dụng Java thường dưới 512 MiB
        long metaspaceThreshold = 512L * 1024 * 1024;
        percent = roundTwoDecimals((double) used / metaspaceThreshold * 100.0);
        displayMaxStr = "Không giới hạn (Ngưỡng 512 MiB)";
      } else if (committed > 0) {
        percent = roundTwoDecimals((double) used / committed * 100.0);
        displayMaxStr = "Không giới hạn";
      } else {
        displayMaxStr = "Không giới hạn";
      }

      pools.add(
          MemoryPoolStats.builder()
              .name(poolId)
              .label(translatePoolName(poolId, area))
              .area(area != null ? area : "unknown")
              .usedBytes(used)
              .committedBytes(committed)
              .maxBytes(max)
              .displayUsed(formatBytes(used))
              .displayCommitted(formatBytes(committed))
              .displayMax(displayMaxStr)
              .usagePercent(percent)
              .build());
    }

    pools.sort(Comparator.comparing(MemoryPoolStats::area).thenComparing(MemoryPoolStats::name));
    return pools;
  }

  // ==========================================
  // 3. Thread & Buffer Stats
  // ==========================================
  private ThreadStats collectThreadStats() {
    int live = (int) getGaugeValue("jvm.threads.live");
    int daemon = (int) getGaugeValue("jvm.threads.daemon");
    int peak = (int) getGaugeValue("jvm.threads.peak");
    long classes = (long) getGaugeValue("jvm.classes.loaded");

    long directBuffer = (long) getGaugeValue("jvm.buffer.memory.used", "id", "direct");
    long mappedBuffer = (long) getGaugeValue("jvm.buffer.memory.used", "id", "mapped");

    HealthStatus liveStatus =
        live > 500
            ? HealthStatus.CRITICAL
            : (live > 200 ? HealthStatus.WARNING : HealthStatus.HEALTHY);

    return ThreadStats.builder()
        .liveThreads(
            new StatusMetric<>("Số thread đang chạy", live, String.valueOf(live), liveStatus))
        .daemonThreads(new LabeledValue<>("Số thread Daemon", daemon))
        .peakThreads(new LabeledValue<>("Số thread đỉnh điểm", peak))
        .classesLoaded(new LabeledValue<>("Số class đã nạp", classes))
        .directBufferUsed(new LabeledValue<>("Direct Buffer đã dùng", formatBytes(directBuffer)))
        .mappedBufferUsed(new LabeledValue<>("Mapped Buffer đã dùng", formatBytes(mappedBuffer)))
        .build();
  }

  // ==========================================
  // 4. Garbage Collection Stats
  // ==========================================
  private GcStats collectGcStats() {
    var pauseTimers = meterRegistry.find("jvm.gc.pause").timers();
    long count = 0;
    double totalTimeMs = 0;
    double maxTimeMs = 0;

    for (Timer t : pauseTimers) {
      count += t.count();
      totalTimeMs += t.totalTime(TimeUnit.MILLISECONDS);
      double max = t.max(TimeUnit.MILLISECONDS);
      if (max > maxTimeMs) {
        maxTimeMs = max;
      }
    }

    HealthStatus gcStatus =
        totalTimeMs > 5000
            ? HealthStatus.CRITICAL
            : (totalTimeMs > 2000 ? HealthStatus.WARNING : HealthStatus.HEALTHY);

    return GcStats.builder()
        .pauseCount(new LabeledValue<>("Số lần GC tạm dừng", count))
        .pauseTotalTimeMs(
            new StatusMetric<>(
                "Tổng thời gian GC dừng app",
                roundTwoDecimals(totalTimeMs),
                formatMs(totalTimeMs),
                gcStatus))
        .maxPauseTimeMs(new LabeledValue<>("Thời gian dừng lâu nhất", roundTwoDecimals(maxTimeMs)))
        .build();
  }

  // ==========================================
  // 5. HikariCP Database Pool Stats
  // ==========================================
  private DatabasePoolStats collectDatabasePoolStats() {
    int total = (int) getGaugeValue("hikaricp.connections");
    int active = (int) getGaugeValue("hikaricp.connections.active");
    int idle = (int) getGaugeValue("hikaricp.connections.idle");
    int pending = (int) getGaugeValue("hikaricp.connections.pending");
    long timeouts = (long) getCounterValue("hikaricp.connections.timeout");

    double creationTimeMs = getTimerMean("hikaricp.connections.creation");
    double usageTimeMs = getTimerMean("hikaricp.connections.usage");
    double acquireTimeMs = getTimerMean("hikaricp.connections.acquire");

    HealthStatus activeStatus =
        total > 0 && ((double) active / total > 0.85)
            ? HealthStatus.CRITICAL
            : (total > 0 && ((double) active / total > 0.6)
                ? HealthStatus.WARNING
                : HealthStatus.HEALTHY);

    HealthStatus pendingStatus =
        pending > 5
            ? HealthStatus.CRITICAL
            : (pending > 0 ? HealthStatus.WARNING : HealthStatus.HEALTHY);

    HealthStatus timeoutStatus =
        timeouts > 5
            ? HealthStatus.CRITICAL
            : (timeouts > 0 ? HealthStatus.WARNING : HealthStatus.HEALTHY);

    HealthStatus acquireStatus =
        acquireTimeMs > 50.0
            ? HealthStatus.CRITICAL
            : (acquireTimeMs > 10.0 ? HealthStatus.WARNING : HealthStatus.HEALTHY);

    HealthStatus creationStatus =
        creationTimeMs > 500.0 ? HealthStatus.WARNING : HealthStatus.HEALTHY;

    return DatabasePoolStats.builder()
        .totalConnections(new LabeledValue<>("Tổng kết nối DB", total))
        .activeConnections(
            new StatusMetric<>(
                "Kết nối DB đang dùng",
                active,
                String.format("%d / %d", active, total),
                activeStatus))
        .idleConnections(new LabeledValue<>("Kết nối DB rảnh", idle))
        .pendingThreads(
            new StatusMetric<>(
                "Thread đang chờ kết nối", pending, String.valueOf(pending), pendingStatus))
        .connectionTimeouts(
            new StatusMetric<>(
                "Số lần lỗi timeout kết nối", timeouts, String.valueOf(timeouts), timeoutStatus))
        .avgCreationTimeMs(
            new StatusMetric<>(
                "Thời gian tạo kết nối TB",
                roundTwoDecimals(creationTimeMs),
                formatMs(creationTimeMs),
                creationStatus))
        .avgUsageTimeMs(
            new LabeledValue<>("Thời gian chạy query TB", roundTwoDecimals(usageTimeMs)))
        .avgAcquireTimeMs(
            new StatusMetric<>(
                "Thời gian lấy kết nối từ pool TB",
                roundTwoDecimals(acquireTimeMs),
                formatMs(acquireTimeMs),
                acquireStatus))
        .build();
  }

  // ==========================================
  // 6. HTTP & Request Stats
  // ==========================================
  private HttpStats collectHttpStats() {
    var httpTimers = meterRegistry.find("http.server.requests").timers();

    long totalRequests = 0;
    long http2xx = 0;
    long http4xx = 0;
    long http5xx = 0;
    double totalDurationMs = 0;
    double maxDurationMs = 0;

    // Aggregate by URI + Method for Top 10
    Map<String, EndpointAggregator> endpointMap = new HashMap<>();

    for (Timer t : httpTimers) {
      long count = t.count();
      if (count <= 0) continue;

      totalRequests += count;
      double durationMs = t.totalTime(TimeUnit.MILLISECONDS);
      totalDurationMs += durationMs;

      double max = t.max(TimeUnit.MILLISECONDS);
      if (max > maxDurationMs) {
        maxDurationMs = max;
      }

      String statusStr = t.getId().getTag("status");
      if (statusStr != null) {
        if (statusStr.startsWith("2")) http2xx += count;
        else if (statusStr.startsWith("4")) http4xx += count;
        else if (statusStr.startsWith("5")) http5xx += count;
      }

      String uri = t.getId().getTag("uri");
      String method = t.getId().getTag("method");
      if (uri != null && method != null) {
        String key = method + " " + uri;
        endpointMap
            .computeIfAbsent(key, k -> new EndpointAggregator(uri, method))
            .accumulate(count, durationMs, max);
      }
    }

    double avgDurationMs = totalRequests > 0 ? totalDurationMs / totalRequests : 0.0;

    HealthStatus h4xxStatus = http4xx > 50 ? HealthStatus.WARNING : HealthStatus.HEALTHY;
    HealthStatus h5xxStatus = http5xx > 0 ? HealthStatus.CRITICAL : HealthStatus.HEALTHY;

    HealthStatus avgStatus =
        avgDurationMs > 1000
            ? HealthStatus.CRITICAL
            : (avgDurationMs > 300 ? HealthStatus.WARNING : HealthStatus.HEALTHY);

    HealthStatus maxStatus =
        maxDurationMs > 5000
            ? HealthStatus.CRITICAL
            : (maxDurationMs > 2000 ? HealthStatus.WARNING : HealthStatus.HEALTHY);

    List<EndpointStats> topEndpoints =
        endpointMap.values().stream()
            .sorted(Comparator.comparingLong((EndpointAggregator e) -> e.count).reversed())
            .limit(10)
            .map(
                e ->
                    EndpointStats.builder()
                        .uri(e.uri)
                        .method(e.method)
                        .count(e.count)
                        .avgDurationMs(
                            roundTwoDecimals(e.count > 0 ? e.totalDurationMs / e.count : 0.0))
                        .maxDurationMs(roundTwoDecimals(e.maxDurationMs))
                        .build())
            .toList();

    return HttpStats.builder()
        .totalRequests(new LabeledValue<>("Tổng số request đã xử lý", totalRequests))
        .http2xxCount(new LabeledValue<>("Phản hồi thành công (2xx)", http2xx))
        .http4xxCount(
            new StatusMetric<>(
                "Lỗi phía Client (4xx)", http4xx, String.valueOf(http4xx), h4xxStatus))
        .http5xxCount(
            new StatusMetric<>(
                "Lỗi hệ thống Server (5xx)", http5xx, String.valueOf(http5xx), h5xxStatus))
        .avgResponseTimeMs(
            new StatusMetric<>(
                "Thời gian phản hồi TB",
                roundTwoDecimals(avgDurationMs),
                formatMs(avgDurationMs),
                avgStatus))
        .maxResponseTimeMs(
            new StatusMetric<>(
                "Thời gian phản hồi lâu nhất",
                roundTwoDecimals(maxDurationMs),
                formatMs(maxDurationMs),
                maxStatus))
        .topEndpoints(topEndpoints)
        .build();
  }

  // ==========================================
  // 7. Logback Log Stats
  // ==========================================
  private LogStats collectLogStats() {
    long info = (long) getCounterValue("logback.events", "level", "info");
    long warn = (long) getCounterValue("logback.events", "level", "warn");
    long error = (long) getCounterValue("logback.events", "level", "error");
    long debug = (long) getCounterValue("logback.events", "level", "debug");
    long trace = (long) getCounterValue("logback.events", "level", "trace");

    HealthStatus warnStatus = warn > 20 ? HealthStatus.WARNING : HealthStatus.HEALTHY;
    HealthStatus errorStatus =
        error > 5
            ? HealthStatus.CRITICAL
            : (error > 0 ? HealthStatus.WARNING : HealthStatus.HEALTHY);

    return LogStats.builder()
        .infoCount(new LabeledValue<>("Số log INFO", info))
        .warnCount(
            new StatusMetric<>("Số log Cảnh báo (WARN)", warn, String.valueOf(warn), warnStatus))
        .errorCount(
            new StatusMetric<>("Số log Lỗi (ERROR)", error, String.valueOf(error), errorStatus))
        .debugCount(new LabeledValue<>("Số log DEBUG", debug))
        .traceCount(new LabeledValue<>("Số log TRACE", trace))
        .build();
  }

  // ==========================================
  // 8. Infrastructure Health (Actuator /health)
  // ==========================================
  private InfrastructureHealth collectInfrastructureHealth() {
    if (healthEndpoint.isEmpty()) {
      return fallbackInfrastructureHealth();
    }

    try {
      HealthDescriptor rootHealth = healthEndpoint.get().health();
      Map<String, HealthDescriptor> components = Collections.emptyMap();
      if (rootHealth instanceof CompositeHealthDescriptor compositeHealth) {
        components = compositeHealth.getComponents();
      }

      ComponentHealth db =
          parseComponent(components.get("db"), "PostgreSQL", "Cơ sở dữ liệu chính");
      ComponentHealth redis =
          parseComponent(components.get("redis"), "Redis", "Bộ nhớ đệm & Rate limit");
      ComponentHealth rabbit =
          parseComponent(components.get("rabbit"), "RabbitMQ", "Hàng đợi tin nhắn");
      ComponentHealth mail =
          parseComponent(components.get("mail"), "Mail Service", "Dịch vụ gửi email");
      ComponentHealth disk = parseDiskSpaceComponent(components.get("diskSpace"));

      return InfrastructureHealth.builder()
          .database(db)
          .redis(redis)
          .rabbitmq(rabbit)
          .mail(mail)
          .diskSpace(disk)
          .build();
    } catch (Exception e) {
      log.warn("[SYSTEM_HEALTH] Failed to read HealthEndpoint: {}", e.getMessage());
      return fallbackInfrastructureHealth();
    }
  }

  private ComponentHealth parseComponent(
      HealthDescriptor comp, String defaultName, String defaultLabel) {
    if (comp == null) {
      return ComponentHealth.builder()
          .name(defaultName)
          .label(defaultLabel)
          .status(HealthStatus.WARNING)
          .details("Không có thông tin từ Actuator")
          .build();
    }

    String statusStr = comp.getStatus().getCode();
    HealthStatus status =
        "UP".equalsIgnoreCase(statusStr)
            ? HealthStatus.HEALTHY
            : ("DOWN".equalsIgnoreCase(statusStr) ? HealthStatus.CRITICAL : HealthStatus.WARNING);

    String detailText = statusStr + " - Đang hoạt động bình thường";
    if (comp instanceof IndicatedHealthDescriptor ind && ind.getDetails() != null) {
      Object ver = ind.getDetails().get("version");
      Object db = ind.getDetails().get("database");
      Object loc = ind.getDetails().get("location");
      if (db != null) {
        detailText = String.format("%s - %s", statusStr, db);
      } else if (ver != null) {
        detailText = String.format("%s - Phiên bản %s", statusStr, ver);
      } else if (loc != null) {
        detailText = String.format("%s - %s", statusStr, loc);
      }
    }

    return ComponentHealth.builder()
        .name(defaultName)
        .label(defaultLabel)
        .status(status)
        .details(detailText)
        .build();
  }

  private ComponentHealth parseDiskSpaceComponent(HealthDescriptor comp) {
    if (comp == null) {
      return ComponentHealth.builder()
          .name("Disk Space")
          .label("Dung lượng ổ cứng")
          .status(HealthStatus.HEALTHY)
          .details("Dung lượng bình thường")
          .build();
    }

    String statusStr = comp.getStatus().getCode();
    HealthStatus status =
        "UP".equalsIgnoreCase(statusStr) ? HealthStatus.HEALTHY : HealthStatus.CRITICAL;

    String detailText = statusStr + " - Còn đủ dung lượng lưu trữ an toàn";
    if (comp instanceof IndicatedHealthDescriptor ind && ind.getDetails() != null) {
      Object freeObj = ind.getDetails().get("free");
      Object totalObj = ind.getDetails().get("total");
      if (freeObj instanceof Number free && totalObj instanceof Number total) {
        detailText =
            String.format(
                "%s - Còn trống %s / %s (%.1f%%)",
                statusStr,
                formatBytes(free.longValue()),
                formatBytes(total.longValue()),
                total.doubleValue() > 0 ? (free.doubleValue() / total.doubleValue() * 100.0) : 0.0);
      }
    }

    return ComponentHealth.builder()
        .name("Disk Space")
        .label("Dung lượng ổ cứng")
        .status(status)
        .details(detailText)
        .build();
  }

  private InfrastructureHealth fallbackInfrastructureHealth() {
    return InfrastructureHealth.builder()
        .database(
            ComponentHealth.builder()
                .name("PostgreSQL")
                .label("Cơ sở dữ liệu chính")
                .status(HealthStatus.HEALTHY)
                .details("Kết nối qua JPA Pool")
                .build())
        .redis(
            ComponentHealth.builder()
                .name("Redis")
                .label("Bộ nhớ đệm & Rate limit")
                .status(HealthStatus.HEALTHY)
                .details("Kết nối Lettuce")
                .build())
        .rabbitmq(
            ComponentHealth.builder()
                .name("RabbitMQ")
                .label("Hàng đợi tin nhắn")
                .status(HealthStatus.HEALTHY)
                .details("Spring AMQP listener")
                .build())
        .mail(
            ComponentHealth.builder()
                .name("Mail Service")
                .label("Dịch vụ gửi email")
                .status(HealthStatus.HEALTHY)
                .details("JavaMailSender")
                .build())
        .diskSpace(
            ComponentHealth.builder()
                .name("Disk Space")
                .label("Dung lượng ổ cứng")
                .status(HealthStatus.HEALTHY)
                .details("Hệ số lưu trữ khả dụng")
                .build())
        .build();
  }

  // ==========================================
  // Overall Health Evaluation
  // ==========================================
  private HealthStatus evaluateOverallStatus(
      BasicStats basic,
      ThreadStats threads,
      DatabasePoolStats dbPool,
      HttpStats http,
      LogStats logs,
      InfrastructureHealth infra) {

    // Critical checks
    if (basic.heapUsage().status() == HealthStatus.CRITICAL
        || basic.nonHeapUsage().status() == HealthStatus.CRITICAL
        || basic.systemCpuUsage().status() == HealthStatus.CRITICAL
        || dbPool.activeConnections().status() == HealthStatus.CRITICAL
        || dbPool.connectionTimeouts().status() == HealthStatus.CRITICAL
        || http.http5xxCount().status() == HealthStatus.CRITICAL
        || logs.errorCount().status() == HealthStatus.CRITICAL
        || infra.database().status() == HealthStatus.CRITICAL
        || infra.redis().status() == HealthStatus.CRITICAL) {
      return HealthStatus.CRITICAL;
    }

    // Warning checks
    if (basic.heapUsage().status() == HealthStatus.WARNING
        || basic.nonHeapUsage().status() == HealthStatus.WARNING
        || basic.systemCpuUsage().status() == HealthStatus.WARNING
        || dbPool.activeConnections().status() == HealthStatus.WARNING
        || dbPool.connectionTimeouts().status() == HealthStatus.WARNING
        || http.http4xxCount().status() == HealthStatus.WARNING
        || http.maxResponseTimeMs().status() == HealthStatus.WARNING
        || logs.warnCount().status() == HealthStatus.WARNING) {
      return HealthStatus.WARNING;
    }

    return HealthStatus.HEALTHY;
  }

  private String buildStatusMessage(
      HealthStatus status, DatabasePoolStats dbPool, HttpStats httpStats, LogStats logStats) {
    if (status == HealthStatus.CRITICAL) {
      return "Hệ thống đang gặp sự cố nghiêm trọng. Cần kiểm tra ngay log lỗi hoặc kết nối cơ sở dữ liệu!";
    }
    if (status == HealthStatus.WARNING) {
      if (httpStats.http5xxCount().value() > 0) {
        return "Hệ thống có cảnh báo: Xuất hiện request lỗi 5xx từ server.";
      }
      if (dbPool.connectionTimeouts().value() > 0) {
        return "Hệ thống có cảnh báo: Xuất hiện timeout kết nối database pool.";
      }
      if (logStats.warnCount().value() > 0) {
        return "Hệ thống có cảnh báo nhẹ: Một số chỉ số cần lưu ý theo dõi.";
      }
      return "Hệ thống hoạt động nhưng có một số chỉ số tài nguyên tiệm cận ngưỡng cảnh báo.";
    }
    return "Hệ thống hoạt động ổn định và khỏe mạnh. Mọi chỉ số đều nằm trong ngưỡng an toàn.";
  }

  // ==========================================
  // Helpers
  // ==========================================
  private double getGaugeValue(String name, String... tags) {
    try {
      var search = meterRegistry.find(name);
      if (tags != null && tags.length >= 2) {
        for (int i = 0; i < tags.length; i += 2) {
          search = search.tag(tags[i], tags[i + 1]);
        }
      }
      var gauge = search.gauge();
      if (gauge != null) {
        double val = gauge.value();
        return Double.isNaN(val) ? 0.0 : val;
      }
    } catch (Exception ignored) {
    }
    return 0.0;
  }

  private long sumGauges(String name, String tagKey, String tagVal) {
    try {
      var gauges = meterRegistry.find(name).tag(tagKey, tagVal).gauges();
      double sum = 0;
      for (var g : gauges) {
        double v = g.value();
        if (!Double.isNaN(v) && v > 0) {
          sum += v;
        }
      }
      return (long) sum;
    } catch (Exception ignored) {
      return 0L;
    }
  }

  private double getCounterValue(String name, String... tags) {
    try {
      var search = meterRegistry.find(name);
      if (tags != null && tags.length >= 2) {
        for (int i = 0; i < tags.length; i += 2) {
          search = search.tag(tags[i], tags[i + 1]);
        }
      }
      var counter = search.counter();
      if (counter != null) {
        return counter.count();
      }
    } catch (Exception ignored) {
    }
    return 0.0;
  }

  private double getTimerMean(String name) {
    try {
      var timer = meterRegistry.find(name).timer();
      if (timer != null) {
        return timer.mean(TimeUnit.MILLISECONDS);
      }
    } catch (Exception ignored) {
    }
    return 0.0;
  }

  private String translatePoolName(String poolId, String area) {
    if (poolId == null) return "Vùng nhớ không tên";
    return switch (poolId) {
      case "G1 Eden Space" -> "Vùng nhớ Eden (Heap)";
      case "G1 Old Gen" -> "Vùng nhớ Old Gen (Heap)";
      case "G1 Survivor Space" -> "Vùng nhớ Survivor (Heap)";
      case "CodeCache" -> "Bộ đệm mã nguồn (CodeCache - Non-heap)";
      case "Compressed Class Space" -> "Không gian lớp nén (Compressed Class - Non-heap)";
      case "Metaspace" -> "Siêu dữ liệu JVM (Metaspace - Non-heap)";
      default -> poolId + ("heap".equalsIgnoreCase(area) ? " (Heap)" : " (Non-heap)");
    };
  }

  private static double roundTwoDecimals(double value) {
    if (Double.isNaN(value) || Double.isInfinite(value)) return 0.0;
    return Math.round(value * 100.0) / 100.0;
  }

  private static String formatBytes(long bytes) {
    if (bytes <= 0) return "0 B";
    if (bytes < 1024) return bytes + " B";
    int exp = (int) (Math.log(bytes) / Math.log(1024));
    String pre = "KMGTPE".charAt(exp - 1) + "iB";
    return String.format(Locale.US, "%.1f %s", bytes / Math.pow(1024, exp), pre);
  }

  private static String formatDuration(long seconds) {
    if (seconds < 60) {
      return seconds + " giây";
    }
    long minutes = seconds / 60;
    long secs = seconds % 60;
    if (minutes < 60) {
      return String.format("%d phút %d giây", minutes, secs);
    }
    long hours = minutes / 60;
    long mins = minutes % 60;
    if (hours < 24) {
      return String.format("%d giờ %d phút", hours, mins);
    }
    long days = hours / 24;
    long hrs = hours % 24;
    return String.format("%d ngày %d giờ", days, hrs);
  }

  private static String formatMs(double ms) {
    if (ms < 1000) {
      return String.format(Locale.US, "%.1f ms", ms);
    }
    return String.format(Locale.US, "%.2f s", ms / 1000.0);
  }

  private static class EndpointAggregator {
    final String uri;
    final String method;
    long count = 0;
    double totalDurationMs = 0;
    double maxDurationMs = 0;

    EndpointAggregator(String uri, String method) {
      this.uri = uri;
      this.method = method;
    }

    void accumulate(long c, double dur, double max) {
      this.count += c;
      this.totalDurationMs += dur;
      if (max > this.maxDurationMs) {
        this.maxDurationMs = max;
      }
    }
  }
}
