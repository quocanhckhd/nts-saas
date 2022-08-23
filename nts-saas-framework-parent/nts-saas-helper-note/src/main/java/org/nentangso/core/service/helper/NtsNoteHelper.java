package org.nentangso.core.service.helper;

import org.apache.commons.lang3.StringUtils;
import org.nentangso.core.domain.NoteEntity;
import org.nentangso.core.repository.NoteRepository;
import org.nentangso.core.service.errors.NotFoundException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.transaction.Transactional;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.util.*;
import java.util.stream.Collectors;

@ConditionalOnProperty(
    prefix = "nts.helper.note",
    name = "enabled",
    havingValue = "true"
)
@Service
public class NtsNoteHelper {
    private final NoteRepository noteRepository;

    public NtsNoteHelper(NoteRepository noteRepository) {
        this.noteRepository = noteRepository;
    }

    public Optional<String> findNoteById(@NotNull @Min(1) Long id) {
        if (id == null || id <= 0) {
            return Optional.empty();
        }
        return noteRepository.findById(id).map(NoteEntity::getNote);
    }

    public Map<Long, String> findAllNoteById(Collection<@NotNull @Min(1) Long> ids) {
        if (CollectionUtils.isEmpty(ids)) {
            return Collections.emptyMap();
        }
        return noteRepository.findAllById(ids).stream()
            .collect(Collectors.toMap(NoteEntity::getId, NoteEntity::getNote));
    }

    @Transactional
    public Optional<NoteEntity> save(String note, Long id) {
        if (StringUtils.isEmpty(note)) {
            if (Objects.nonNull(id)) {
                noteRepository.deleteById(id);
            }
            return Optional.empty();
        }
        NoteEntity noteEntity = new NoteEntity();
        if (Objects.nonNull(id)) {
            noteEntity = noteRepository.findById(id).orElseThrow(NotFoundException::new);
            if (StringUtils.equals(noteEntity.getNote(), note)) {
                return Optional.of(noteEntity);
            }
        }
        noteEntity.setNote(note);
        return Optional.of(noteRepository.save(noteEntity));
    }

    @Transactional
    public Optional<NoteEntity> save(String note, NoteEntity noteEntity) {
        if (StringUtils.isEmpty(note)) {
            if (Objects.nonNull(noteEntity) && Objects.nonNull(noteEntity.getId())) {
                noteRepository.deleteById(noteEntity.getId());
            }
            return Optional.empty();
        }
        if (Objects.isNull(noteEntity) || Objects.isNull(noteEntity.getId())) {
            noteEntity = new NoteEntity();
        }
        if (StringUtils.equals(noteEntity.getNote(), note)) {
            return Optional.of(noteEntity);
        }
        noteEntity.setNote(note);
        return Optional.of(noteRepository.save(noteEntity));
    }
}
