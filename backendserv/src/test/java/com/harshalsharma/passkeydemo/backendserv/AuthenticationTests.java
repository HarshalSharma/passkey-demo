package com.harshalsharma.passkeydemo.backendserv;

import com.harshalsharma.passkeydemo.apispec.api.AuthenticationApi;
import com.harshalsharma.passkeydemo.apispec.api.RegistrationApi;
import com.harshalsharma.passkeydemo.apispec.model.Error;
import com.harshalsharma.passkeydemo.apispec.model.*;
import com.harshalsharma.passkeydemo.backendserv.data.cache.CacheService;
import com.harshalsharma.passkeydemo.backendserv.domain.webauthn.ErrorDescriptions;
import com.harshalsharma.passkeydemo.backendserv.domain.webauthn.WebauthnDataService;
import com.harshalsharma.passkeydemo.backendserv.domain.webauthn.WebauthnProperties;
import com.harshalsharma.passkeydemo.backendserv.domain.webauthn.entities.Credential;
import com.harshalsharma.passkeydemo.backendserv.exceptions.InvalidRequestException;
import com.harshalsharma.webauthncommons.attestationObject.AttestationObjectReader;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.RandomStringUtils;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.util.CollectionUtils;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@TestPropertySource(locations = "classpath:application.properties",
        properties = "spring.datasource.url=jdbc:h2:mem:testdb")
public class AuthenticationTests {

    @Autowired
    private AuthenticationApi authenticationApi;
    @Autowired
    private RegistrationApi registrationApi;

    @Autowired
    private WebauthnProperties webauthnProperties;

    @Autowired
    private CacheService cacheService;

    @Autowired
    private WebauthnDataService webauthnDataService;

    @Nested
    @DisplayName("Credential AuthN Options Tests")
    class CredentialAuthenticationOptionsTests {

        @Test
        @DisplayName("When there is no registered credential, authentication options is a Invalid Request.")
        void authenticationGetWithoutRegisterIsInvalidRequest() {
            //given
            String userHandle = RandomStringUtils.randomAlphanumeric(10);

            try {
                //when
                authenticationApi.authenticationUserHandleGet(userHandle);
                fail("exception expected above");
            } catch (InvalidRequestException e) {
                //then
                assertEquals(400, e.getStatus());
                Error entity = e.getError();
                assertInstanceOf(Error.class, entity);
                assertEquals(ErrorDescriptions.NO_CREDENTIALS_FOUND, entity.getDescription());
            }
        }

        @Test
        @DisplayName("On Authentication Options, Previously Registered Credential Id must be present. ")
        void authenticationGetMustReturnRegisteredCredentials() {
            //given
            PublicKeyCredentialCreationOptionsResponse creationOptionsResponse = registrationApi.registrationGet();
            String userHandle = creationOptionsResponse.getUserId();
            String attestationObject = CommonUtils.getValidAttestationObjectString();
            String webauthnId = AttestationObjectReader.read(attestationObject).getWebauthnId();
            RegistrationRequest request = new RegistrationRequest();
            request.setAttestationObject(attestationObject.trim());
            request.setClientDataJson(CommonUtils.createClientDataJson(creationOptionsResponse, webauthnProperties.getOrigin()));
            request.setUserHandle(userHandle);
            registrationApi.registrationPost(request);

            //when
            PublicKeyCredentialRequestOptionsResponse authOptions =
                    authenticationApi.authenticationUserHandleGet(userHandle);

            //then
            assertFalse(CollectionUtils.isEmpty(authOptions.getAllowedCredentials()), "Credentials list must not be empty.");
            assertEquals(1, authOptions.getAllowedCredentials().size(), "Registered credentials size must match.");
            assertEquals(webauthnId, authOptions.getAllowedCredentials().get(0).getId(), "Registered credential must match");
            assertEquals("public-key", authOptions.getAllowedCredentials().get(0).getType(), "Cred Type must match.");
            assertEquals("preferred", authOptions.getUserVerification());
            assertEquals(webauthnProperties.getRpId(), authOptions.getRpId());
        }


        @Test
        @DisplayName("A Challenge should be generated as an UUID and must also exist in cache.")
        void challengeMustBeGeneratedAsUUIDInCacheAndShared() {
            //given
            String userHandle = RandomStringUtils.randomAlphanumeric(10);
            storeRandomCred(userHandle);

            //when
            Optional<String> cacheChallengePreOptions = getCacheChallenge(userHandle);
            PublicKeyCredentialRequestOptionsResponse optionsResponse = authenticationApi.authenticationUserHandleGet(userHandle);

            //then
            Optional<String> cacheChallenge = getCacheChallenge(userHandle);
            assertFalse(cacheChallengePreOptions.isPresent());
            assertTrue(cacheChallenge.isPresent());
            assertEquals(UUID.fromString(cacheChallenge.get()).toString(), optionsResponse.getChallenge(),
                    "UUID Challenge must be same as present in cache.");
        }
    }

    @Nested
    @DisplayName("Credential AuthN Tests")
    class CredentialAuthenticationTests {

        @Test
        @DisplayName("When Pre-Login challenge is not available in cache, any login call is invalid without it.")
        void challengeMustBeValid() {
            //given
            String userHandle = createRandomRegistration();
            String clientDataJson = CommonUtils.createClientDataJson(RandomStringUtils.randomAlphanumeric(5),
                    webauthnProperties.getOrigin(), CommonUtils.WEBAUTHN_GET_TYPE);

            try {
                //when
                AuthenticationRequest request = new AuthenticationRequest();
                request.setClientDataJson(clientDataJson);
                authenticationApi.authenticationUserHandlePost(userHandle, request);
                fail("exception expected above");
            } catch (InvalidRequestException e) {
                //then
                assertEquals(400, e.getStatus());
                Error entity = e.getError();
                assertInstanceOf(Error.class, entity);
                assertEquals(ErrorDescriptions.INVALID_CHALLENGE, entity.getDescription());
            }
        }

        @Test
        @DisplayName("During login, client data json type must be webauthn.get")
        void webauthnTypeMustBeValid() {
            //given
            String userHandle = createRandomRegistration();
            PublicKeyCredentialRequestOptionsResponse optionsResponse = authenticationApi.authenticationUserHandleGet(userHandle);
            String clientDataJson = CommonUtils.createClientDataJson(optionsResponse.getChallenge(),
                    webauthnProperties.getOrigin(), RandomStringUtils.randomAlphanumeric(5));

            try {
                //when
                AuthenticationRequest request = new AuthenticationRequest();
                request.setClientDataJson(clientDataJson);
                authenticationApi.authenticationUserHandlePost(userHandle, request);
                fail("exception expected above");
            } catch (InvalidRequestException e) {
                //then
                assertEquals(400, e.getStatus());
                Error entity = e.getError();
                assertInstanceOf(Error.class, entity);
                assertEquals(ErrorDescriptions.INVALID_CLIENT_DATA_JSON, entity.getDescription());
            }
        }

        @Test
        @DisplayName("On valid Request, Authentication must generate token.")
        void testTokenIsGenerated() {
            //given existing credential for user:
            String userHandle = "ZGO0yp6G/apFbyZetyMtog==";
            String credentialId = Base64.encodeBase64URLSafeString(
                    Base64.decodeBase64("DCkEcAIHZOuElhKEoaYMoiMABC0KzteoC4KilQIQNW0="));
            Credential credential = Credential.builder()
                    .credentialId(credentialId)
                    .userId(userHandle)
                    .publicKey("MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEr8absn5ASvV/avoUdx0ND8j/EgAOx7oUzXU+Qc/fe4mTiEJXUIg/vYmIiy2nHKS7ZQGL8zKd9AdfMyRGNInBUA==")
                    .publicKeyType("EC").build();
            webauthnDataService.save(credential);

            //when
            cacheService.put(userHandle + "_challenge", Base64.encodeBase64("hello".getBytes()));
            AuthenticationRequest request = CommonUtils.getValidAuthNRequest(credentialId);
            SuccessfulAuthenticationResponse successfulAuthenticationResponse = authenticationApi.authenticationUserHandlePost(userHandle, request);

            //then
            assertNotNull(successfulAuthenticationResponse);
            assertNotNull(successfulAuthenticationResponse.getAccessToken());
        }
    }

    private Optional<String> getCacheChallenge(String userHandle) {
        return cacheService.get(userHandle + "_challenge");
    }

    private void storeRandomCred(String userHandle) {
        String id = RandomStringUtils.randomAlphanumeric(10);
        Credential testCred = Credential.builder()
                .credentialId(id).userId(userHandle)
                .publicKeyType("EC").publicKey(RandomStringUtils.random(10)).build();
        webauthnDataService.save(testCred);
    }

    private String createRandomRegistration() {
        PublicKeyCredentialCreationOptionsResponse creationOptionsResponse = registrationApi.registrationGet();
        String userHandle = creationOptionsResponse.getUserId();
        String attestationObject = CommonUtils.getValidAttestationObjectString();
        String clientDataJson = CommonUtils.createClientDataJson(creationOptionsResponse, webauthnProperties.getOrigin());
        RegistrationRequest request = new RegistrationRequest();
        request.setAttestationObject(attestationObject.trim());
        request.setClientDataJson(clientDataJson);
        request.setUserHandle(userHandle);
        registrationApi.registrationPost(request);
        return userHandle;
    }
}
