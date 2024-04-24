package com.harshalsharma.passkeydemo.backendserv.data.repositories;

import com.harshalsharma.passkeydemo.backendserv.domain.webauthn.entities.Preference;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public interface PreferenceRepository extends CrudRepository<Preference, String> {

    List<Preference> findByLatitudeBetweenAndLongitudeBetween(double minLat, double maxLat, double minLong, double maxLong);

}
