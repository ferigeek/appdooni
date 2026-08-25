package com.github.ferigeek.appdooni.service;

/**
 * Callback used while importing a database to decide how each conflicting
 * entry should be handled. {@code type} is "OS", "Tag", or "App"; {@code name}
 * is the conflicting name. Called on the caller's thread.
 */
@FunctionalInterface
public interface ImportConflictResolver {

    ImportAction resolve(String type, String name);
}