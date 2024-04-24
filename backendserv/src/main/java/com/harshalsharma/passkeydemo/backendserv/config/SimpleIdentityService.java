package com.harshalsharma.passkeydemo.backendserv.config;

import com.harshalsharma.passkeydemo.backendserv.domain.notes.IdentityService;
import com.harshalsharma.passkeydemo.backendserv.domain.webauthn.authentication.TokenService;
import jakarta.inject.Inject;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

@Component
public class SimpleIdentityService implements IdentityService {

    private final TokenService tokenService;

    private static final ThreadLocal<String> authToken = new ThreadLocal<>();

    @Inject
    public SimpleIdentityService(TokenService tokenService) {
        this.tokenService = tokenService;
    }

    @Override
    public String getCurrentUserId() {
        String token = authToken.get();
        if (StringUtils.isNotEmpty(token)) {
            return tokenService.getSecurityContext(token).getUserHandle();
        }
        return null;
    }

    public void setAuthToken(String token) {
        authToken.set(token);
    }

    public void clearRequestAuthToken() {
        authToken.remove();
    }
}
