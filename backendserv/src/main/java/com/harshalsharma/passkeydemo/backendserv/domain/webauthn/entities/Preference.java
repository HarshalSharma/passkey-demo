package com.harshalsharma.passkeydemo.backendserv.domain.webauthn.entities;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.*;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(of = {"userId"})
@Entity
public class Preference {

    @Id
    private String userId;

    private double latitude;

    private double longitude;

}
