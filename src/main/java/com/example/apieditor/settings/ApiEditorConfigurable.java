package com.example.apieditor.settings;

import com.example.apieditor.model.ApiEndpoint;
import com.example.apieditor.services.ApiEndpointService;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.ui.Messages;
import com.intellij.ui.AddEditDeleteListPanel;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Configurable component for API Editor settings.
 */
public class ApiEditorConfigurable implements Configurable {
    private static final Logger LOG = Logger.getInstance(ApiEditorConfigurable.class);
    private ApiEndpointListPanel endpointListPanel;
    private ApiEndpointService endpointService;
    private List<ApiEndpoint> modifiedEndpoints;

    @Nls(capitalization = Nls.Capitalization.Title)
    @Override
    public String getDisplayName() {
        return "API Editor Settings";
    }

    private ApiEndpointService getEndpointService() {
        if (endpointService == null) {
            endpointService = ApiEndpointService.getInstance();
        }
        return endpointService;
    }

    @Nullable
    @Override
    public JComponent createComponent() {
        modifiedEndpoints = new ArrayList<>(getEndpointService().getEndpoints());
        endpointListPanel = new ApiEndpointListPanel(modifiedEndpoints);
        return endpointListPanel;
    }

    @Override
    public boolean isModified() {
        return !modifiedEndpoints.equals(getEndpointService().getEndpoints());
    }

    @Override
    public void apply() throws ConfigurationException {
        LOG.info("Applying endpoint changes. Current endpoints count: " + modifiedEndpoints.size());
        try {
            getEndpointService().setEndpoints(modifiedEndpoints);
            LOG.info("Successfully applied endpoint changes");
        } catch (Exception e) {
            String errorMsg = "Failed to apply endpoint changes: " + e.getMessage();
            LOG.error(errorMsg, e);
            throw new ConfigurationException(errorMsg, "Save Error");
        }
    }

    @Override
    public void reset() {
        modifiedEndpoints = new ArrayList<>(getEndpointService().getEndpoints());
        endpointListPanel.resetFromSettings();
    }

    @Override
    public void disposeUIResources() {
        endpointListPanel = null;
        modifiedEndpoints = null;
    }

    private class ApiEndpointListPanel extends AddEditDeleteListPanel<ApiEndpoint> {
        public ApiEndpointListPanel(List<ApiEndpoint> endpoints) {
            super("API Endpoints", endpoints);
        }

        @Override
        protected ApiEndpoint findItemToAdd() {
            ApiEndpointDialog dialog = new ApiEndpointDialog(null);
            if (dialog.showAndGet()) {
                ApiEndpoint endpoint = dialog.getEndpoint();
                String password = dialog.getPassword();
                if (password != null && !password.isEmpty()) {
                    getEndpointService().storePassword(endpoint, password);
                }
                // The panel will add the endpoint to the list model,
                // which updates the `modifiedEndpoints` list automatically.
                return endpoint;
            }
            return null;
        }

        @Override
        protected ApiEndpoint editSelectedItem(ApiEndpoint selectedValue) {
            ApiEndpointDialog dialog = new ApiEndpointDialog(selectedValue);
            if (dialog.showAndGet()) {
                ApiEndpoint endpoint = dialog.getEndpoint();
                String password = dialog.getPassword();
                if (password != null && !password.isEmpty()) {
                    getEndpointService().storePassword(endpoint, password);
                }
                // Update the endpoint in the modifiedEndpoints list
                for (int i = 0; i < modifiedEndpoints.size(); i++) {
                    if (modifiedEndpoints.get(i).getId().equals(endpoint.getId())) {
                        modifiedEndpoints.set(i, endpoint);
                        break;
                    }
                }
                // Immediately persist the endpoint changes to the service
                getEndpointService().updateEndpoint(endpoint);
                return endpoint;
            }
            return null;
        }

        protected String getItemText(ApiEndpoint endpoint) {
            return endpoint.getName();
        }

        // This method is called by the parent class when an item is deleted
        protected void removeSelectedItem(ApiEndpoint selectedValue) {
            LOG.info("removeSelectedItem called with selectedValue: " + (selectedValue != null ? selectedValue.getName() : "null"));

            // Use the provided selectedValue parameter
            if (selectedValue != null) {
                LOG.info("Removing endpoint from UI: " + selectedValue.getName() + " (ID: " + selectedValue.getId() + ")");
                LOG.info("Current modifiedEndpoints before removal: " + modifiedEndpoints.size());
                for (ApiEndpoint ep : modifiedEndpoints) {
                    LOG.info("  - " + ep.getName() + " (ID: " + ep.getId() + ")");
                }

                // Remove the endpoint from the modifiedEndpoints list
                boolean removedFromList = modifiedEndpoints.removeIf(e -> e.getId().equals(selectedValue.getId()));
                if (removedFromList) {
                    LOG.info("Endpoint removed from modifiedEndpoints list: " + selectedValue.getName());
                    LOG.info("Current modifiedEndpoints after removal: " + modifiedEndpoints.size());
                    for (ApiEndpoint ep : modifiedEndpoints) {
                        LOG.info("  - " + ep.getName() + " (ID: " + ep.getId() + ")");
                    }

                    // Immediately persist the endpoint removal to the service
                    LOG.info("Calling endpointService.removeEndpoint to persist the removal");
                    getEndpointService().removeEndpoint(selectedValue);
                } else {
                    LOG.warn("Failed to remove endpoint from modifiedEndpoints list: " + selectedValue.getName());
                }
            } else {
                LOG.warn("Attempted to remove null endpoint");
            }
        }

        protected ApiEndpoint getSelectedValue() {
            return (ApiEndpoint) myList.getSelectedValue();
        }

        public void resetFromSettings() {
            // The list given to the panel is a reference to modifiedEndpoints, so we clear and refill it.
            myListModel.removeAllElements();
            myListModel.addAll(getEndpointService().getEndpoints());
        }
    }
}
