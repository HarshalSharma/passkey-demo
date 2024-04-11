package com.harshalsharma.passkeydemo.backendserv.data.repositories;

import com.harshalsharma.passkeydemo.backendserv.domain.webauthn.entities.Credential;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public interface WebAuthnRepository extends CrudRepository<Credential, String> {

    List<Credential> findByUserId(String userId);

}
