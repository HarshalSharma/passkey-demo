package com.harshalsharma.passkeydemo.backendserv.config;

import com.harshalsharma.passkeydemo.backendserv.exceptions.GenericExceptionHandler;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

@Component
@Configuration
public class JerseyConfig extends ResourceConfig {

    public JerseyConfig() {
        // Packages where your Jersey resources are located
        packages("com.harshalsharma.passkeydemo.apispec.api");
        packages("com.harshalsharma.passkeydemo.backendserv.config.filters");
        packages("com.harshalsharma.passkeydemo.backendserv.domain.webauthn.authentication");
        packages("com.harshalsharma.passkeydemo.backendserv.domain.webauthn.registration");
        register(new GenericExceptionHandler());
        property(ServletProperties.FILTER_FORWARD_ON_404, true);
    }

}