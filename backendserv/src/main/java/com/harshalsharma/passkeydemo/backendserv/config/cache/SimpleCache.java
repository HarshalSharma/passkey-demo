package com.harshalsharma.passkeydemo.backendserv.config.cache;

import com.harshalsharma.passkeydemo.backendserv.data.cache.CacheService;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class SimpleCache implements CacheService {

    private final ConcurrentHashMap<String, byte[]> cache = new ConcurrentHashMap<>();

    @Override
    public void put(String key, String value) {
        put(key, value.getBytes());
    }

    @Override
    public void put(String key, byte[] value) {
        cache.put(key, value);
    }

    @Override
    public Optional<String> get(String key) {
        return getBytes(key).map(String::new);
    }

    @Override
    public Optional<byte[]> getBytes(String key) {
        return Optional.ofNullable(cache.get(key));
    }
}
