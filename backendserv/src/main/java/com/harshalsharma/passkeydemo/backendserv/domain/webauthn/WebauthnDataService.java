package com.harshalsharma.passkeydemo.backendserv.domain.webauthn;

import com.harshalsharma.passkeydemo.backendserv.domain.webauthn.entities.Credential;
import jakarta.transaction.Transactional;

import java.util.Optional;

public interface WebauthnDataService {

    Optional<Credential> findById(String credentialId);

    @Transactional
    void save(Credential credential);
}
