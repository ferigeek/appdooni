package com.github.ferigeek.appdooni.model;

import java.util.Objects;

/** An operating system an application can be available for. */
public final class OperatingSystem {

    private int id;
    private String name;

    public OperatingSystem(String name) {
        this.name = name;
    }

    public OperatingSystem(int id, String name) {
        this.id = id;
        this.name = name;
    }

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

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof OperatingSystem that)) {
            return false;
        }
        return id != 0 && id == that.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return name;
    }
}