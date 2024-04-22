package com.harshalsharma.passkeydemo.backendserv.domain.webauthn;

import com.harshalsharma.passkeydemo.backendserv.domain.webauthn.entities.Credential;
import jakarta.transaction.Transactional;

import java.util.List;
import java.util.Optional;

public interface WebauthnDataService {

    Optional<Credential> findById(String credentialId);

    List<Credential> findByUserId(String userId);

    @Transactional
    void save(Credential credential);

    /**
     * @param latitude  latitude of location
     * @param longitude longitude of location
     * @param radius    number of Kms around
     * @return credentials matching this area.
     */
    List<Credential> findByLocation(double latitude, double longitude, double radius);
}
