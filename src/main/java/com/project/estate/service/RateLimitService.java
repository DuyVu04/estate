package com.project.estate.service;

import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;

@RequiredArgsConstructor
@Service
public class RateLimitService {

    private final ProxyManager<String> proxyManager;

    public boolean isAllowed(String key, int limit, Duration window) {
        BucketConfiguration configuration = BucketConfiguration.builder()
                .addLimit(l -> l.capacity(limit).refillGreedy(limit, window))
                .build();

        Bucket bucket = proxyManager.builder().build(key, () -> configuration);
        return bucket.tryConsume(1);
    }
}
