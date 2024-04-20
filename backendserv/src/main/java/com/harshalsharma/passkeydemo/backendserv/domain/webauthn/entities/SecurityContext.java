package com.harshalsharma.passkeydemo.backendserv.domain.webauthn.entities;

import lombok.Data;

@Data
public class SecurityContext {

    private String userHandle;

    private long validity;

}
