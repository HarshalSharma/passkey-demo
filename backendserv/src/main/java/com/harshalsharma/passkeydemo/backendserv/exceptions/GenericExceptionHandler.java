package com.harshalsharma.passkeydemo.backendserv.exceptions;

import com.harshalsharma.passkeydemo.backendserv.domain.UniqueStringGenerator;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;

public class GenericExceptionHandler implements ExceptionMapper<InvalidRequestException> {

    @Override
    public Response toResponse(InvalidRequestException e) {
        logException(e);
        return Response.status(e.getStatus())
                .entity(e.getError())
                .build();
    }

    private static void logException(Exception ex) {
        String logId = UniqueStringGenerator.generateUUIDString();
        RuntimeException wrappedException = new RuntimeException(logId, ex);
        wrappedException.printStackTrace();
    }
}
