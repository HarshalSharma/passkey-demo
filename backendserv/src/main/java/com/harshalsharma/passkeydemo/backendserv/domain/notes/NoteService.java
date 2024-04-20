package com.harshalsharma.passkeydemo.backendserv.domain.notes;

import com.harshalsharma.passkeydemo.apispec.api.NotesApi;
import com.harshalsharma.passkeydemo.apispec.model.SimpleNote;
import com.harshalsharma.passkeydemo.backendserv.data.repositories.NotesRepository;
import com.harshalsharma.passkeydemo.backendserv.domain.notes.entities.Note;
import jakarta.inject.Inject;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class NoteService implements NotesApi {

    private NotesRepository notesRepository;

    private IdentityService identityService;

    @Inject
    public NoteService(NotesRepository notesRepository, IdentityService identityService) {
        this.notesRepository = notesRepository;
        this.identityService = identityService;
    }

    @Override
    public SimpleNote notesGet() {
        Optional<Note> optionalNote = notesRepository.findById(identityService.getCurrentUserId());
        SimpleNote simpleNote = new SimpleNote();
        if (optionalNote.isEmpty()) {
            return simpleNote;
        } else {
            simpleNote.setNote(optionalNote.get().getNotes());
            return simpleNote;
        }
    }

    @Override
    public void notesPut(SimpleNote simpleNote) {
        String currentUserId = identityService.getCurrentUserId();
        notesRepository.save(Note.builder().userId(currentUserId).notes(simpleNote.getNote()).build());
    }
}
