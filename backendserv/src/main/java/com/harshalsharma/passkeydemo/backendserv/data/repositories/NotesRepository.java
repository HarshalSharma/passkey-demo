package com.harshalsharma.passkeydemo.backendserv.data.repositories;

import com.harshalsharma.passkeydemo.backendserv.domain.notes.entities.Note;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotesRepository extends JpaRepository<Note, String> {
}
