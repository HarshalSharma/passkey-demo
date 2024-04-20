package com.harshalsharma.passkeydemo.backendserv.domain.webauthn.entities;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.*;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(of = {"credentialId", "userId"})
@Entity
public class Credential {

    @Id
    private String credentialId;

    private String userId;

    private String publicKey;

    private String publicKeyType;

}
