package com.harshalsharma.passkeydemo.backendserv.config.cache;

import jakarta.inject.Inject;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

@Component
public class SimpleCacheEvictionJob {

    public static final int JOB_INTERVAL = 5000;
    public static final int CACHE_TIMEOUT = 60000;
    private final SimpleCache simpleCache;

    @Inject
    public SimpleCacheEvictionJob(SimpleCache cacheService) {
        this.simpleCache = cacheService;
    }

    /**
     * runs every fixedRate milliseconds (1min)
     */
    @Scheduled(fixedRate = JOB_INTERVAL)
    public void run() {
        long current = System.currentTimeMillis();
        ConcurrentHashMap<String, SimpleCache.CacheValue> cache = simpleCache.getCache();
        cache.forEach((k, v) -> {
            if ((current - v.timestamp) >= CACHE_TIMEOUT) {
                simpleCache.remove(k);
            }
        });
    }

}
