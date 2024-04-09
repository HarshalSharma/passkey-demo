package com.harshalsharma.passkeydemo.backendserv.controllers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.harshalsharma.passkeydemo.apispec.api.AuthenticationApi;
import com.harshalsharma.passkeydemo.apispec.api.RegistrationApi;
import com.harshalsharma.passkeydemo.apispec.model.*;
import com.harshalsharma.passkeydemo.backendserv.data.cache.CacheService;
import com.harshalsharma.passkeydemo.backendserv.domain.webauthn.WebauthnDataService;
import com.harshalsharma.passkeydemo.backendserv.domain.webauthn.WebauthnProperties;
import com.harshalsharma.passkeydemo.backendserv.domain.webauthn.entities.Credential;
import com.harshalsharma.passkeydemo.backendserv.exceptions.InvalidRequestException;
import com.harshalsharma.webauthncommons.attestationObject.AttestationObjectExplorer;
import com.harshalsharma.webauthncommons.attestationObject.AttestationObjectReader;
import com.harshalsharma.webauthncommons.attestationObject.exceptions.InvalidAttestationObjException;
import com.harshalsharma.webauthncommons.entities.ClientDataJson;
import jakarta.inject.Inject;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.harshalsharma.passkeydemo.backendserv.domain.UniqueStringGenerator.generateUUIDString;

@Component
public class WebAuthnController implements RegistrationApi, AuthenticationApi {

    private final WebauthnProperties webAuthnProperties;

    private final CacheService cacheService;

    private final WebauthnDataService webauthnDataService;

    @Inject
    public WebAuthnController(WebauthnProperties webAuthnProperties, CacheService cacheService,
                              WebauthnDataService webauthnDataService) {
        this.webAuthnProperties = webAuthnProperties;
        this.cacheService = cacheService;
        this.webauthnDataService = webauthnDataService;
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
        if (StringUtils.isBlank(registrationRequest.getUserHandle())) {
            throw new InvalidRequestException("Invalid UserHandle.");
        }
        try {
            String attestationObject = registrationRequest.getAttestationObject();
            AttestationObjectExplorer objectExplorer = AttestationObjectReader.read(attestationObject);
            String clientDataJson = registrationRequest.getClientDataJson();
            if (StringUtils.isBlank(clientDataJson)) {
                throw new InvalidRequestException("Invalid ClientDataJson");
            }
            String decodedClientDataJson = new String(Base64.decodeBase64(clientDataJson));
            ClientDataJson clientDataJsonObject;
            ObjectMapper mapper = new ObjectMapper();
            try {
                clientDataJsonObject = mapper.readValue(decodedClientDataJson, ClientDataJson.class);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
            Optional<String> cacheChallenge = cacheService.get(registrationRequest.getUserHandle() + "_challenge");
            if (cacheChallenge.isPresent() && StringUtils.equals(cacheChallenge.get(), clientDataJsonObject.getChallenge())) {
                //
            } else {
                throw new InvalidRequestException("Invalid ClientDataJson");
            }
            webauthnDataService.save(Credential.builder()
                    .credentialId(objectExplorer.getWebauthnId())
                    .publicKey(objectExplorer.getEncodedPublicKeySpec())
                    .publicKeyType(objectExplorer.getKeyType())
                    .build());
        } catch (InvalidAttestationObjException ex) {
            throw new InvalidRequestException("Invalid Attestation Object", ex);
        }
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
