package com.harshalsharma.passkeydemo.backendserv;

import com.harshalsharma.passkeydemo.apispec.api.RegistrationApi;
import com.harshalsharma.passkeydemo.backendserv.data.cache.CacheService;
import com.harshalsharma.passkeydemo.backendserv.domain.webauthn.WebauthnDataService;
import com.harshalsharma.passkeydemo.backendserv.domain.webauthn.WebauthnProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(locations = "classpath:application.properties",
        properties = "spring.datasource.url=jdbc:h2:mem:testdb")
public class AuthenticationTests {

    public static final String WEBAUTHN_AUTHENTICATE_TYPE = "webauthn.get";
    @Autowired
    private RegistrationApi registrationApi;

    @Autowired
    private WebauthnProperties webauthnProperties;

    @Autowired
    private CacheService cacheService;

    @Autowired
    private WebauthnDataService webauthnDataService;



}
