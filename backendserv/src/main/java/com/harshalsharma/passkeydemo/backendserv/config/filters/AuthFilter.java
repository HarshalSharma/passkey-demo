package com.harshalsharma.passkeydemo.backendserv.config.filters;

import com.harshalsharma.passkeydemo.backendserv.config.SimpleIdentityService;
import com.harshalsharma.passkeydemo.backendserv.exceptions.InvalidRequestException;
import jakarta.inject.Inject;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.Response;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Component;

@Component
public class AuthFilter implements ContainerRequestFilter {

    private final SimpleIdentityService identityService;

    @Inject
    public AuthFilter(SimpleIdentityService identityService) {
        this.identityService = identityService;
    }

    @Override
    public void filter(ContainerRequestContext requestContext) {
        try {
            String path = requestContext.getUriInfo().getPath();
            if (StringUtils.isNotBlank(path) && path.contains("notes")) {
                String token = getAuthToken(requestContext);
                if (StringUtils.isNotBlank(token)) {
                    identityService.setAuthToken(token.trim());
                }
            }
        } catch (Exception e) {
            throw new InvalidRequestException("Error processing the request.");
        }
    }

    @Nullable
    private static String getAuthToken(ContainerRequestContext requestContext) {
        String token = null;
        String xAuth = requestContext.getHeaderString("Authorization");
        if (StringUtils.isBlank(xAuth)) {
            requestContext.abortWith(Response.status(
                    Response.Status.UNAUTHORIZED).build());
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