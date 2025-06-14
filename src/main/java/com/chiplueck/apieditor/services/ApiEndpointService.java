package com.chiplueck.apieditor.services;

import com.chiplueck.apieditor.model.ApiEndpoint;
import com.intellij.credentialStore.CredentialAttributes;
import com.intellij.credentialStore.CredentialAttributesKt;
import com.intellij.credentialStore.Credentials;
import com.intellij.ide.passwordSafe.PasswordSafe;
import com.intellij.openapi.components.*;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.util.xmlb.XmlSerializerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Service for managing API endpoints.
 * This service is responsible for storing and retrieving the list of configured endpoints,
 * as well as handling the secure storage of credentials.
 */
@State(
    name = "ApiEndpointService",
    storages = {@Storage("apiEditorSettings.xml")}
)
public class ApiEndpointService implements PersistentStateComponent<ApiEndpointService> {
    private static final Logger LOG = Logger.getInstance(ApiEndpointService.class);
    private List<ApiEndpoint> endpoints = new ArrayList<>();

    public static ApiEndpointService getInstance() {
        return ApplicationManager.getApplication().getService(ApiEndpointService.class);
    }

    @Nullable
    @Override
    public ApiEndpointService getState() {
        LOG.info("getState called, returning current state with " + endpoints.size() + " endpoints");
        for (ApiEndpoint ep : endpoints) {
            LOG.info("  - " + ep.getName() + " (ID: " + ep.getId() + ")");
        }
        return this;
    }

    @Override
    public void loadState(@NotNull ApiEndpointService state) {
        LOG.info("loadState called with state containing " + state.endpoints.size() + " endpoints");
        for (ApiEndpoint ep : state.endpoints) {
            LOG.info("  - " + ep.getName() + " (ID: " + ep.getId() + ")");
        }
        XmlSerializerUtil.copyBean(state, this);
        LOG.info("State loaded, current endpoints: " + endpoints.size());
        for (ApiEndpoint ep : endpoints) {
            LOG.info("  - " + ep.getName() + " (ID: " + ep.getId() + ")");
        }
    }

    public List<ApiEndpoint> getEndpoints() {
        return new ArrayList<>(endpoints);
    }

    public void setEndpoints(List<ApiEndpoint> endpoints) {
        LOG.info("Setting endpoints. New count: " + endpoints.size());
        for (ApiEndpoint ep : endpoints) {
            LOG.info("  - " + ep.getName() + " (ID: " + ep.getId() + ")");
        }

        // Create a deep copy of the endpoints list to ensure it's not modified externally
        this.endpoints = new ArrayList<>();
        for (ApiEndpoint ep : endpoints) {
            // Use the constructor that preserves the ID
            this.endpoints.add(new ApiEndpoint(ep.getId(), ep.getName(), ep.getUrl(), ep.getUsername()));
        }

        try {
            // Force a state save by explicitly calling saveSettings
            ApplicationManager.getApplication().saveSettings();
            LOG.info("Settings saved after updating endpoints list");

            // Verify that the endpoints were saved correctly
            LOG.info("Verifying endpoints after save: " + this.endpoints.size());
            for (ApiEndpoint ep : this.endpoints) {
                LOG.info("  - " + ep.getName() + " (ID: " + ep.getId() + ")");
            }
        } catch (Exception e) {
            LOG.error("Failed to save settings after updating endpoints list", e);
            throw new RuntimeException("Failed to save endpoint settings: " + e.getMessage(), e);
        }
    }

    public void addEndpoint(ApiEndpoint endpoint) {
        LOG.info("Adding endpoint: " + endpoint.getName() + " (ID: " + endpoint.getId() + ")");
        endpoints.add(endpoint);

        try {
            ApplicationManager.getApplication().saveSettings();
            LOG.info("Settings saved after adding endpoint: " + endpoint.getName());
        } catch (Exception e) {
            LOG.error("Failed to save settings after adding endpoint: " + endpoint.getName(), e);
            throw new RuntimeException("Failed to save endpoint settings: " + e.getMessage(), e);
        }
    }

    public void updateEndpoint(ApiEndpoint endpoint) {
        LOG.info("Updating endpoint: " + endpoint.getName() + " (ID: " + endpoint.getId() + ")");
        boolean updated = false;

        for (int i = 0; i < endpoints.size(); i++) {
            if (endpoints.get(i).getId().equals(endpoint.getId())) {
                endpoints.set(i, endpoint);
                updated = true;
                break;
            }
        }

        if (updated) {
            LOG.info("Endpoint updated in list: " + endpoint.getName());
            try {
                ApplicationManager.getApplication().saveSettings();
                LOG.info("Settings saved after updating endpoint: " + endpoint.getName());
            } catch (Exception e) {
                LOG.error("Failed to save settings after updating endpoint: " + endpoint.getName(), e);
                throw new RuntimeException("Failed to save endpoint settings: " + e.getMessage(), e);
            }
        } else {
            LOG.warn("Failed to update endpoint: " + endpoint.getName() + " - not found in list");
        }
    }

    public void removeEndpoint(ApiEndpoint endpoint) {
        LOG.info("Removing endpoint: " + endpoint.getName() + " (ID: " + endpoint.getId() + ")");
        LOG.info("Current endpoints before removal: " + endpoints.size());
        for (ApiEndpoint ep : endpoints) {
            LOG.info("  - " + ep.getName() + " (ID: " + ep.getId() + ")");
        }

        // Create a new list without the endpoint to be removed
        List<ApiEndpoint> updatedEndpoints = new ArrayList<>();
        for (ApiEndpoint ep : endpoints) {
            if (!ep.getId().equals(endpoint.getId())) {
                updatedEndpoints.add(ep);
            }
        }

        boolean removed = endpoints.size() != updatedEndpoints.size();

        if (removed) {
            LOG.info("Endpoint will be removed: " + endpoint.getName());
            LOG.info("Updated endpoints list size: " + updatedEndpoints.size());
            for (ApiEndpoint ep : updatedEndpoints) {
                LOG.info("  - " + ep.getName() + " (ID: " + ep.getId() + ")");
            }

            // Also remove the password from secure storage
            removePassword(endpoint);

            // Ensure settings are saved to disk
            try {
                LOG.info("Saving state explicitly via setEndpoints");
                // Save the state explicitly using the updated list
                // This will also update the endpoints list in the service
                setEndpoints(new ArrayList<>(updatedEndpoints));

                LOG.info("Calling ApplicationManager.getApplication().saveSettings() again to ensure persistence");
                ApplicationManager.getApplication().saveSettings();

                LOG.info("Forcing state save");

                LOG.info("Settings saved after removing endpoint: " + endpoint.getName());
                LOG.info("Verifying endpoints after save: " + endpoints.size());
                for (ApiEndpoint ep : endpoints) {
                    LOG.info("  - " + ep.getName() + " (ID: " + ep.getId() + ")");
                }
            } catch (Exception e) {
                LOG.error("Failed to save settings after removing endpoint: " + endpoint.getName(), e);
            }
        } else {
            LOG.warn("Failed to remove endpoint: " + endpoint.getName() + " - not found in list");
        }
    }

    public Optional<ApiEndpoint> findEndpointById(String id) {
        return endpoints.stream()
                .filter(e -> e.getId().equals(id))
                .findFirst();
    }

    // Methods for secure password storage

    private CredentialAttributes createCredentialAttributes(ApiEndpoint endpoint) {
        return new CredentialAttributes(
                CredentialAttributesKt.generateServiceName("ApiEditorPlugin", endpoint.getId())
        );
    }

    public void storePassword(ApiEndpoint endpoint, String password) {
        CredentialAttributes credentialAttributes = createCredentialAttributes(endpoint);
        Credentials credentials = new Credentials(endpoint.getUsername(), password);
        PasswordSafe.getInstance().set(credentialAttributes, credentials);
    }

    public String getPassword(ApiEndpoint endpoint) {
        CredentialAttributes credentialAttributes = createCredentialAttributes(endpoint);
        Credentials credentials = PasswordSafe.getInstance().get(credentialAttributes);
        return credentials != null ? credentials.getPasswordAsString() : null;
    }

    public void removePassword(ApiEndpoint endpoint) {
        CredentialAttributes credentialAttributes = createCredentialAttributes(endpoint);
        PasswordSafe.getInstance().set(credentialAttributes, null);
    }
}
