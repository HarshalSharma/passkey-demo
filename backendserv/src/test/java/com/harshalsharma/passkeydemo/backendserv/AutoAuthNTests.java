package com.harshalsharma.passkeydemo.backendserv;

import com.harshalsharma.passkeydemo.apispec.api.AuthenticationApi;
import com.harshalsharma.passkeydemo.apispec.model.AllowedCredential;
import com.harshalsharma.passkeydemo.apispec.model.AuthenticationRequest;
import com.harshalsharma.passkeydemo.apispec.model.Preferences;
import com.harshalsharma.passkeydemo.apispec.model.PublicKeyCredentialRequestOptionsResponse;
import com.harshalsharma.passkeydemo.backendserv.data.cache.CacheService;
import com.harshalsharma.passkeydemo.backendserv.domain.webauthn.WebauthnDataService;
import com.harshalsharma.passkeydemo.backendserv.domain.webauthn.entities.Credential;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.test.context.TestPropertySource;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.Random;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(locations = "classpath:application.properties",
        properties = "spring.datasource.url=jdbc:h2:mem:testdb")
public class AutoAuthNTests {

    @Autowired
    private AuthenticationApi authenticationApi;

    @Autowired
    private WebauthnDataService webauthnDataService;

    @Autowired
    private CacheService cacheService;

    private String accessToken;

    private String credentialId;

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @BeforeEach
    void setup() {
        if (StringUtils.isBlank(accessToken)) {
            //given existing credential for user:
            String userHandle = "ZGO0yp6G/apFbyZetyMtog==";
            credentialId = Base64.encodeBase64URLSafeString(
                    Base64.decodeBase64("DCkEcAIHZOuElhKEoaYMoiMABC0KzteoC4KilQIQNW0="));
            Credential credential = Credential.builder()
                    .credentialId(credentialId)
                    .userId(userHandle)
                    .publicKey("MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEr8absn5ASvV/avoUdx0ND8j/EgAOx7oUzXU+Qc/fe4mTiEJXUIg/vYmIiy2nHKS7ZQGL8zKd9AdfMyRGNInBUA==")
                    .publicKeyType("EC").build();
            webauthnDataService.save(credential);
            cacheService.put(userHandle + "_challenge", Base64.encodeBase64("hello".getBytes()));
            AuthenticationRequest request = CommonUtils.getValidAuthNRequest(credentialId);
            accessToken = authenticationApi.authenticationUserHandlePost(userHandle, request).getAccessToken();
        }
    }

    @DisplayName("Auto-Authentication must return the credential when location is same.")
    @Test()
    void testAutoAuthNReturnsValidCredential() {
        //given registered location.
        Random random = new Random();
        double latitude = random.nextDouble(100);
        double longitude = random.nextDouble(100);
        updatePreferences(latitude, longitude);

        //when generate AuthN options for location
        String url = "http://localhost:" + port + "/auto-authentication?latitude=" + latitude + "&longitude=" + longitude;
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        HttpEntity<?> httpEntity = new HttpEntity<>(headers);
        ResponseEntity<PublicKeyCredentialRequestOptionsResponse> getEntity = restTemplate.exchange(url, HttpMethod.GET, httpEntity, PublicKeyCredentialRequestOptionsResponse.class);

        //then
        Assertions.assertEquals(200, getEntity.getStatusCode().value());
        PublicKeyCredentialRequestOptionsResponse body = getEntity.getBody();
        Assertions.assertNotNull(body);
        Assertions.assertFalse(CollectionUtils.isEmpty(body.getAllowedCredentials()));
        boolean result = body.getAllowedCredentials().stream()
                .map(AllowedCredential::getId).anyMatch(id -> id.equals(credentialId));
        Assertions.assertTrue(result);
    }

    private void updatePreferences(double latitude, double longitude) {
        String url = "http://localhost:" + port + "/preferences";
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setContentType(MediaType.APPLICATION_JSON);
        Preferences preferences = new Preferences();
        preferences.setHomeLat(BigDecimal.valueOf(latitude));
        preferences.setHomeLog(BigDecimal.valueOf(longitude));
        HttpEntity<Preferences> httpEntity = new HttpEntity<>(preferences, headers);
        ResponseEntity<String> postEntity = restTemplate.exchange(url, HttpMethod.PUT, httpEntity, String.class);
        Assertions.assertEquals(204, postEntity.getStatusCode().value());
    }
}
