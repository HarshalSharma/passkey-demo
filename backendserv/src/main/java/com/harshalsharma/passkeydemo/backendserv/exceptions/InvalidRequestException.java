package com.harshalsharma.passkeydemo.backendserv.exceptions;

import com.harshalsharma.passkeydemo.apispec.model.Error;
import jakarta.ws.rs.core.Response;

public class InvalidRequestException extends RuntimeException implements BusinessError {

    private static final int STATUS = Response.Status.BAD_REQUEST.getStatusCode();
    private static final String ERROR = "INVALID_REQUEST";
    private final String description;

    public InvalidRequestException(String description) {
        super(description);
        this.description = description;
    }

    public InvalidRequestException(String description, Throwable throwable) {
        super(throwable);
        this.description = description;
    }

    @Override
    public Error getError() {
        Error error = new Error();
        error.setError(ERROR);
        error.setDescription(description);
        return error;
    }

    @Override
    public int getStatus() {
        return STATUS;
    }
}
