package com.harshalsharma.passkeydemo.backendserv.config.props;

import com.harshalsharma.passkeydemo.backendserv.domain.webauthn.WebauthnProperties;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Data
@Configuration
@ConfigurationProperties(prefix = "webauthn")
public class WebauthnPropertiesEntity implements WebauthnProperties {

    private String rpId;

    private String rpName;

    private List<Integer> supportedPublicKeyAlgs;

    private int tokenTimeoutInMillis;

    private String origin;

    private double locationSearchRadius;
}
