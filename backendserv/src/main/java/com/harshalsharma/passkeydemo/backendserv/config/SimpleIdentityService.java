package com.harshalsharma.passkeydemo.backendserv.config;

import com.harshalsharma.passkeydemo.backendserv.domain.notes.IdentityService;
import com.harshalsharma.passkeydemo.backendserv.domain.webauthn.authentication.TokenService;
import jakarta.inject.Inject;
import lombok.Setter;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

@Component
@RequestScope
public class SimpleIdentityService implements IdentityService {

    private final TokenService tokenService;

    @Setter
    private String authToken;

    @Inject
    public SimpleIdentityService(TokenService tokenService) {
        this.tokenService = tokenService;
    }

    @Override
    public String getCurrentUserId() {
        return tokenService.getSecurityContext(authToken).getUserHandle();
    }
}
