package com.harshalsharma.passkeydemo.backendserv.config.filters;

import com.harshalsharma.passkeydemo.backendserv.config.SimpleIdentityService;
import jakarta.inject.Inject;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import org.springframework.stereotype.Component;

@Component
public class GenericFilter implements ContainerResponseFilter {

    private final SimpleIdentityService identityService;

    @Inject
    public GenericFilter(SimpleIdentityService identityService) {
        this.identityService = identityService;
    }

    @Override
    public void filter(ContainerRequestContext containerRequestContext,
                       ContainerResponseContext containerResponseContext) {
        identityService.clearRequestAuthToken();
    }
}
