package com.harshalsharma.passkeydemo.backendserv.domain.webauthn.authentication;

import com.harshalsharma.passkeydemo.backendserv.domain.webauthn.entities.SecurityContext;

public interface TokenService {

    String createToken(String userHandle);

    boolean isValid(String token);

    SecurityContext getSecurityContext(String token);

}
