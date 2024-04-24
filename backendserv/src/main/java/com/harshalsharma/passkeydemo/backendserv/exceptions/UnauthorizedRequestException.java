package com.harshalsharma.passkeydemo.backendserv.exceptions;

import com.harshalsharma.passkeydemo.apispec.model.Error;
import jakarta.ws.rs.core.Response;

public class UnauthorizedRequestException extends RuntimeException implements BusinessError {

    private static final int STATUS = Response.Status.UNAUTHORIZED.getStatusCode();
    private static final String ERROR = Response.Status.UNAUTHORIZED.name();
    private static final String DESCRIPTION = Response.Status.UNAUTHORIZED.getReasonPhrase();

    @Override
    public Error getError() {
        Error error = new Error();
        error.setError(ERROR);
        error.setDescription(DESCRIPTION);
        return error;
    }

    @Override
    public int getStatus() {
        return STATUS;
    }
}
