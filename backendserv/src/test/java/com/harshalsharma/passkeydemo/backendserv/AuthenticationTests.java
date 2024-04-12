package com.harshalsharma.passkeydemo.backendserv;

import com.harshalsharma.passkeydemo.apispec.api.AuthenticationApi;
import com.harshalsharma.passkeydemo.apispec.api.RegistrationApi;
import com.harshalsharma.passkeydemo.apispec.model.Error;
import com.harshalsharma.passkeydemo.apispec.model.PublicKeyCredentialCreationOptionsResponse;
import com.harshalsharma.passkeydemo.apispec.model.PublicKeyCredentialRequestOptionsResponse;
import com.harshalsharma.passkeydemo.apispec.model.RegistrationRequest;
import com.harshalsharma.passkeydemo.backendserv.data.cache.CacheService;
import com.harshalsharma.passkeydemo.backendserv.domain.webauthn.ErrorDescriptions;
import com.harshalsharma.passkeydemo.backendserv.domain.webauthn.WebauthnDataService;
import com.harshalsharma.passkeydemo.backendserv.domain.webauthn.WebauthnProperties;
import com.harshalsharma.passkeydemo.backendserv.domain.webauthn.entities.Credential;
import com.harshalsharma.passkeydemo.backendserv.exceptions.InvalidRequestException;
import com.harshalsharma.webauthncommons.attestationObject.AttestationObjectReader;
import org.apache.commons.lang3.RandomStringUtils;
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

    public static final String WEBAUTHN_AUTHENTICATE_TYPE = "webauthn.get";
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
    @DisplayName("Credential Creation Options Tests")
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
            request.setClientDataJson(CommonUtils.createClientDataJson(creationOptionsResponse, webauthnProperties.getRpId()));
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

}
