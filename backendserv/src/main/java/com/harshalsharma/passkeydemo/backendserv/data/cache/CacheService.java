package com.harshalsharma.passkeydemo.backendserv.data.cache;

import java.util.Optional;

public interface CacheService {
    void put(String key, String value);

    void put(String key, byte[] value);

    Optional<String> get(String key);

    Optional<byte[]> getBytes(String key);

    void remove(String cacheKey);
}
