package com.harshalsharma.passkeydemo.backendserv;

import com.harshalsharma.passkeydemo.apispec.api.RegistrationApi;
import com.harshalsharma.passkeydemo.apispec.model.Error;
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
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.RandomStringUtils;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@TestPropertySource(locations = "classpath:application.properties",
        properties = "spring.datasource.url=jdbc:h2:mem:testdb")
public class RegistrationTests {

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
    class CredentialCreationOptionsTests {
        @Test
        @DisplayName("During Registration, Relying party details must be same as per configuration.")
        void registrationGetMustReturnConfiguredRp() {
            //given
            String rpId = webauthnProperties.getRpId();
            String rpName = webauthnProperties.getRpName();

            //when
            PublicKeyCredentialCreationOptionsResponse creationOptions = registrationApi.registrationGet();

            //then
            assertEquals(rpId, creationOptions.getRpId(), "RP Id must match.");
            assertEquals(rpName, creationOptions.getRpName(), "RP Name must match.");
        }

        @Test
        @DisplayName("Registration options must only have configured public key algorithms.")
        void registrationGetMustReturnValidPublicKeyAlg() {
            //given
            List<Integer> allowedAlgs = webauthnProperties.getSupportedPublicKeyAlgs();

            //when
            PublicKeyCredentialCreationOptionsResponse creationOptions = registrationApi.registrationGet();

            //then
            List<PublicKeyCredentialParam> pubKeyCredParams = creationOptions.getPubKeyCredParams();
            assertTrue(pubKeyCredParams.stream().map(PublicKeyCredentialParam::getType).allMatch("public-key"::equals));
            assertTrue(pubKeyCredParams.stream().map(PublicKeyCredentialParam::getAlg)
                    .map(BigDecimal::intValue).collect(Collectors.toSet())
                    .containsAll(allowedAlgs), "allowed algs must match.");
        }

        @Test
        @DisplayName("A Challenge should be generated as an UUID and must also exist in cache.")
        void challengeMustBeGeneratedAsUUIDInCacheAndShared() {
            //given
            PublicKeyCredentialCreationOptionsResponse creationOptions = registrationApi.registrationGet();

            //when
            String userId = creationOptions.getUserId();
            String challenge = creationOptions.getChallenge();
            Optional<String> cacheChallenge = cacheService.get(userId + "_challenge");

            //then
            assertTrue(cacheChallenge.isPresent());
            assertEquals(UUID.fromString(challenge).toString(), cacheChallenge.get(),
                    "UUID Challenge must be same as present in cache.");
        }

        @Test
        @DisplayName("A randomly generated UUID is set as userId, username and display name is Passkey-Demo.")
        void testUserIdUserNameAndDisplayNameValue() {
            //given
            String defaultDisplayName = "PassKey-Demo";
            PublicKeyCredentialCreationOptionsResponse creationOptions = registrationApi.registrationGet();

            //when
            String userId = creationOptions.getUserId();
            String userName = creationOptions.getUserName();
            String displayName = creationOptions.getDisplayName();

            //then
            assertEquals(UUID.fromString(userId).toString(), userId,
                    "UserId must be an valid UUID");
            assertEquals(userId, userName, "By default username is same as userid");
            assertEquals(defaultDisplayName, displayName, "display name has the default value.");
        }
    }

    @Nested
    @DisplayName("Registration POST Tests")
    class RegistrationPostTests {

        @Test
        @DisplayName("When UserHandle is absent, then it is invalid request")
        void testAbsentUserHandleIsInvalidRequest() {
            //given
            RegistrationRequest request = new RegistrationRequest();

            try {
                //when
                registrationApi.registrationPost(request);
                fail("exception expected above");
            } catch (InvalidRequestException e) {
                //then
                assertEquals(400, e.getStatus());
                Error entity = e.getError();
                assertInstanceOf(Error.class, entity);
                assertEquals(ErrorDescriptions.INVALID_USER_HANDLE, entity.getDescription());
            }
        }

        @Test
        @DisplayName("On post, if attestation object is unreadable, then it is invalid request")
        void testAttestationObjectIsNotInvalid() {
            //given
            String attestationObject = RandomStringUtils.randomAlphabetic(20);

            try {
                //when
                RegistrationRequest request = new RegistrationRequest();
                request.setUserHandle(RandomStringUtils.randomAlphanumeric(10));
                request.setAttestationObject(attestationObject);
                registrationApi.registrationPost(request);
                fail("exception expected above");
            } catch (InvalidRequestException e) {
                //then
                assertEquals(400, e.getStatus());
                Error entity = e.getError();
                assertInstanceOf(Error.class, entity);
                assertEquals(ErrorDescriptions.INVALID_ATTESTATION_OBJECT, entity.getDescription());
            }
        }

        @Test
        @DisplayName("When clientDataJson is absent, then it is invalid request")
        void testClientDataJsonIsPresent() {
            //given
            String attestationObject = CommonUtils.getValidAttestationObjectString();

            try {
                //when
                RegistrationRequest request = new RegistrationRequest();
                request.setUserHandle(RandomStringUtils.randomAlphanumeric(10));
                request.setAttestationObject(attestationObject);
                registrationApi.registrationPost(request);
                fail("exception expected above");
            } catch (InvalidRequestException e) {
                //then
                assertEquals(400, e.getStatus());
                Error entity = e.getError();
                assertInstanceOf(Error.class, entity);
                assertEquals(ErrorDescriptions.INVALID_VALUE_FOR_CLIENT_DATA_JSON, entity.getDescription());
            }
        }

        @Test
        @DisplayName("When clientDataJson type is not webauthn.create, then it is invalid request")
        void testClientDataJsonTypeIsCreate() {
            //given
            String attestationObject = CommonUtils.getValidAttestationObjectString();
            String clientDataJson = CommonUtils.createClientDataJson("1234",
                    CommonUtils.createValidOrigin(webauthnProperties.getRpId()),
                    RandomStringUtils.randomAlphanumeric(5));
            try {
                //when
                RegistrationRequest request = new RegistrationRequest();
                request.setUserHandle(RandomStringUtils.randomAlphanumeric(10));
                request.setAttestationObject(attestationObject);
                request.setClientDataJson(clientDataJson);
                registrationApi.registrationPost(request);
                fail("exception expected above");
            } catch (InvalidRequestException e) {
                //then
                assertEquals(400, e.getStatus());
                Error entity = e.getError();
                assertInstanceOf(Error.class, entity);
                assertEquals(ErrorDescriptions.INVALID_CDJ_TYPE, entity.getDescription());
            }
        }

        @Test
        @DisplayName("When clientDataJson origin is not valid, then it is invalid request")
        void testClientDataJsonOriginIsInvalid() {
            //given
            String attestationObject = CommonUtils.getValidAttestationObjectString();
            String clientDataJson = CommonUtils.createClientDataJson("1234", RandomStringUtils.randomAlphanumeric(5),
                    CommonUtils.WEBAUTHN_CREATE_TYPE);
            try {
                //when
                RegistrationRequest request = new RegistrationRequest();
                request.setUserHandle(RandomStringUtils.randomAlphanumeric(10));
                request.setAttestationObject(attestationObject);
                request.setClientDataJson(clientDataJson);
                registrationApi.registrationPost(request);
                fail("exception expected above");
            } catch (InvalidRequestException e) {
                //then
                assertEquals(400, e.getStatus());
                Error entity = e.getError();
                assertInstanceOf(Error.class, entity);
                assertEquals(ErrorDescriptions.INVALID_ORIGIN, entity.getDescription());
            }
        }

        @Test
        @DisplayName("When clientDataJson is present and challenge is missing in cache, then it is invalid request")
        void testClientDataJsonChallengeIsInvalid() {
            //given
            String challenge = RandomStringUtils.randomAlphanumeric(10);
            String attestationObject = CommonUtils.getValidAttestationObjectString();
            String clientDataJson = CommonUtils.createClientDataJson(challenge,
                    CommonUtils.createValidOrigin(webauthnProperties.getRpId()),
                    CommonUtils.WEBAUTHN_CREATE_TYPE);

            try {
                //when
                RegistrationRequest request = new RegistrationRequest();
                request.setUserHandle(RandomStringUtils.randomAlphanumeric(10));
                request.setAttestationObject(attestationObject);
                request.setClientDataJson(clientDataJson);
                registrationApi.registrationPost(request);
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
        @DisplayName("On post, if attestation object and client-data-json is valid, then credential must be stored to db.")
        void testAttestationObjectIsReadable() {
            //given
            PublicKeyCredentialCreationOptionsResponse creationOptionsResponse = registrationApi.registrationGet();
            String attestationObject = CommonUtils.getValidAttestationObjectString();
            AttestationObjectExplorer objectExplorer = AttestationObjectReader.read(attestationObject.trim());
            String publicKey = objectExplorer.getEncodedPublicKeySpec();
            String publicKeyType = objectExplorer.getKeyType();
            String credentialId = objectExplorer.getWebauthnId();

            //when
            RegistrationRequest request = new RegistrationRequest();
            request.setAttestationObject(attestationObject.trim());
            request.setClientDataJson(CommonUtils.createClientDataJson(creationOptionsResponse, webauthnProperties.getRpId()));
            request.setUserHandle(creationOptionsResponse.getUserId());
            registrationApi.registrationPost(request);

            //then
            Optional<Credential> optionalCredential = webauthnDataService.findById(credentialId);
            assertTrue(optionalCredential.isPresent(), "Credential must be saved to the database.");
            Credential credential = optionalCredential.get();
            assertEquals(publicKey, credential.getPublicKey(), "Credential's public key must match.");
            assertEquals(publicKeyType, credential.getPublicKeyType(), "Credential's public key type must match.");
            assertEquals(credentialId, credential.getCredentialId(), "Credential's credential id must match.");
            assertEquals(creationOptionsResponse.getUserId(), credential.getUserId(), "user handle must match.");

        }


    }

}