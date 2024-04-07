package com.harshalsharma.passkeydemo.backendserv;

import com.harshalsharma.passkeydemo.apispec.api.RegistrationApi;
import com.harshalsharma.passkeydemo.apispec.model.Error;
import com.harshalsharma.passkeydemo.apispec.model.PublicKeyCredentialCreationOptionsResponse;
import com.harshalsharma.passkeydemo.apispec.model.PublicKeyCredentialParam;
import com.harshalsharma.passkeydemo.apispec.model.RegistrationRequest;
import com.harshalsharma.passkeydemo.backendserv.data.cache.CacheService;
import com.harshalsharma.passkeydemo.backendserv.domain.webauthn.WebauthnProperties;
import com.harshalsharma.passkeydemo.backendserv.exceptions.InvalidRequestException;
import org.apache.commons.lang3.RandomStringUtils;
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
@TestPropertySource(locations = "classpath:application.properties")
public class RegistrationTests {

    @Autowired
    private RegistrationApi registrationApi;

    @Autowired
    private WebauthnProperties webauthnProperties;

    @Autowired
    private CacheService cacheService;

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
        @DisplayName("On post, if attestation object is unreadable, then it is invalid request")
        void testAttestationObjectIsNotInvalid() {
            //given
            String attestationObject = RandomStringUtils.randomAlphabetic(20);

            try {
                //when
                RegistrationRequest request = new RegistrationRequest();
                request.setAttestationObject(attestationObject);
                registrationApi.registrationPost(request);
                fail("exception expected above");
            } catch (InvalidRequestException e) {
                //then
                assertEquals(400, e.getStatus());
                Object entity = e.getError();
                assertInstanceOf(Error.class, entity);
                Error error = (Error) entity;
                assertEquals("Invalid Attestation Object", error.getDescription());
            }
        }

//        @Test
//        @DisplayName("On post, if attestation object is valid, then public key must be stored to db.")
//        void testAttestationObjectIsReadable() {
//            //given
//            String attestationObject =
//
//            try {
//                //when
//                RegistrationRequest request = new RegistrationRequest();
//                request.setAttestationObject(attestationObject);
//                registrationApi.registrationPost(request);
//                fail("exception expected above");
//            } catch (InvalidRequestException e) {
//                //then
//                assertEquals(400, e.getStatus());
//                Object entity = e.getError();
//                assertInstanceOf(Error.class, entity);
//                Error error = (Error) entity;
//                assertEquals("Invalid Attestation Object", error.getDescription());
//            }
//        }

    }

}