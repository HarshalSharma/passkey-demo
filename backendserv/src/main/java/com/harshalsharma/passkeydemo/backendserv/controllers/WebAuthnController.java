package com.harshalsharma.passkeydemo.backendserv.controllers;

import com.harshalsharma.passkeydemo.apispec.api.AuthenticationApi;
import com.harshalsharma.passkeydemo.apispec.api.RegistrationApi;
import com.harshalsharma.passkeydemo.apispec.model.*;
import com.harshalsharma.passkeydemo.backendserv.data.cache.CacheService;
import com.harshalsharma.passkeydemo.backendserv.domain.webauthn.WebauthnProperties;
import com.harshalsharma.passkeydemo.backendserv.exceptions.InvalidRequestException;
import jakarta.inject.Inject;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

import static com.harshalsharma.passkeydemo.backendserv.domain.UniqueStringGenerator.generateUUIDString;

@Component
public class WebAuthnController implements RegistrationApi, AuthenticationApi {

    private final WebauthnProperties webAuthnProperties;

    private final CacheService cacheService;

    @Inject
    public WebAuthnController(WebauthnProperties webAuthnProperties, CacheService cacheService) {
        this.webAuthnProperties = webAuthnProperties;
        this.cacheService = cacheService;
    }

    @Override
    public PublicKeyCredentialCreationOptionsResponse registrationGet() {
        PublicKeyCredentialCreationOptionsResponse creationOptionsResponse
                = new PublicKeyCredentialCreationOptionsResponse();
        creationOptionsResponse.setRpId(webAuthnProperties.getRpId());
        creationOptionsResponse.setRpName(webAuthnProperties.getRpName());
        List<PublicKeyCredentialParam> publicKeyCredentialParams = webAuthnProperties.getSupportedPublicKeyAlgs().stream()
                .map(alg -> new PublicKeyCredentialParam().alg(BigDecimal.valueOf(alg)).type("public-key"))
                .collect(Collectors.toList());
        creationOptionsResponse.setPubKeyCredParams(publicKeyCredentialParams);
        String challenge = generateUUIDString();
        String userId = generateUUIDString();
        creationOptionsResponse.setUserId(userId);
        creationOptionsResponse.setChallenge(challenge);
        cacheService.put(userId + "_challenge", challenge);
        creationOptionsResponse.setDisplayName("PassKey-Demo");
        creationOptionsResponse.setUserName(userId);
        return creationOptionsResponse;
    }

    @Override
    public void registrationPost(RegistrationRequest registrationRequest) {

        throw new InvalidRequestException("Invalid Attestation Object");
    }

    @Override
    public PublicKeyCredentialRequestOptionsResponse authenticationUserHandleGet(String userHandle) {
        return null;
    }

    @Override
    public SuccessfulAuthenticationResponse authenticationUserHandlePost(String userHandle, AuthenticationRequest authenticationRequest) {
        return null;
    }
}
