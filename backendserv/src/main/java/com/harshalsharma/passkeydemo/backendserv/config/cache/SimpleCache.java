package com.harshalsharma.passkeydemo.backendserv.config.cache;

import com.harshalsharma.passkeydemo.backendserv.data.cache.CacheService;
import lombok.Getter;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Getter
@Component
public class SimpleCache implements CacheService {

    private final ConcurrentHashMap<String, CacheValue> cache = new ConcurrentHashMap<>();

    @Override
    public void put(String key, String value) {
        put(key, value.getBytes());
    }

    @Override
    public void put(String key, byte[] value) {
        cache.put(key, new CacheValue(value));
    }

    @Override
    public Optional<String> get(String key) {
        return getBytes(key).map(String::new);
    }

    @Override
    public Optional<byte[]> getBytes(String key) {
        return Optional.ofNullable(cache.get(key)).map(CacheValue::getValue);
    }

    @Override
    public void remove(String cacheKey) {
        cache.remove(cacheKey);
    }

    @Getter
    static class CacheValue {
        long timestamp;
        byte[] value;

        public CacheValue(byte[] value) {
            this.value = value;
            this.timestamp = System.currentTimeMillis();
        }
    }
}
