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
        double earthRadius = 6371; // Earth's radius in kilometers (or miles, adjust accordingly)

        double dLat = Math.toRadians(radius / earthRadius);
        double dLon = Math.toRadians(radius / (earthRadius * Math.cos(Math.toRadians(latitude))));

        double minLat = latitude - dLat;
        double maxLat = latitude + dLat;

        double minLong = longitude - dLon;
        double maxLong = longitude + dLon;

        List<Preference> prefs = preferenceRepository.findByLatitudeBetweenAndLongitudeBetween(
                Math.max(minLat, -90),
                Math.min(maxLat, 90), Math.max(minLong, -180), Math.min(maxLong, 180));
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
