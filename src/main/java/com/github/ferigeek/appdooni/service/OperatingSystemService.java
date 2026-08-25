package com.github.ferigeek.appdooni.service;

import com.github.ferigeek.appdooni.model.Application;
import com.github.ferigeek.appdooni.model.OperatingSystem;
import com.github.ferigeek.appdooni.repository.ApplicationRepository;
import com.github.ferigeek.appdooni.repository.OperatingSystemRepository;

import java.util.List;
import java.util.Optional;

/**
 * Business rules for operating systems. Names must be non-blank and unique.
 * Deleting an operating system removes any application associated only with it,
 * and otherwise drops that operating system from the application's set, keeping
 * the application with its remaining operating systems.
 */
public final class OperatingSystemService {

    private final OperatingSystemRepository osRepository;
    private final ApplicationRepository applicationRepository;

    public OperatingSystemService(OperatingSystemRepository osRepository, ApplicationRepository applicationRepository) {
        this.osRepository = osRepository;
        this.applicationRepository = applicationRepository;
    }

    public List<OperatingSystem> listOperatingSystems() {
        return osRepository.findAll();
    }

    public Optional<OperatingSystem> findOperatingSystem(int id) {
        return osRepository.findById(id);
    }

    /** Creates a new operating system. Returns it with its generated id. */
    public OperatingSystem addOperatingSystem(String name) {
        String trimmed = validateName(name);
        if (osRepository.existsByName(trimmed)) {
            throw new DuplicateNameException("An operating system named '" + trimmed + "' already exists.");
        }
        return osRepository.insert(new OperatingSystem(trimmed));
    }

    /** Renames an existing operating system. */
    public void updateOperatingSystem(OperatingSystem operatingSystem) {
        String trimmed = validateName(operatingSystem.getName());
        osRepository.findByName(trimmed).ifPresent(existing -> {
            if (existing.getId() != operatingSystem.getId()) {
                throw new DuplicateNameException("An operating system named '" + trimmed + "' already exists.");
            }
        });
        operatingSystem.setName(trimmed);
        osRepository.update(operatingSystem);
    }

    /**
     * Deletes an operating system. Applications that are available only for this
     * operating system are deleted; applications available for others keep them.
     */
    public void deleteOperatingSystem(int id) {
        for (Application application : applicationRepository.findByOperatingSystemId(id)) {
            if (application.getOperatingSystems().size() <= 1) {
                applicationRepository.delete(application.getId());
            } else {
                applicationRepository.removeOperatingSystem(application.getId(), id);
            }
        }
        osRepository.delete(id);
    }

    private String validateName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Operating system name must not be blank.");
        }
        return name.trim();
    }
}