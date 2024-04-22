package com.harshalsharma.passkeydemo.backendserv.config.filters;

import com.harshalsharma.passkeydemo.backendserv.config.SimpleIdentityService;
import com.harshalsharma.passkeydemo.backendserv.exceptions.UnauthorizedRequestException;
import jakarta.inject.Inject;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.PreMatching;
import jakarta.ws.rs.ext.Provider;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@PreMatching
@Provider
@Component
public class AuthFilter implements ContainerRequestFilter {

    private static final String[] GUARDED_PATHS = new String[]{"notes", "preferences"};
    private final SimpleIdentityService identityService;

    @Inject
    public AuthFilter(SimpleIdentityService identityService) {
        this.identityService = identityService;
    }

    @Override
    public void filter(ContainerRequestContext requestContext) {
        String path = requestContext.getUriInfo().getPath();
        if (StringUtils.isNotBlank(path) && isGuardedRoute(path)) {
            String token = getAuthToken(requestContext);
            if (StringUtils.isNotBlank(token)) {
                identityService.setAuthToken(token.trim());
            }
        }
    }

    private boolean isGuardedRoute(String path) {
        return Arrays.stream(GUARDED_PATHS).anyMatch(path::contains);
    }

    @Nullable
    private static String getAuthToken(ContainerRequestContext requestContext) {
        String token = null;
        String xAuth = requestContext.getHeaderString("Authorization");
        if (StringUtils.isBlank(xAuth)) {
            throw new UnauthorizedRequestException();
        } else {
            token = getTokenFromAuthHeader(xAuth);
        }
        return token;
    }

    private static String getTokenFromAuthHeader(String xAuth) {
        String token = null;
        String[] tokens = xAuth.split(" ");
        if (tokens.length == 1) {
            token = tokens[0];
        } else if (tokens.length == 2) {
            token = tokens[1];
        }
        return token;
    }
}