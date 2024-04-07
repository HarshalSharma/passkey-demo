package com.harshalsharma.passkeydemo.backendserv.config.filters;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;

import java.io.IOException;

public class GenericFilter implements ContainerRequestFilter, ContainerResponseFilter {
    @Override
    public void filter(ContainerRequestContext containerRequestContext) {

    }

    @Override
    public void filter(ContainerRequestContext containerRequestContext,
                       ContainerResponseContext containerResponseContext) throws IOException {
//        Error error;
//        switch (Response.Status.fromStatusCode(containerResponseContext.getStatus())) {
//            case NOT_FOUND:
//                error = new Error();
//                error.setError("INVALID_REQUEST");
//                error.setDescription("Resource Not Found.");
//                containerResponseContext.setEntity(error);
//                break;
//            case INTERNAL_SERVER_ERROR:
//                error = new Error();
//                error.setError("INTERNAL_SERVER_ERROR");
//                error.setDescription("Some error occurred while processing the request.");
//                containerResponseContext.setEntity(error);
//                break;
//        }
//        if (containerResponseContext.hasEntity()
//                && containerResponseContext.getEntity() instanceof ExceptionResponse exception) {
//            containerResponseContext.setStatus(exception.getStatus());
//            containerResponseContext.setEntity(exception.getEntity());
//        }
    }
}
