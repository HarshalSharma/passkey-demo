package com.harshalsharma.passkeydemo.backendserv.domain.webauthn.registration;

import com.harshalsharma.passkeydemo.apispec.api.RegistrationApi;
import com.harshalsharma.passkeydemo.apispec.model.PublicKeyCredentialCreationOptionsResponse;
import com.harshalsharma.passkeydemo.apispec.model.PublicKeyCredentialParam;
import com.harshalsharma.passkeydemo.apispec.model.RegistrationRequest;
import com.harshalsharma.passkeydemo.backendserv.data.cache.CacheService;
import com.harshalsharma.passkeydemo.backendserv.domain.webauthn.ErrorDescriptions;
import com.harshalsharma.passkeydemo.backendserv.domain.webauthn.WebauthnDataService;
import com.harshalsharma.passkeydemo.backendserv.domain.webauthn.WebauthnProperties;
import com.harshalsharma.passkeydemo.backendserv.domain.webauthn.entities.Credential;
import com.harshalsharma.passkeydemo.backendserv.exceptions.InvalidRequestException;
import com.harshalsharma.webauthncommons.attestationObject.AttestationObjectExplorer;
import com.harshalsharma.webauthncommons.attestationObject.AttestationObjectReader;
import com.harshalsharma.webauthncommons.attestationObject.exceptions.InvalidAttestationObjException;
import com.harshalsharma.webauthncommons.clientdatajson.ClientDataJsonReader;
import com.harshalsharma.webauthncommons.clientdatajson.ClientDataJsonValidator;
import com.harshalsharma.webauthncommons.clientdatajson.InvalidClientDataJsonException;
import com.harshalsharma.webauthncommons.entities.ClientDataJson;
import jakarta.inject.Inject;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.harshalsharma.passkeydemo.backendserv.domain.UniqueStringGenerator.*;

@Component
public class WebAuthnRegistrationService implements RegistrationApi {

    private final WebauthnProperties webAuthnProperties;

    private final CacheService cacheService;

    private final WebauthnDataService webauthnDataService;

    private final ClientDataJsonValidator clientDataJsonValidator;

    @Inject
    public WebAuthnRegistrationService(WebauthnProperties webAuthnProperties, CacheService cacheService,
                                       WebauthnDataService webauthnDataService) {
        this.webAuthnProperties = webAuthnProperties;
        this.cacheService = cacheService;
        this.webauthnDataService = webauthnDataService;
        this.clientDataJsonValidator = ClientDataJsonValidator.builder()
                .origin(webAuthnProperties.getOrigin())
                .operation(ClientDataJsonValidator.CDJType.WEBAUTHN_CREATE).build();
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

        String challenge = generateChallenge();
        String userId = generateUniqueName();

        creationOptionsResponse.setUserId(userId);
        creationOptionsResponse.setChallenge(challenge);

        cacheService.put(userId + "_challenge", challenge);

        creationOptionsResponse.setDisplayName("PassKey-Demo");
        creationOptionsResponse.setUserName(userId);
        return creationOptionsResponse;
    }

    @NotNull
    private String generateUniqueName() {
        String userId = null;
        do {
            userId = generateRandomName();
        }
        while (webauthnDataService.findById(userId).isPresent());
        return userId;
    }

    @Override
    public void registrationPost(RegistrationRequest registrationRequest) {
        String userHandle = registrationRequest.getUserHandle();
        if (StringUtils.isBlank(userHandle)) {
            throw new InvalidRequestException(ErrorDescriptions.INVALID_USER_HANDLE);
        }

        String cacheKey = userHandle + "_challenge";
        Optional<String> cacheChallenge = cacheService.get(cacheKey);
        if (cacheChallenge.isEmpty()) {
            throw new InvalidRequestException(ErrorDescriptions.INVALID_CHALLENGE);
        }
        validateClientDataJson(registrationRequest);
        AttestationObjectExplorer objectExplorer = getAttestationObjectExplorer(registrationRequest);
        webauthnDataService.save(Credential.builder()
                .credentialId(objectExplorer.getWebauthnId())
                .publicKey(objectExplorer.getEncodedPublicKeySpec())
                .publicKeyType(objectExplorer.getKeyType())
                .userId(userHandle)
                .build());
        cacheService.remove(cacheKey);
    }

    private void validateClientDataJson(RegistrationRequest registrationRequest) {
        try {
            ClientDataJson clientDataJson = ClientDataJsonReader.read(registrationRequest.getClientDataJson());
            clientDataJsonValidator.validate(clientDataJson, clientDataJson.getChallenge());
        } catch (InvalidClientDataJsonException e) {
            throw new InvalidRequestException(ErrorDescriptions.INVALID_CLIENT_DATA_JSON, e);
        }
    }

    @NotNull
    private static AttestationObjectExplorer getAttestationObjectExplorer(RegistrationRequest registrationRequest) {
        AttestationObjectExplorer objectExplorer;
        try {
            String attestationObject = registrationRequest.getAttestationObject();
            objectExplorer = AttestationObjectReader.read(attestationObject);
        } catch (InvalidAttestationObjException ex) {
            throw new InvalidRequestException(ErrorDescriptions.INVALID_ATTESTATION_OBJECT, ex);
        }
        return objectExplorer;
    }

}
