package com.harshalsharma.passkeydemo.backendserv.domain.webauthn.preferences;

import com.harshalsharma.passkeydemo.apispec.api.PreferencesApi;
import com.harshalsharma.passkeydemo.apispec.model.Preferences;
import com.harshalsharma.passkeydemo.backendserv.data.repositories.PreferenceRepository;
import com.harshalsharma.passkeydemo.backendserv.domain.notes.IdentityService;
import com.harshalsharma.passkeydemo.backendserv.domain.webauthn.entities.Preference;
import jakarta.inject.Inject;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Optional;

@Component
public class PreferencesService implements PreferencesApi {

    private final PreferenceRepository preferenceRepository;

    private final IdentityService identityService;

    @Inject
    public PreferencesService(PreferenceRepository preferenceRepository, IdentityService identityService) {
        this.preferenceRepository = preferenceRepository;
        this.identityService = identityService;
    }

    @Override
    public Preferences preferencesGet() {
        Optional<Preference> dbPrefs = preferenceRepository.findById(identityService.getCurrentUserId());
        Preferences preferences = new Preferences();
        if (dbPrefs.isEmpty()) {
            return preferences;
        }
        Preference preference = dbPrefs.get();
        preferences.setHomeLat(BigDecimal.valueOf(preference.getLatitude()));
        preferences.setHomeLog(BigDecimal.valueOf(preference.getLongitude()));
        return preferences;
    }

    @Override
    public void preferencesPut(Preferences preferences) {
        Preference preference = Preference.builder().userId(identityService.getCurrentUserId())
                .latitude(preferences.getHomeLat().doubleValue())
                .longitude(preferences.getHomeLog().doubleValue()).build();
        preferenceRepository.save(preference);
    }
}
