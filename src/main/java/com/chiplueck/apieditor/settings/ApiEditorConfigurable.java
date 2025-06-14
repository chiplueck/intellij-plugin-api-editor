package com.chiplueck.apieditor.settings;

import com.chiplueck.apieditor.model.ApiEndpoint;
import com.chiplueck.apieditor.services.ApiEndpointService;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.ui.ToolbarDecorator;
import com.intellij.ui.components.JBList;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import java.awt.BorderLayout;
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

    private class ApiEndpointListPanel extends JPanel {
        private final JBList<ApiEndpoint> endpointList;
        private final DefaultListModel<ApiEndpoint> listModel;

        public ApiEndpointListPanel(List<ApiEndpoint> endpoints) {
            super(new BorderLayout());

            // Create the list model and populate it with endpoints
            listModel = new DefaultListModel<>();
            for (ApiEndpoint endpoint : endpoints) {
                listModel.addElement(endpoint);
            }

            // Create the list with the model
            endpointList = new JBList<>(listModel);
            endpointList.setCellRenderer(new DefaultListCellRenderer() {
                @Override
                public java.awt.Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                    super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                    if (value instanceof ApiEndpoint) {
                        setText(((ApiEndpoint) value).getName());
                    }
                    return this;
                }
            });

            // Create the toolbar decorator
            ToolbarDecorator decorator = ToolbarDecorator.createDecorator(endpointList);

            // Add action
            decorator.setAddAction(button -> {
                ApiEndpointDialog dialog = new ApiEndpointDialog(null);
                if (dialog.showAndGet()) {
                    ApiEndpoint endpoint = dialog.getEndpoint();
                    String password = dialog.getPassword();
                    if (password != null && !password.isEmpty()) {
                        getEndpointService().storePassword(endpoint, password);
                    }

                    // Add the endpoint to the list model and modifiedEndpoints
                    listModel.addElement(endpoint);
                    modifiedEndpoints.add(endpoint);

                    // Select the new endpoint
                    endpointList.setSelectedValue(endpoint, true);
                }
            });

            // Edit action
            decorator.setEditAction(button -> {
                ApiEndpoint selectedEndpoint = endpointList.getSelectedValue();
                if (selectedEndpoint != null) {
                    ApiEndpointDialog dialog = new ApiEndpointDialog(selectedEndpoint);
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

                        // Update the list model
                        int index = endpointList.getSelectedIndex();
                        listModel.set(index, endpoint);

                        // Immediately persist the endpoint changes to the service
                        getEndpointService().updateEndpoint(endpoint);

                        // Reselect the edited item
                        endpointList.setSelectedValue(endpoint, true);
                    }
                }
            });

            // Remove action
            decorator.setRemoveAction(button -> {
                ApiEndpoint selectedEndpoint = endpointList.getSelectedValue();
                if (selectedEndpoint != null) {
                    LOG.info("Removing endpoint from UI: " + selectedEndpoint.getName() + " (ID: " + selectedEndpoint.getId() + ")");

                    // Remove from the list model
                    listModel.removeElement(selectedEndpoint);

                    // Remove from modifiedEndpoints
                    boolean removedFromList = modifiedEndpoints.removeIf(e -> e.getId().equals(selectedEndpoint.getId()));
                    if (removedFromList) {
                        LOG.info("Endpoint removed from modifiedEndpoints list: " + selectedEndpoint.getName());

                        // Immediately persist the endpoint removal to the service
                        LOG.info("Calling endpointService.removeEndpoint to persist the removal");
                        getEndpointService().removeEndpoint(selectedEndpoint);
                    } else {
                        LOG.warn("Failed to remove endpoint from modifiedEndpoints list: " + selectedEndpoint.getName());
                    }
                }
            });

            // Add move up/down actions for ordering endpoints
            decorator.setMoveUpAction(button -> {
                ApiEndpoint selectedEndpoint = endpointList.getSelectedValue();
                if (selectedEndpoint != null) {
                    int index = endpointList.getSelectedIndex();
                    if (index > 0) {
                        // Swap with the previous element in the model
                        ApiEndpoint temp = listModel.get(index - 1);
                        listModel.set(index - 1, selectedEndpoint);
                        listModel.set(index, temp);

                        // Swap in the modifiedEndpoints list
                        modifiedEndpoints.set(index - 1, selectedEndpoint);
                        modifiedEndpoints.set(index, temp);

                        // Reselect the moved item
                        endpointList.setSelectedValue(selectedEndpoint, true);
                    }
                }
            });

            decorator.setMoveDownAction(button -> {
                ApiEndpoint selectedEndpoint = endpointList.getSelectedValue();
                if (selectedEndpoint != null) {
                    int index = endpointList.getSelectedIndex();
                    if (index < listModel.size() - 1) {
                        // Swap with the next element in the model
                        ApiEndpoint temp = listModel.get(index + 1);
                        listModel.set(index + 1, selectedEndpoint);
                        listModel.set(index, temp);

                        // Swap in the modifiedEndpoints list
                        modifiedEndpoints.set(index + 1, selectedEndpoint);
                        modifiedEndpoints.set(index, temp);

                        // Reselect the moved item
                        endpointList.setSelectedValue(selectedEndpoint, true);
                    }
                }
            });

            // Add the decorated panel to this panel
            add(decorator.createPanel(), BorderLayout.CENTER);
        }

        public void resetFromSettings() {
            // Clear and refill the list model
            listModel.clear();
            for (ApiEndpoint endpoint : getEndpointService().getEndpoints()) {
                listModel.addElement(endpoint);
            }
        }
    }
}
