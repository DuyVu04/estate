package com.project.estate.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.project.estate.dto.request.PropertyUpdateRequest;
import com.project.estate.dto.response.PropertyResponse;
import com.project.estate.entity.Property;
import com.project.estate.enums.PropertyStatus;
import com.project.estate.enums.PropertyType;
import com.project.estate.mapper.PropertyMapper;
import com.project.estate.repository.PropertyRepository;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

@SpringJUnitConfig(
    classes = {PropertyCacheBenchmarkTest.CacheTestConfig.class, PropertyService.class})
class PropertyCacheBenchmarkTest {

  @TestConfiguration
  @EnableCaching
  static class CacheTestConfig {
    @Bean
    public CacheManager cacheManager() {
      return new ConcurrentMapCacheManager("property_detail");
    }

    @Bean
    public PropertyRepository propertyRepository() {
      return mock(PropertyRepository.class);
    }

    @Bean
    public PropertyMapper propertyMapper() {
      return mock(PropertyMapper.class);
    }

    @Bean
    public com.project.estate.messaging.producer.PropertyVectorProducer propertyVectorProducer() {
      return mock(com.project.estate.messaging.producer.PropertyVectorProducer.class);
    }

    @Bean
    public MinioService minioService() {
      return mock(MinioService.class);
    }
  }

  @Autowired private PropertyService propertyService;
  @Autowired private PropertyRepository propertyRepository;
  @Autowired private PropertyMapper propertyMapper;
  @Autowired private CacheManager cacheManager;

  @Test
  @DisplayName("Benchmark & Invalidation: Measure Cache Miss vs 1,000 Cache Hits & Verify Eviction")
  void testCachePerformanceAndInvalidationBenchmark() {
    String propertyId = "prop-villa-001";

    Property mockProperty =
        Property.builder()
            .id(propertyId)
            .title("Villa Riviera")
            .price(BigDecimal.valueOf(15000000000L))
            .status(PropertyStatus.AVAILABLE)
            .build();

    PropertyResponse mockResponse =
        PropertyResponse.builder()
            .id(propertyId)
            .title("Villa Riviera")
            .description("Luxury Villa with Pool")
            .propertyType(PropertyType.VILLA)
            .address("123 Nguyen Van Huong")
            .ward("Thao Dien")
            .district("District 2")
            .city("HCMC")
            .area(BigDecimal.valueOf(500))
            .price(BigDecimal.valueOf(15000000000L))
            .status(PropertyStatus.AVAILABLE)
            .build();

    when(propertyRepository.findById(propertyId))
        .thenAnswer(
            inv -> {
              // Simulate Database IO Latency (20ms query time)
              Thread.sleep(20);
              return Optional.of(mockProperty);
            });

    when(propertyMapper.toPropertyResponse(mockProperty)).thenReturn(mockResponse);

    // =========================================================================
    // 1. FIRST CALL: CACHE MISS (Hits Simulated Database)
    // =========================================================================
    long startDb = System.nanoTime();
    PropertyResponse res1 = propertyService.getPropertyById(propertyId);
    long dbDurationNs = System.nanoTime() - startDb;
    double dbDurationMs = dbDurationNs / 1_000_000.0;

    assertNotNull(res1);
    verify(propertyRepository, times(1)).findById(propertyId);

    // =========================================================================
    // 2. NEXT 1,000 CALLS: CACHE HIT (Pure RAM Cache Retrieval)
    // =========================================================================
    int iterations = 1000;
    long startCache = System.nanoTime();
    for (int i = 0; i < iterations; i++) {
      PropertyResponse cached = propertyService.getPropertyById(propertyId);
      assertNotNull(cached);
    }
    long cacheTotalDurationNs = System.nanoTime() - startCache;
    double cacheAvgDurationMs = (cacheTotalDurationNs / (double) iterations) / 1_000_000.0;

    // Database must still be called ONLY ONCE because of cache hits!
    verify(propertyRepository, times(1)).findById(propertyId);

    double speedup = dbDurationMs / Math.max(cacheAvgDurationMs, 0.0001);

    // =========================================================================
    // 3. CACHE EVICTION TEST (Update Property -> Cache is purged)
    // =========================================================================
    when(propertyRepository.save(any())).thenReturn(mockProperty);
    PropertyUpdateRequest updateReq =
        PropertyUpdateRequest.builder()
            .title("Villa Riviera Updated")
            .description("New Desc")
            .propertyType(PropertyType.VILLA)
            .address("123 Nguyen Van Huong")
            .ward("Thao Dien")
            .district("District 2")
            .city("HCMC")
            .area(BigDecimal.valueOf(500))
            .price(BigDecimal.valueOf(16000000000L))
            .build();

    propertyService.updateProperty(propertyId, updateReq);

    // After update, next getPropertyById MUST hit database again! (Total DB calls = 3: 1st get + 1
    // update + 2nd get)
    propertyService.getPropertyById(propertyId);
    verify(propertyRepository, times(3)).findById(propertyId);

    // =========================================================================
    // 4. PRINT BENCHMARK DASHBOARD
    // =========================================================================
    System.out.println("\n=======================================================================");
    System.out.println("               🚀 REDIS / SPRING CACHE BENCHMARK REPORT                ");
    System.out.println("=======================================================================");
    System.out.printf(
        "  1. Database Query (Cache MISS) :  %.3f ms (Simulated DB fetch)\n", dbDurationMs);
    System.out.printf(
        "  2. Cache Read (1,000 Hits Avg) :  %.4f ms / request\n", cacheAvgDurationMs);
    System.out.printf("  3. Total Iterations Tested     :  %d requests\n", iterations);
    System.out.printf("  4. Performance Acceleration    :  🔥 %.1fx FASTER with Cache!\n", speedup);
    System.out.println("  5. Cache Eviction on Update    :  ✅ PASSED (Cache cleared successfully)");
    System.out.println("=======================================================================\n");

    assertTrue(speedup > 5.0, "Cache read must be significantly faster than DB query");
  }
}
