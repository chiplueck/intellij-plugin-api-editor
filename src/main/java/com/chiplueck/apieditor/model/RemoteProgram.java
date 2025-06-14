package com.chiplueck.apieditor.model;

import java.util.Objects;

/**
 * Represents a program stored on the remote API.
 */
public class RemoteProgram {
    private String id;
    private String name;
    private String extension;
    private String content;
    private long lastModified;

    public RemoteProgram() {
    }

    public RemoteProgram(String id, String name, String extension) {
        this.id = id;
        this.name = name;
        this.extension = extension;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getExtension() {
        return extension;
    }

    public void setExtension(String extension) {
        this.extension = extension;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public long getLastModified() {
        return lastModified;
    }

    public void setLastModified(long lastModified) {
        this.lastModified = lastModified;
    }

    /**
     * Gets the full name of the program including its extension.
     * @return The full name with extension
     */
    public String getFullName() {
        if (extension != null && !extension.isEmpty()) {
            if (extension.startsWith(".")) {
                return name + extension;
            } else {
                return name + "." + extension;
            }
        }
        return name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RemoteProgram that = (RemoteProgram) o;
        return lastModified == that.lastModified &&
                Objects.equals(id, that.id) &&
                Objects.equals(name, that.name) &&
                Objects.equals(extension, that.extension);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, extension, lastModified);
    }

    @Override
    public String toString() {
        return "RemoteProgram{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", extension='" + extension + '\'' +
                ", lastModified=" + lastModified +
                '}';
    }
}
