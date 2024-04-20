package com.harshalsharma.passkeydemo.backendserv.domain;

import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class UniqueStringGenerator {

    @NotNull
    public static synchronized String generateUUIDString() {
        return UUID.randomUUID().toString();
    }

}
