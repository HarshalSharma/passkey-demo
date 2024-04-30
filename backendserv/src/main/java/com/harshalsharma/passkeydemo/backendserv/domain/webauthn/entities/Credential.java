package com.harshalsharma.passkeydemo.backendserv.domain.webauthn.entities;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.*;
import org.hibernate.validator.constraints.Length;

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

    @Length(max = 2048)
    private String publicKey;

    private String publicKeyType;

}
