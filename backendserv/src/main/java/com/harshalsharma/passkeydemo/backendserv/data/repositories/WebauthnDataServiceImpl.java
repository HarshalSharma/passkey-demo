package com.harshalsharma.passkeydemo.backendserv.data.repositories;

import com.harshalsharma.passkeydemo.backendserv.domain.webauthn.WebauthnDataService;
import com.harshalsharma.passkeydemo.backendserv.domain.webauthn.entities.Credential;
import jakarta.inject.Inject;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class WebauthnDataServiceImpl implements WebauthnDataService {

    private final WebAuthnRepository webAuthnRepository;

    @Inject
    public WebauthnDataServiceImpl(WebAuthnRepository webAuthnRepository) {
        this.webAuthnRepository = webAuthnRepository;
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
}
