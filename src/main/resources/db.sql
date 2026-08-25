CREATE TABLE applications (
    id INTEGER PRIMARY KEY,
    name TEXT NOT NULL,
    description TEXT,
    installation_source TEXT,
    website_url TEXT,
    created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_application_name_not_blank
        CHECK (length(trim(name)) > 0)
);

CREATE TABLE operating_systems (
    id INTEGER PRIMARY KEY,
    name TEXT NOT NULL UNIQUE,

    CONSTRAINT chk_operating_system_name_not_blank
        CHECK (length(trim(name)) > 0)
);

CREATE TABLE tags (
    id INTEGER PRIMARY KEY,
    name TEXT NOT NULL UNIQUE,

    CONSTRAINT chk_tag_name_not_blank
        CHECK (length(trim(name)) > 0)
);

CREATE TABLE application_operating_systems (
    application_id INTEGER NOT NULL,
    operating_system_id INTEGER NOT NULL,

    PRIMARY KEY (application_id, operating_system_id),

    FOREIGN KEY (application_id)
        REFERENCES applications(id)
        ON DELETE CASCADE,

    FOREIGN KEY (operating_system_id)
        REFERENCES operating_systems(id)
        ON DELETE CASCADE
);

CREATE TABLE application_tags (
    application_id INTEGER NOT NULL,
    tag_id INTEGER NOT NULL,

    PRIMARY KEY (application_id, tag_id),

    FOREIGN KEY (application_id)
        REFERENCES applications(id)
        ON DELETE CASCADE,

    FOREIGN KEY (tag_id)
        REFERENCES tags(id)
        ON DELETE CASCADE
);

CREATE INDEX idx_application_name
    ON applications(name);

CREATE INDEX idx_application_operating_systems_os
    ON application_operating_systems(operating_system_id);

CREATE INDEX idx_application_tags_tag
    ON application_tags(tag_id);

INSERT INTO operating_systems (name) VALUES
    ('Linux'),
    ('Windows'),
    ('macOS'),
    ('BSD'),
    ('iOS'),
    ('Android');
