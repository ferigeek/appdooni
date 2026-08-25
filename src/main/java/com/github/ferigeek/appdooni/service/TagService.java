package com.github.ferigeek.appdooni.service;

import com.github.ferigeek.appdooni.model.Tag;
import com.github.ferigeek.appdooni.repository.TagRepository;

import java.util.List;
import java.util.Optional;

/**
 * Business rules for tags: names must be non-blank and unique. Deleting a tag
 * only removes the association; the applications that carried it are kept.
 */
public final class TagService {

    private final TagRepository tagRepository;

    public TagService(TagRepository tagRepository) {
        this.tagRepository = tagRepository;
    }

    public List<Tag> listTags() {
        return tagRepository.findAll();
    }

    public Optional<Tag> findTag(int id) {
        return tagRepository.findById(id);
    }

    /** Creates a new tag. Returns the tag with its generated id. */
    public Tag addTag(String name) {
        String trimmed = validateName(name);
        if (tagRepository.existsByName(trimmed)) {
            throw new DuplicateNameException("A tag named '" + trimmed + "' already exists.");
        }
        return tagRepository.insert(new Tag(trimmed));
    }

    /** Renames an existing tag. */
    public void updateTag(Tag tag) {
        String trimmed = validateName(tag.getName());
        if (tagRepository.existsByName(trimmed)) {
            throw new DuplicateNameException("A tag named '" + trimmed + "' already exists.");
        }
        tag.setName(trimmed);
        tagRepository.update(tag);
    }

    /** Deletes the tag; associated applications are left unchanged. */
    public void deleteTag(int id) {
        tagRepository.delete(id);
    }

    private String validateName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Tag name must not be blank.");
        }
        return name.trim();
    }
}