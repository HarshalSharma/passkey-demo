package com.harshalsharma.passkeydemo.backendserv.domain.webauthn.authentication;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.harshalsharma.passkeydemo.apispec.api.AuthenticationApi;
import com.harshalsharma.passkeydemo.apispec.model.AllowedCredential;
import com.harshalsharma.passkeydemo.apispec.model.AuthenticationRequest;
import com.harshalsharma.passkeydemo.apispec.model.PublicKeyCredentialRequestOptionsResponse;
import com.harshalsharma.passkeydemo.apispec.model.SuccessfulAuthenticationResponse;
import com.harshalsharma.passkeydemo.backendserv.data.cache.CacheService;
import com.harshalsharma.passkeydemo.backendserv.domain.webauthn.ErrorDescriptions;
import com.harshalsharma.passkeydemo.backendserv.domain.webauthn.WebauthnDataService;
import com.harshalsharma.passkeydemo.backendserv.domain.webauthn.WebauthnProperties;
import com.harshalsharma.passkeydemo.backendserv.domain.webauthn.entities.Credential;
import com.harshalsharma.passkeydemo.backendserv.exceptions.InvalidRequestException;
import com.harshalsharma.webauthncommons.attestationObject.parsers.AuthenticatorDataReader;
import com.harshalsharma.webauthncommons.authentication.SignatureVerifier;
import com.harshalsharma.webauthncommons.entities.AuthenticatorAssertionResponse;
import com.harshalsharma.webauthncommons.entities.AuthenticatorData;
import com.harshalsharma.webauthncommons.entities.ClientDataJson;
import com.harshalsharma.webauthncommons.publickey.PublicKeyCredential;
import jakarta.inject.Inject;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.harshalsharma.passkeydemo.backendserv.domain.UniqueStringGenerator.generateUUIDString;

@Component
public class WebAuthnAuthenticationService implements AuthenticationApi {

    private final WebauthnProperties webAuthnProperties;

    private final CacheService cacheService;

    private final WebauthnDataService webauthnDataService;

    private final TokenService tokenService;

    @Inject
    public WebAuthnAuthenticationService(WebauthnProperties webAuthnProperties, CacheService cacheService,
                                         WebauthnDataService webauthnDataService, TokenService tokenService) {
        this.webAuthnProperties = webAuthnProperties;
        this.cacheService = cacheService;
        this.webauthnDataService = webauthnDataService;
        this.tokenService = tokenService;
    }

    @Override
    public PublicKeyCredentialRequestOptionsResponse authenticationUserHandleGet(String userHandle) {
        PublicKeyCredentialRequestOptionsResponse optionsResponse = new PublicKeyCredentialRequestOptionsResponse();
        List<Credential> credentials = webauthnDataService.findByUserId(userHandle);
        if (CollectionUtils.isEmpty(credentials)) {
            throw new InvalidRequestException(ErrorDescriptions.NO_CREDENTIALS_FOUND);
        }

        optionsResponse.setAllowedCredentials(getAllowedCreds(credentials));
        optionsResponse.setUserVerification("preferred");
        optionsResponse.setRpId(webAuthnProperties.getRpId());
        String challenge = generateUUIDString();
        optionsResponse.setChallenge(challenge);
        cacheService.put(getCacheKey(userHandle), challenge);
        return optionsResponse;
    }

    private List<AllowedCredential> getAllowedCreds(List<Credential> credentials) {
        return credentials.stream().map(cred -> {
            AllowedCredential allowedCredential = new AllowedCredential();
            allowedCredential.setId(cred.getCredentialId());
            allowedCredential.setType("public-key");
            return allowedCredential;
        }).collect(Collectors.toList());
    }

    @Override
    public SuccessfulAuthenticationResponse authenticationUserHandlePost(String userHandle,
                                                                         AuthenticationRequest authenticationRequest) {
        String clientDataJson = authenticationRequest.getClientDataJson();
        ClientDataJson clientDataJsonObj = readClientDataJson(clientDataJson);
        validateClientDataJson(clientDataJsonObj);
        Optional<String> cacheChallenge = cacheService.get(getCacheKey(userHandle));
        if (cacheChallenge.isPresent() && cacheChallenge.get().equals(clientDataJsonObj.getChallenge())) {
            String authenticatorData = authenticationRequest.getAuthenticatorData();
            AuthenticatorData authData = AuthenticatorDataReader.read(authenticatorData.getBytes());
            byte[] credentialIdBytes = authData.getAttestedCredentialData().getCredentialId();
            String credentialId = Base64.encodeBase64URLSafeString(credentialIdBytes);
            Optional<Credential> optionalCredential = webauthnDataService.findById(credentialId);
            if (optionalCredential.isPresent()) {
                AuthenticatorAssertionResponse assertionResponse = AuthenticatorAssertionResponse.builder()
                        .base64Signature(authenticationRequest.getSignature())
                        .base64AuthenticatorData(authenticationRequest.getAuthenticatorData())
                        .base64UserHandle(Base64.encodeBase64String(userHandle.getBytes()))
                        .base64ClientDataJson(clientDataJson)
                        .build();
                Credential credential = optionalCredential.get();
                PublicKeyCredential publicKey = PublicKeyCredential.builder()
                        .keyType(credential.getPublicKeyType())
                        .encodedKeySpec(credential.getPublicKey())
                        .build();
                boolean isSignatureValid = SignatureVerifier.verifySignature(assertionResponse, cacheChallenge.get(), publicKey);
                if (isSignatureValid) {
                    SuccessfulAuthenticationResponse successResponse = new SuccessfulAuthenticationResponse();
                    successResponse.setAccessToken(tokenService.createToken(userHandle));
                    return successResponse;
                } else {
                    throw new InvalidRequestException("Not Allowed");
                }
            } else {
                throw new InvalidRequestException("No Matching Credential Found to verify this request.");
            }
        } else {
            throw new InvalidRequestException(ErrorDescriptions.INVALID_CHALLENGE);
        }
    }

    @NotNull
    private static String getCacheKey(String userHandle) {
        return userHandle + "_challenge";
    }

    private void validateClientDataJson(ClientDataJson clientDataJson) {
        if (!StringUtils.equals(clientDataJson.getOrigin(), "https://" + webAuthnProperties.getRpId())) {
            throw new InvalidRequestException(ErrorDescriptions.INVALID_ORIGIN);
        }
        if (!StringUtils.equals(clientDataJson.getType(), "webauthn.get")) {
            throw new InvalidRequestException(ErrorDescriptions.INVALID_CDJ_TYPE);
        }
    }

    private static ClientDataJson readClientDataJson(String clientDataJson) {
        ClientDataJson clientDataJsonObject;
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
