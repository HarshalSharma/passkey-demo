package com.harshalsharma.passkeydemo.backendserv.data.repositories;

import com.harshalsharma.passkeydemo.backendserv.domain.webauthn.WebauthnDataService;
import com.harshalsharma.passkeydemo.backendserv.domain.webauthn.entities.Credential;
import com.harshalsharma.passkeydemo.backendserv.domain.webauthn.entities.Preference;
import jakarta.inject.Inject;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;

@Component
public class WebauthnDataServiceImpl implements WebauthnDataService {

    private final WebAuthnRepository webAuthnRepository;
    private final PreferenceRepository preferenceRepository;

    @Inject
    public WebauthnDataServiceImpl(WebAuthnRepository webAuthnRepository, PreferenceRepository preferenceRepository) {
        this.webAuthnRepository = webAuthnRepository;
        this.preferenceRepository = preferenceRepository;
    }

    @Override
    public Optional<Credential> findById(String credentialId) {
        return webAuthnRepository.findById(credentialId);
    }

    @Override
    public List<Credential> findByUserId(String userId) {
        return webAuthnRepository.findByUserId(userId);
    }

    @Override
    public void save(Credential credential) {
        webAuthnRepository.save(credential);
    }

    @Override
    public List<Credential> findByLocation(double latitude, double longitude, double radius) {
        double minLat = latitude - (radius / 111.12);
        double maxLat = latitude + (radius / 111.12);
        double minLong = longitude - (radius / (111.12 * Math.cos(latitude)));
        double maxLong = longitude + (radius / (111.12 * Math.cos(latitude)));
        List<Preference> prefs = preferenceRepository.findByLatitudeBetweenAndLongitudeBetween(minLat, maxLat, minLong, maxLong);
        if (CollectionUtils.isEmpty(prefs)) {
            return Collections.emptyList();
        }
        Set<String> userHandles = prefs.stream().map(Preference::getUserId).collect(Collectors.toSet());
        List<Credential> output = new ArrayList<>();
        for (String userHandle : userHandles) {
            output.addAll(webAuthnRepository.findByUserId(userHandle));
        }
        return output;
    }
}
