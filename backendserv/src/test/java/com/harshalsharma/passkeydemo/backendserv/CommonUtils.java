package com.harshalsharma.passkeydemo.backendserv;

import com.harshalsharma.passkeydemo.apispec.model.AuthenticationRequest;
import com.harshalsharma.passkeydemo.apispec.model.PublicKeyCredentialCreationOptionsResponse;
import org.apache.commons.codec.binary.Base64;
import org.jetbrains.annotations.NotNull;

public class CommonUtils {

    public static final String WEBAUTHN_CREATE_TYPE = "webauthn.create";
    public static final String WEBAUTHN_GET_TYPE = "webauthn.get";

    @NotNull
    public static String getValidAttestationObjectString() {
        return """
                o2NmbXRkbm9uZWdhdHRTdG10oGhhdXRoRGF0YVikt8DGRTBfls-BhOH2QC404lvdhe_t2_NkvM0n
                QWEEADdFAAAAAK3OAAI1vMYKZIsLJfHwVQMAIG3U68BVLKmmjpNF5gfsJf9w4gbLeAAuoOUO92iCL8yMpQECAyYgASFYIB6
                TbnDZAtONzEw2l_fgafcbO9LbMve1DfVrRMu3TKl7Ilgg-wT1ncos7Hh-kHfiFxuuvENQt3RUc7evD4FewvEIrNg
                """;
    }

    public static String createClientDataJson(PublicKeyCredentialCreationOptionsResponse creationOptionsResponse, String origin) {
        String challenge = creationOptionsResponse.getChallenge();
        return createClientDataJson(challenge, origin, WEBAUTHN_CREATE_TYPE);
    }

    public static String createClientDataJson(String challenge, String origin, String type) {
        String clientDataJson = "{ \"challenge\":\"" + challenge + "\", \"origin\":\"" + origin + "\", " +
                "\"type\": \"" + type + "\"}";
        return Base64.encodeBase64String(clientDataJson.getBytes());
    }

    @NotNull
    public static AuthenticationRequest getValidAuthNRequest(String credentialId) {
        AuthenticationRequest request = new AuthenticationRequest();
        request.setClientDataJson("eyJ0eXBlIjoid2ViYXV0aG4uZ2V0IiwiY2hhbGxlbmdlIjoiYUdWc2JHOCIsIm9yaWdpbiI6Imh0dHBzOi8vaGFyc2hhbC1iaXRzLWZpbmFsLXByb2plY3Qud2ViLmFwcCIsImNyb3NzT3JpZ2luIjpmYWxzZX0=");
        request.setSignature("MEUCIDRJkyQFc4Zq5LDOzXhVHTvQNy9iIfUUCVfGxoWQ2GjHAiEAilgWwOeASLfJfrC3FB+u+2b2AFphBdnA+ZcQTCgt2kc=");
        request.setAuthenticatorData("XplMM5wOtrEQZZaAkWy7bwddLei9qvVveK3GdtMrqk0FAAAAAA==");
        request.setCredentialId(credentialId);
        return request;
    }
}
