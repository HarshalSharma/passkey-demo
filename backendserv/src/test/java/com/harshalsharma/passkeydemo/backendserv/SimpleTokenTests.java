package com.harshalsharma.passkeydemo.backendserv;

import com.harshalsharma.passkeydemo.apispec.api.AuthenticationApi;
import com.harshalsharma.passkeydemo.apispec.model.AuthenticationRequest;
import com.harshalsharma.passkeydemo.apispec.model.SuccessfulAuthenticationResponse;
import com.harshalsharma.passkeydemo.backendserv.data.cache.CacheService;
import com.harshalsharma.passkeydemo.backendserv.domain.webauthn.WebauthnDataService;
import com.harshalsharma.passkeydemo.backendserv.domain.webauthn.entities.Credential;
import org.apache.commons.codec.binary.Base64;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
@TestPropertySource(locations = "classpath:application.properties",
        properties = "spring.datasource.url=jdbc:h2:mem:testdb")
public class SimpleTokenTests {

    @Autowired
    private AuthenticationApi authenticationApi;

    @Autowired
    private CacheService cacheService;

    @Autowired
    private WebauthnDataService webauthnDataService;

    @Test
    @DisplayName("On Generated Token, Post Authentication it must be present in cache,")
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
        System.out.println();
    }
}
