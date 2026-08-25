package com.github.ferigeek.appdooni.model;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * An application catalog entry. Holds the basic information plus the operating
 * systems it is available for and its optional tags.
 */
public final class Application {

    private int id;
    private String name;
    private String description;
    private String installationSource;
    private String websiteUrl;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private final Set<OperatingSystem> operatingSystems = new LinkedHashSet<>();
    private final Set<Tag> tags = new LinkedHashSet<>();

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getInstallationSource() {
        return installationSource;
    }

    public void setInstallationSource(String installationSource) {
        this.installationSource = installationSource;
    }

    public String getWebsiteUrl() {
        return websiteUrl;
    }

    public void setWebsiteUrl(String websiteUrl) {
        this.websiteUrl = websiteUrl;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Set<OperatingSystem> getOperatingSystems() {
        return operatingSystems;
    }

    public Set<Tag> getTags() {
        return tags;
    }

    public void addOperatingSystem(OperatingSystem operatingSystem) {
        operatingSystems.add(operatingSystem);
    }

    public void addTag(Tag tag) {
        tags.add(tag);
    }

    public void removeTag(Tag tag) {
        tags.remove(tag);
    }

    public void removeOperatingSystem(OperatingSystem operatingSystem) {
        operatingSystems.remove(operatingSystem);
    }

    /** Unmodifiable view of the operating systems for rendering. */
    public Set<OperatingSystem> operatingSystems() {
        return Collections.unmodifiableSet(operatingSystems);
    }

    /** Unmodifiable view of the tags for rendering. */
    public Set<Tag> tags() {
        return Collections.unmodifiableSet(tags);
    }
}