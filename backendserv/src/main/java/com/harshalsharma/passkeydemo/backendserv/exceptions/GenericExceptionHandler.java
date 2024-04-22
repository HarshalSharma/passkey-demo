package com.harshalsharma.passkeydemo.backendserv.exceptions;

import com.harshalsharma.passkeydemo.apispec.model.Error;
import com.harshalsharma.passkeydemo.backendserv.domain.UniqueStringGenerator;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import org.springframework.stereotype.Component;

@Component
public class GenericExceptionHandler implements ExceptionMapper<Exception> {

    @Override
    public Response toResponse(Exception ex) {
        Response.Status status = Response.Status.INTERNAL_SERVER_ERROR;
        Error error = new Error();

        if (ex instanceof BusinessError bizError) {
            status = Response.Status.fromStatusCode(bizError.getStatus());
            error = bizError.getError();
        } else if (ex instanceof NotFoundException) {
            status = Response.Status.NOT_FOUND;
            error.setError(Response.Status.NOT_FOUND.name());
            error.setDescription(Response.Status.NOT_FOUND.getReasonPhrase());
        } else {
            String id = logException(ex);
            error.setError("INTERNAL_SERVER_ERROR");
            error.setDescription("Error processing this request, reference id - " + id);
        }
        // Customize the error response according to your requirements
        return Response.status(status)
                .entity(error)
                .build();
    }

    private static String logException(Exception ex) {
        String logId = UniqueStringGenerator.generateUUIDString();
        RuntimeException wrappedException = new RuntimeException(logId, ex);
        wrappedException.printStackTrace();
        return logId;
    }
}
