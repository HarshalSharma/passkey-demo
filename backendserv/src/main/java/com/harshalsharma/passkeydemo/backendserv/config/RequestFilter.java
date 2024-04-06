package com.harshalsharma.passkeydemo.backendserv.config;

import com.harshalsharma.passkeydemo.apispec.model.Error;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;

import java.io.IOException;

public class RequestFilter implements ContainerRequestFilter, ContainerResponseFilter {
    @Override
    public void filter(ContainerRequestContext containerRequestContext) {

    }

    @Override
    public void filter(ContainerRequestContext containerRequestContext,
                       ContainerResponseContext containerResponseContext) throws IOException {
        if (!containerResponseContext.hasEntity() || containerResponseContext.getEntity() == null) {
            Error error = new Error();
            error.setError("Unimplemented API");
            error.setError("API is not yet implemented.");
            containerResponseContext.setEntity(error);
        }
    }
}
