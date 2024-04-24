package com.harshalsharma.passkeydemo.backendserv.domain.webauthn.authentication;

import com.harshalsharma.passkeydemo.apispec.api.AuthenticationApi;
import com.harshalsharma.passkeydemo.apispec.api.AutoAuthenticationApi;
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
import com.harshalsharma.webauthncommons.authentication.AuthNDataValidator;
import com.harshalsharma.webauthncommons.authentication.InvalidAuthDataException;
import com.harshalsharma.webauthncommons.authentication.SignatureVerifier;
import com.harshalsharma.webauthncommons.clientdatajson.ClientDataJsonReader;
import com.harshalsharma.webauthncommons.clientdatajson.ClientDataJsonValidator;
import com.harshalsharma.webauthncommons.clientdatajson.InvalidClientDataJsonException;
import com.harshalsharma.webauthncommons.entities.AuthenticatorAssertionResponse;
import com.harshalsharma.webauthncommons.entities.ClientDataJson;
import com.harshalsharma.webauthncommons.publickey.PublicKeyCredential;
import jakarta.inject.Inject;
import org.apache.commons.codec.binary.Base64;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static com.harshalsharma.passkeydemo.backendserv.domain.UniqueStringGenerator.generateUUIDString;

@Component
public class WebAuthnAuthenticationService implements AuthenticationApi, AutoAuthenticationApi {

    private final WebauthnProperties webAuthnProperties;

    private final CacheService cacheService;

    private final WebauthnDataService webauthnDataService;

    private final TokenService tokenService;

    private final ClientDataJsonValidator clientDataJsonValidator;

    private final AuthNDataValidator authNDataValidator;

    @Inject
    public WebAuthnAuthenticationService(WebauthnProperties webAuthnProperties, CacheService cacheService,
                                         WebauthnDataService webauthnDataService, TokenService tokenService) {
        this.webAuthnProperties = webAuthnProperties;
        this.cacheService = cacheService;
        this.webauthnDataService = webauthnDataService;
        this.tokenService = tokenService;
        clientDataJsonValidator = ClientDataJsonValidator.builder()
                .operation(ClientDataJsonValidator.CDJType.WEBAUTHN_GET)
                .origin(webAuthnProperties.getOrigin())
                .build();
        authNDataValidator = AuthNDataValidator.builder()
                .rpId(webAuthnProperties.getRpId())
                .build();
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

    @Override
    public PublicKeyCredentialRequestOptionsResponse autoAuthenticationGet(Double latitude, Double longitude) {
        if (latitude == null || longitude == null) {
            throw new InvalidRequestException(ErrorDescriptions.INVALID_LOCATION);
        }
        PublicKeyCredentialRequestOptionsResponse optionsResponse = new PublicKeyCredentialRequestOptionsResponse();
        double radius = webAuthnProperties.getLocationSearchRadius();
        List<Credential> credentials = webauthnDataService.findByLocation(latitude, longitude, radius);

        if (CollectionUtils.isEmpty(credentials)) {
            throw new InvalidRequestException(ErrorDescriptions.NO_CREDENTIALS_FOUND);
        }

        Set<String> possibleUserHandles = credentials.stream().map(Credential::getUserId).collect(Collectors.toSet());

        optionsResponse.setAllowedCredentials(getAllowedCreds(credentials));
        optionsResponse.setUserVerification("preferred");
        optionsResponse.setRpId(webAuthnProperties.getRpId());
        String challenge = generateUUIDString();
        optionsResponse.setChallenge(challenge);

        //adds same challenge for each possible user.
        possibleUserHandles.forEach(userHandle -> {
            if (cacheService.get(getCacheKey(userHandle)).isEmpty()) {
                cacheService.put(getCacheKey(userHandle), challenge);
            }
        });

        return optionsResponse;
    }

    @Override
    public SuccessfulAuthenticationResponse authenticationUserHandlePost(String userHandle,
                                                                         AuthenticationRequest authenticationRequest) {
        //validate challenge.
        Optional<String> cacheChallenge = cacheService.get(getCacheKey(userHandle));
        if (cacheChallenge.isEmpty()) {
            throw new InvalidRequestException(ErrorDescriptions.INVALID_CHALLENGE);
        }

        //validate client data json and auth data
        validateClientDataJson(authenticationRequest.getClientDataJson(), cacheChallenge.get());
        validateAuthData(authenticationRequest.getAuthenticatorData());

        //validate credential
        Optional<Credential> optionalCredential = getCredential(authenticationRequest);
        if (optionalCredential.isEmpty()) {
            throw new InvalidRequestException(ErrorDescriptions.NO_CREDENTIALS_FOUND);
        }

        cacheService.remove(getCacheKey(userHandle));

        //validate signature
        boolean isSignatureValid = isSignatureValid(authenticationRequest,
                optionalCredential.get(), cacheChallenge.get());

        if (!isSignatureValid) {
            throw new InvalidRequestException(ErrorDescriptions.NOT_ALLOWED);
        }

        //grant token
        SuccessfulAuthenticationResponse successResponse = new SuccessfulAuthenticationResponse();
        successResponse.setAccessToken(tokenService.createToken(userHandle));
        return successResponse;
    }

    private void validateClientDataJson(String clientDataJson, String cacheChallenge) {
        try {
            ClientDataJson clientDataJsonObj = ClientDataJsonReader.read(clientDataJson);
            clientDataJsonValidator.validate(clientDataJsonObj, cacheChallenge);
        } catch (InvalidClientDataJsonException e) {
            throw new InvalidRequestException(ErrorDescriptions.INVALID_CLIENT_DATA_JSON, e);
        }
    }

    private List<AllowedCredential> getAllowedCreds(List<Credential> credentials) {
        return credentials.stream().map(cred -> {
            AllowedCredential allowedCredential = new AllowedCredential();
            allowedCredential.setId(cred.getCredentialId());
            allowedCredential.setType("public-key");
            return allowedCredential;
        }).collect(Collectors.toList());
    }

    /**
     * encodes credential id as base64 url safe and fetches the matching credential if present.
     *
     * @param authenticationRequest assertion request.
     * @return required credential entity object.
     */
    private Optional<Credential> getCredential(AuthenticationRequest authenticationRequest) {
        String credentialId = authenticationRequest.getCredentialId();
        credentialId = Base64.encodeBase64URLSafeString(Base64.decodeBase64(credentialId));
        return webauthnDataService.findById(credentialId);
    }

    private static boolean isSignatureValid(AuthenticationRequest authenticationRequest,
                                            Credential credential, String cacheChallenge) {
        AuthenticatorAssertionResponse assertionResponse = AuthenticatorAssertionResponse.builder()
                .base64Signature(authenticationRequest.getSignature())
                .base64AuthenticatorData(authenticationRequest.getAuthenticatorData())
                .base64ClientDataJson(authenticationRequest.getClientDataJson())
                .build();
        PublicKeyCredential publicKey = PublicKeyCredential.builder()
                .keyType(credential.getPublicKeyType())
                .encodedKeySpec(credential.getPublicKey())
                .build();
        return SignatureVerifier.verifySignature(assertionResponse, cacheChallenge, publicKey);
    }

    private void validateAuthData(String authData) {
        try {
            authNDataValidator.validate(authData);
        } catch (InvalidAuthDataException e) {
            throw new InvalidRequestException(ErrorDescriptions.INVALID_ASSERTION, e);
        }
    }

    @NotNull
    private static String getCacheKey(String userHandle) {
        return userHandle + "_challenge";
    }
}
