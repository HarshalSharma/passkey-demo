package com.harshalsharma.passkeydemo.backendserv.config;

import com.harshalsharma.passkeydemo.backendserv.config.filters.AuthFilter;
import com.harshalsharma.passkeydemo.backendserv.config.filters.CORSFilter;
import com.harshalsharma.passkeydemo.backendserv.config.filters.GenericFilter;
import com.harshalsharma.passkeydemo.backendserv.exceptions.GenericExceptionHandler;
import jakarta.inject.Inject;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

@Component
@Configuration
public class JerseyConfig extends ResourceConfig {

    @Inject
    public JerseyConfig(GenericExceptionHandler genericExceptionHandler,
                        CORSFilter corsFilter, AuthFilter authFilter,
                        GenericFilter genericFilter) {
        // Packages where your Jersey resources are located
        packages("com.harshalsharma.passkeydemo.apispec.api");
        packages("com.harshalsharma.passkeydemo.backendserv.config.filters");
        packages("com.harshalsharma.passkeydemo.backendserv.domain.webauthn.authentication");
        packages("com.harshalsharma.passkeydemo.backendserv.domain.webauthn.registration");
        packages("com.harshalsharma.passkeydemo.backendserv.exceptions");
        register(authFilter);
        register(genericExceptionHandler);
        register(corsFilter);
        register(genericFilter);
        property(ServletProperties.FILTER_FORWARD_ON_404, true);
    }

}