package com.harshalsharma.passkeydemo.backendserv.domain.webauthn.registration;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.harshalsharma.passkeydemo.apispec.api.RegistrationApi;
import com.harshalsharma.passkeydemo.apispec.model.*;
import com.harshalsharma.passkeydemo.backendserv.data.cache.CacheService;
import com.harshalsharma.passkeydemo.backendserv.domain.webauthn.ErrorDescriptions;
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
public class WebAuthnRegistrationService implements RegistrationApi {

    private final WebauthnProperties webAuthnProperties;

    private final CacheService cacheService;

    private final WebauthnDataService webauthnDataService;

    @Inject
    public WebAuthnRegistrationService(WebauthnProperties webAuthnProperties, CacheService cacheService,
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
        String userHandle = registrationRequest.getUserHandle();
        if (StringUtils.isBlank(userHandle)) {
            throw new InvalidRequestException(ErrorDescriptions.INVALID_USER_HANDLE);
        }
        try {
            String attestationObject = registrationRequest.getAttestationObject();
            AttestationObjectExplorer objectExplorer = AttestationObjectReader.read(attestationObject);
            ClientDataJson clientDataJson = readClientDataJson(registrationRequest);
            validateClientDataJson(clientDataJson);
            String cacheKey = userHandle + "_challenge";
            Optional<String> cacheChallenge = cacheService.get(cacheKey);
            if (cacheChallenge.isPresent() && StringUtils.equals(cacheChallenge.get(), clientDataJson.getChallenge())) {
                webauthnDataService.save(Credential.builder()
                        .credentialId(objectExplorer.getWebauthnId())
                        .publicKey(objectExplorer.getEncodedPublicKeySpec())
                        .publicKeyType(objectExplorer.getKeyType())
                        .userId(userHandle)
                        .build());
            } else {
                throw new InvalidRequestException(ErrorDescriptions.INVALID_CHALLENGE);
            }
        } catch (InvalidAttestationObjException ex) {
            throw new InvalidRequestException(ErrorDescriptions.INVALID_ATTESTATION_OBJECT, ex);
        }
    }

    private void validateClientDataJson(ClientDataJson clientDataJson) {
        if (!StringUtils.equals(clientDataJson.getOrigin(), "https://" + webAuthnProperties.getRpId())) {
            throw new InvalidRequestException(ErrorDescriptions.INVALID_ORIGIN);
        }
        if (!StringUtils.equals(clientDataJson.getType(), "webauthn.create")) {
            throw new InvalidRequestException(ErrorDescriptions.INVALID_CDJ_TYPE);
        }
    }

    private static ClientDataJson readClientDataJson(RegistrationRequest registrationRequest) {
        ClientDataJson clientDataJsonObject;
        String clientDataJson = registrationRequest.getClientDataJson();
        if (StringUtils.isBlank(clientDataJson)) {
            throw new InvalidRequestException(ErrorDescriptions.INVALID_VALUE_FOR_CLIENT_DATA_JSON);
        }
        try {
            String decodedClientDataJson = new String(Base64.decodeBase64(clientDataJson));
            ObjectMapper mapper = new ObjectMapper();
            try {
                clientDataJsonObject = mapper.readValue(decodedClientDataJson, ClientDataJson.class);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        } catch (Exception e) {
            throw new InvalidRequestException(ErrorDescriptions.INVALID_VALUE_FOR_CLIENT_DATA_JSON);
        }
        return clientDataJsonObject;
    }

}
