package com.github.ferigeek.appdooni.service;

/** How a conflict found during import should be resolved. */
public enum ImportAction {
    SKIP,
    OVERWRITE,
    KEEP_BOTH
}