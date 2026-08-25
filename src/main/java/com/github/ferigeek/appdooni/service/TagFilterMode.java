package com.github.ferigeek.appdooni.service;

/**
 * How multiple selected tags are combined when filtering applications:
 * either every selected tag must match ({@link #AND}) or at least one must
 * match ({@link #OR}).
 */
public enum TagFilterMode {
    AND,
    OR
}