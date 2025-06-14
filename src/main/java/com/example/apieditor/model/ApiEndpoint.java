package com.example.apieditor.model;

import com.intellij.util.xmlb.annotations.Transient;
import java.util.Objects;
import java.util.UUID;

/**
 * Represents a remote API endpoint configuration.
 * This class is serialized by the IntelliJ platform's serialization mechanism.
 */
public class ApiEndpoint {
    private String id;
    private String name;
    private String url;
    private String username;
    @Transient
    private String password; // This will be stored securely

    public ApiEndpoint() {
        this.id = UUID.randomUUID().toString();
    }

    public ApiEndpoint(String name, String url, String username) {
        this();
        this.name = name;
        this.url = url;
        this.username = username;
    }

    public ApiEndpoint(String id, String name, String url, String username) {
        this.id = id;
        this.name = name;
        this.url = url;
        this.username = username;
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

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    // Password is not included in toString, equals, or hashCode for security reasons

    @Override
    public String toString() {
        return  name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ApiEndpoint that = (ApiEndpoint) o;
        return Objects.equals(id, that.id) &&
                Objects.equals(name, that.name) &&
                Objects.equals(url, that.url) &&
                Objects.equals(username, that.username);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, url, username);
    }
}
