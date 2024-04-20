package com.harshalsharma.passkeydemo.backendserv.exceptions;

import com.harshalsharma.passkeydemo.apispec.model.Error;

public interface BusinessError {

    Error getError();

    int getStatus();

}
