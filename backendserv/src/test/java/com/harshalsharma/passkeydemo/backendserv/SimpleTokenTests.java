package com.harshalsharma.passkeydemo.backendserv;

import com.harshalsharma.passkeydemo.apispec.api.AuthenticationApi;
import com.harshalsharma.passkeydemo.apispec.model.AuthenticationRequest;
import com.harshalsharma.passkeydemo.apispec.model.SimpleNote;
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

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(locations = "classpath:application.properties",
        properties = "spring.datasource.url=jdbc:h2:mem:testdb")
public class SimpleTokenTests {

    @Autowired
    private AuthenticationApi authenticationApi;

    @Autowired
    private WebauthnDataService webauthnDataService;

    @Autowired
    private CacheService cacheService;

    private String accessToken;

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @BeforeEach
    void setup() {
        if (StringUtils.isBlank(accessToken)) {
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
            cacheService.put(userHandle + "_challenge", Base64.encodeBase64("hello".getBytes()));
            AuthenticationRequest request = CommonUtils.getValidAuthNRequest(credentialId);
            accessToken = authenticationApi.authenticationUserHandlePost(userHandle, request).getAccessToken();
        }
    }

    @Test
    @DisplayName("Without token, call is not allowed.")
    void testWithoutTokenIsBad() {
        //when
        ResponseEntity<String> getNotes = this.restTemplate.getForEntity("http://localhost:" + port + "/notes", String.class);

        //then
        Assertions.assertEquals(401, getNotes.getStatusCode().value());
    }

    @Test
    @DisplayName("With valid token, call is allowed")
    void testWithTokenIsGood() {
        //given
        String url = "http://localhost:" + port + "/notes";
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        HttpEntity<String> httpEntity = new HttpEntity<>("", headers);

        //when
        ResponseEntity<String> responseEntity = restTemplate.exchange(url, HttpMethod.GET, httpEntity, String.class);

        //then
        Assertions.assertEquals(200, responseEntity.getStatusCode().value());
    }

    @Test
    @DisplayName("With valid token, updating note is allowed")
    void testWithTokenUpdateNotesIsAllowed() {
        //given
        String url = "http://localhost:" + port + "/notes";
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setContentType(MediaType.APPLICATION_JSON);
        SimpleNote note = new SimpleNote();
        HttpEntity<SimpleNote> httpEntity = new HttpEntity<>(note, headers);

        //when
        ResponseEntity<String> postEntity = restTemplate.exchange(url, HttpMethod.PUT, httpEntity, String.class);
        ResponseEntity<SimpleNote> getEntity = restTemplate.exchange(url, HttpMethod.GET, httpEntity, SimpleNote.class);

        //then
        Assertions.assertEquals(204, postEntity.getStatusCode().value());
        Assertions.assertEquals(200, getEntity.getStatusCode().value());
        SimpleNote body = getEntity.getBody();
        Assertions.assertNotNull(body);
        Assertions.assertEquals(note.getNote(), body.getNote());
    }
}
