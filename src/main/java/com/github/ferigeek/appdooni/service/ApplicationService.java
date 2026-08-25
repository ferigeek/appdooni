package com.github.ferigeek.appdooni.service;

import com.github.ferigeek.appdooni.model.Application;
import com.github.ferigeek.appdooni.model.OperatingSystem;
import com.github.ferigeek.appdooni.model.Tag;
import com.github.ferigeek.appdooni.repository.ApplicationRepository;
import com.github.ferigeek.appdooni.repository.OperatingSystemRepository;
import com.github.ferigeek.appdooni.repository.TagRepository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Business rules for applications: a non-blank name is required and every
 * application must be associated with at least one operating system. Tags are
 * optional. Provides searching and combined filtering by operating system and
 * tags.
 */
public final class ApplicationService {

    private final ApplicationRepository applicationRepository;
    private final OperatingSystemRepository osRepository;
    private final TagRepository tagRepository;

    public ApplicationService(ApplicationRepository applicationRepository,
                              OperatingSystemRepository osRepository,
                              TagRepository tagRepository) {
        this.applicationRepository = applicationRepository;
        this.osRepository = osRepository;
        this.tagRepository = tagRepository;
    }

    public List<Application> listApplications() {
        return applicationRepository.findAll();
    }

    public Optional<Application> findApplication(int id) {
        return applicationRepository.findById(id);
    }

    /** Creates an application with the given operating system and tag ids. */
    public Application addApplication(Application application, Set<Integer> operatingSystemIds, Set<Integer> tagIds) {
        validate(application, operatingSystemIds);
        populateAssociations(application, operatingSystemIds, tagIds);
        return applicationRepository.insert(application);
    }

    /** Updates an application with the given operating system and tag ids. */
    public void updateApplication(Application application, Set<Integer> operatingSystemIds, Set<Integer> tagIds) {
        validate(application, operatingSystemIds);
        populateAssociations(application, operatingSystemIds, tagIds);
        applicationRepository.update(application);
    }

    public void deleteApplication(int id) {
        applicationRepository.delete(id);
    }

    /** Returns applications matching the search text and OS/tag filters. */
    public List<Application> findFiltered(String searchText, Integer operatingSystemId,
                                          Set<Integer> tagIds, TagFilterMode mode) {
        return applicationRepository.findFiltered(searchText, operatingSystemId, tagIds, mode);
    }

    private void validate(Application application, Set<Integer> operatingSystemIds) {
        if (application.getName() == null || application.getName().isBlank()) {
            throw new IllegalArgumentException("Application name must not be blank.");
        }
        if (operatingSystemIds == null || operatingSystemIds.isEmpty()) {
            throw new IllegalArgumentException("Every application must have at least one operating system.");
        }
    }

    private void populateAssociations(Application application, Set<Integer> osIds, Set<Integer> tagIds) {
        application.getOperatingSystems().clear();
        for (int id : osIds) {
            osRepository.findById(id).ifPresent(application::addOperatingSystem);
        }
        application.getTags().clear();
        if (tagIds != null) {
            for (int id : tagIds) {
                tagRepository.findById(id).ifPresent(application::addTag);
            }
        }
    }
}