package com.example.apieditor.settings;

import com.example.apieditor.model.ApiEndpoint;
import com.example.apieditor.services.ApiEndpointService;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.FormBuilder;
import com.intellij.util.ui.UI;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;

/**
 * Dialog for adding or editing an API endpoint.
 */
public class ApiEndpointDialog extends DialogWrapper {
    private final JBTextField nameField = new JBTextField();
    private final JBTextField urlField = new JBTextField();
    private final JBTextField usernameField = new JBTextField();
    private final JPasswordField passwordField = new JPasswordField();
    private final ApiEndpoint originalEndpoint;
    private ApiEndpoint resultEndpoint;

    public ApiEndpointDialog(@Nullable ApiEndpoint endpoint) {
        super(true); // Modal dialog
        this.originalEndpoint = endpoint;
        setTitle(endpoint == null ? "Add API Endpoint" : "Edit API Endpoint");
        init();
        initFields();
    }

    private void initFields() {
        if (originalEndpoint != null) {
            nameField.setText(originalEndpoint.getName());
            urlField.setText(originalEndpoint.getUrl());
            usernameField.setText(originalEndpoint.getUsername());

            // Load password from secure storage if editing an existing endpoint
            String password = ApiEndpointService.getInstance().getPassword(originalEndpoint);
            if (password != null) {
                passwordField.setText(password);
            }
        }
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        JPanel panel = FormBuilder.createFormBuilder()
                .addLabeledComponent(new JBLabel("Name:"), nameField, true)
                .addLabeledComponent(new JBLabel("URL:"), urlField, true)
                .addLabeledComponent(new JBLabel("Username:"), usernameField, true)
                .addLabeledComponent(new JBLabel("Password:"), passwordField, true)
                .addComponentFillVertically(new JPanel(), 0)
                .getPanel();

        panel.setPreferredSize(new Dimension(400, 200));
        return panel;
    }

    @Nullable
    @Override
    protected ValidationInfo doValidate() {
        if (nameField.getText().trim().isEmpty()) {
            return new ValidationInfo("Name cannot be empty", nameField);
        }
        if (urlField.getText().trim().isEmpty()) {
            return new ValidationInfo("URL cannot be empty", urlField);
        }
        if (!urlField.getText().startsWith("http://") && !urlField.getText().startsWith("https://")) {
            return new ValidationInfo("URL must start with http:// or https://", urlField);
        }
        return null;
    }

    @Override
    protected void doOKAction() {
        resultEndpoint = originalEndpoint != null ? originalEndpoint : new ApiEndpoint();
        resultEndpoint.setName(nameField.getText().trim());
        resultEndpoint.setUrl(urlField.getText().trim());
        resultEndpoint.setUsername(usernameField.getText().trim());
        super.doOKAction();
    }

    public ApiEndpoint getEndpoint() {
        return resultEndpoint;
    }

    public String getPassword() {
        return new String(passwordField.getPassword());
    }
}
