package com.github.ferigeek.appdooni.service;

/** Thrown when a unique entity name is already in use. */
public class DuplicateNameException extends RuntimeException {

    public DuplicateNameException(String message) {
        super(message);
    }
}