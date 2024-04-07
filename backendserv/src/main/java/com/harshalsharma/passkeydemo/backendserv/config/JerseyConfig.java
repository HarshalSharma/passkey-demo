package com.harshalsharma.passkeydemo.backendserv.config;

import com.harshalsharma.passkeydemo.backendserv.config.filters.CORSFilter;
import com.harshalsharma.passkeydemo.backendserv.config.filters.GenericFilter;
import com.harshalsharma.passkeydemo.backendserv.exceptions.GenericExceptionHandler;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletProperties;
import org.springframework.stereotype.Component;

@Component
public class JerseyConfig extends ResourceConfig {

    public JerseyConfig() {
        // Packages where your Jersey resources are located
        packages("com.harshalsharma.passkeydemo.apispec.api");
        packages("com.harshalsharma.passkeydemo.backendserv.controllers");
        register(new CORSFilter());
        register(new GenericFilter());
        register(new GenericExceptionHandler());
        property(ServletProperties.FILTER_FORWARD_ON_404, true);
    }

}