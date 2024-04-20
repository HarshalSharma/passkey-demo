package com.harshalsharma.passkeydemo.backendserv.domain.webauthn.authentication;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.harshalsharma.passkeydemo.backendserv.data.cache.CacheService;
import com.harshalsharma.passkeydemo.backendserv.domain.UniqueStringGenerator;
import com.harshalsharma.passkeydemo.backendserv.domain.webauthn.WebauthnProperties;
import com.harshalsharma.passkeydemo.backendserv.domain.webauthn.entities.SecurityContext;
import jakarta.inject.Inject;
import org.apache.commons.codec.binary.Base64;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Optional;

@Component
public class SimpleTokenService implements TokenService {

    private final CacheService cacheService;

    private final WebauthnProperties properties;

    private final ObjectMapper mapper = new ObjectMapper();

    @Inject
    public SimpleTokenService(CacheService cacheService, WebauthnProperties properties) {
        this.cacheService = cacheService;
        this.properties = properties;
    }

    @Override
    public String createToken(String userHandle) {
        String token = Base64.encodeBase64URLSafeString(UniqueStringGenerator.generateUUIDString().getBytes());
        SecurityContext securityContext = new SecurityContext();
        securityContext.setUserHandle(userHandle);
        securityContext.setValidity(getValidity());
        try {
            cacheService.put(token, mapper.writeValueAsBytes(securityContext));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        return token;
    }

    @Override
    public boolean isValid(String token) {
        return cacheService.getBytes(token).isPresent();
    }

    @Override
    public SecurityContext getSecurityContext(String token) {
        Optional<byte[]> optionalBytes = cacheService.getBytes(token);
        if (optionalBytes.isPresent()) {
            try {
                return mapper.readValue(optionalBytes.get(), SecurityContext.class);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return null;
    }

    private long getValidity() {
        return System.currentTimeMillis() + properties.getTokenTimeoutInMillis();
    }
}
