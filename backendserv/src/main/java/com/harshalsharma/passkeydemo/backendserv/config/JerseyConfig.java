package com.harshalsharma.passkeydemo.backendserv.config;

import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletProperties;
import org.springframework.stereotype.Component;

@Component
public class JerseyConfig extends ResourceConfig {

    public JerseyConfig() {
        // Packages where your Jersey resources are located
        packages("com.harshalsharma.passkeydemo.backendserv.api");
        packages("com.harshalsharma.passkeydemo.backendserv.controllers");
        register(new CORSFilter());
        register(new RequestFilter());
        property(ServletProperties.FILTER_FORWARD_ON_404, true);
    }

}