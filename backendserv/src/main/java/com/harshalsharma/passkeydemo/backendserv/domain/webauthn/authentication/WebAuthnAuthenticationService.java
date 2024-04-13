package com.harshalsharma.passkeydemo.backendserv.domain.webauthn.authentication;

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
import com.harshalsharma.webauthncommons.clientdatajson.ClientDataJsonReader;
import com.harshalsharma.webauthncommons.clientdatajson.ClientDataJsonValidator;
import com.harshalsharma.webauthncommons.clientdatajson.InvalidClientDataJsonException;
import com.harshalsharma.webauthncommons.entities.AuthenticatorAssertionResponse;
import com.harshalsharma.webauthncommons.entities.AuthenticatorData;
import com.harshalsharma.webauthncommons.entities.ClientDataJson;
import com.harshalsharma.webauthncommons.io.DataEncoderDecoder;
import com.harshalsharma.webauthncommons.io.MessageUtils;
import com.harshalsharma.webauthncommons.publickey.PublicKeyCredential;
import jakarta.inject.Inject;
import org.apache.commons.codec.binary.Base64;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.Arrays;
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

    private final ClientDataJsonValidator clientDataJsonValidator;

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
    public SuccessfulAuthenticationResponse authenticationUserHandlePost(String userHandle,
                                                                         AuthenticationRequest authenticationRequest) {
        //validate challenge.
        Optional<String> cacheChallenge = cacheService.get(getCacheKey(userHandle));
        if (cacheChallenge.isEmpty()) {
            throw new InvalidRequestException(ErrorDescriptions.INVALID_CHALLENGE);
        }

        String clientDataJson = authenticationRequest.getClientDataJson();
        validateClientDataJson(clientDataJson, cacheChallenge.get());

        AuthenticatorData authenticatorData = AuthenticatorDataReader
                .read(DataEncoderDecoder.decodeBase64Bytes(authenticationRequest.getAuthenticatorData()));

        Arrays.equals(MessageUtils.hashSHA256(webAuthnProperties.getRpId().getBytes()), authenticatorData.getRpIdHash());
//        System.out.println("RP_ID_HASH_COMPARION" + equals);

        byte flags = authenticatorData.getFlags();
        /**
         * Bit 0, User Presence (UP): If set (i.e., to 1), the authenticator validated that the user was present through some Test of User Presence (TUP), such as touching a button on the authenticator.
         * Bit 2, User Verification (UV): If set, the authenticator verified the actual user through a biometric, PIN, or other method.
         */
        int BIT_USER_PRESENCE = 1;
        int BIT_USER_VERIFICATION = 1 << 2;
        if ((flags & BIT_USER_PRESENCE) != 0) {
            System.out.println("UP BIT IS SET");
        } else if ((flags & BIT_USER_VERIFICATION) != 0) {
            System.out.println("UV BIT IS SET");
        } else {
            System.out.println("NO UV OR UP BIT IS SET");
        }

        //validate credential
        Optional<Credential> optionalCredential = getCredential(authenticationRequest);
        if (optionalCredential.isEmpty()) {
            throw new InvalidRequestException(ErrorDescriptions.NO_CREDENTIALS_FOUND);
        }

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

    private Optional<Credential> getCredential(AuthenticationRequest authenticationRequest) {
        String authenticatorData = authenticationRequest.getAuthenticatorData();
        AuthenticatorData authData = AuthenticatorDataReader.read(authenticatorData.getBytes());
        byte[] credentialIdBytes = authData.getAttestedCredentialData().getCredentialId();
        String credentialId = Base64.encodeBase64URLSafeString(credentialIdBytes);
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

    @NotNull
    private static String getCacheKey(String userHandle) {
        return userHandle + "_challenge";
    }

}
