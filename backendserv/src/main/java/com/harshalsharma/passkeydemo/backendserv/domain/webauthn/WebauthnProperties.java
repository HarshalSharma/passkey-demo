package com.harshalsharma.passkeydemo.backendserv.domain.webauthn;

import java.util.List;

public interface WebauthnProperties {

    String getRpId();

    String getRpName();

    List<Integer> getSupportedPublicKeyAlgs();

    int getTokenTimeoutInMillis();
}
