package com.chiplueck.apieditor.actions;

import com.chiplueck.apieditor.api.ApiClient;
import com.chiplueck.apieditor.api.HttpApiClient;
import com.chiplueck.apieditor.fs.ApiEditorFileSystem;
import com.chiplueck.apieditor.model.ApiEndpoint;
import com.chiplueck.apieditor.model.RemoteProgram;
import com.chiplueck.apieditor.services.ApiEndpointService;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.ui.popup.PopupStep;
import com.intellij.openapi.ui.popup.util.BaseListPopupStep;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.List;

/**
 * Action for connecting to an API endpoint and browsing programs.
 */
public class ConnectAction extends AnAction {
    private static final Logger LOG = Logger.getInstance(ConnectAction.class);
    private final ApiEndpointService endpointService = ApiEndpointService.getInstance();

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) return;

        List<ApiEndpoint> endpoints = endpointService.getEndpoints();
        if (endpoints.isEmpty()) {
            Messages.showInfoMessage(project,
                    "No API endpoints configured. Please add an endpoint in Settings -> API Editor Settings.",
                    "No Endpoints");
            return;
        }

        // Show endpoint selection popup
        JBPopupFactory.getInstance()
                .createListPopup(new BaseListPopupStep<ApiEndpoint>("Select API Endpoint", endpoints) {
                    @Override
                    public @NotNull String getTextFor(ApiEndpoint endpoint) {
                        return endpoint.getName();
                    }

                    @Override
                    public @Nullable PopupStep<?> onChosen(ApiEndpoint endpoint, boolean finalChoice) {
                        if (finalChoice) {
                            ApplicationManager.getApplication().invokeLater(() -> connectToEndpoint(project, endpoint));
                        }
                        return FINAL_CHOICE;
                    }
                })
                .showCenteredInCurrentWindow(project);
    }

    private void connectToEndpoint(Project project, ApiEndpoint endpoint) {
        LOG.info("Connecting to endpoint: " + endpoint.getName() + " (" + endpoint.getUrl() + ")");

        ProgressManager.getInstance().run(new Task.Backgroundable(project, "Connecting to API Endpoint") {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                indicator.setIndeterminate(true);
                try {
                    // Always create a new client and fetch the latest program list
                    ApiClient apiClient = new HttpApiClient(endpoint);
                    List<RemoteProgram> programs = apiClient.listPrograms();
                    LOG.info("Successfully connected to endpoint: " + endpoint.getName() + ", found " + programs.size() + " programs");

                    ApplicationManager.getApplication().invokeLater(() -> {
                        showProgramList(project, endpoint, programs);
                    });
                } catch (IOException ex) {
                    LOG.error("Failed to connect to API endpoint: " + endpoint.getName(), ex);

                    ApplicationManager.getApplication().invokeLater(() -> {
                        String errorMessage = "Failed to connect to API endpoint: " + endpoint.getUrl();

                        // Provide more specific error messages based on the exception
                        if (ex.getMessage().contains("Connection refused") || ex.getMessage().contains("ConnectException")) {
                            errorMessage += "\n\nThe server appears to be offline or unreachable. Please check that the server is running and that you can access it from your network.";
                        } else if (ex.getMessage().contains("Authentication failed") || ex.getMessage().contains("401")) {
                            errorMessage += "\n\nAuthentication failed. Please check your username and password in the API Editor Settings.";
                        } else if (ex.getMessage().contains("timeout") || ex.getMessage().contains("timed out")) {
                            errorMessage += "\n\nThe connection timed out. The server might be slow or overloaded.";
                        } else if (ex.getMessage().contains("Unknown host") || ex.getMessage().contains("UnknownHostException")) {
                            errorMessage += "\n\nThe hostname could not be resolved. Please check that the URL is correct.";
                        } else {
                            errorMessage += "\n\nError details: " + ex.getMessage();
                        }

                        Messages.showErrorDialog(project, errorMessage, "Connection Error");
                    });
                }
            }
        });
    }

    private void showProgramList(Project project, ApiEndpoint endpoint, List<RemoteProgram> programs) {
        if (programs.isEmpty()) {
            Messages.showInfoMessage(project, "No programs found on the API endpoint.", "No Programs");
            return;
        }

        // Show program selection popup
        JBPopupFactory.getInstance()
                .createListPopup(new BaseListPopupStep<RemoteProgram>("Select Program", programs) {
                    @Override
                    public @NotNull String getTextFor(RemoteProgram program) {
                        return program.getFullName();
                    }

                    @Override
                    public @Nullable PopupStep<?> onChosen(RemoteProgram program, boolean finalChoice) {
                        if (finalChoice) {
                            ApplicationManager.getApplication().invokeLater(() -> openProgram(project, endpoint, program));
                        }
                        return FINAL_CHOICE;
                    }
                })
                .showCenteredInCurrentWindow(project);
    }

    private void openProgram(Project project, ApiEndpoint endpoint, RemoteProgram program) {
        LOG.info("Opening program: " + program.getFullName() + " from endpoint: " + endpoint.getName());

        ProgressManager.getInstance().run(new Task.Backgroundable(project, "Loading Program") {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                indicator.setIndeterminate(true);
                try {
                    // Use the ApiEditorFileSystem service to open the program
                    ApiEditorFileSystem.getInstance().openProgram(project, endpoint, program);
                    LOG.info("Successfully opened program: " + program.getFullName());
                } catch (IOException ex) {
                    LOG.error("Failed to load program: " + program.getFullName(), ex);

                    ApplicationManager.getApplication().invokeLater(() -> {
                        String errorMessage = "Failed to load program: " + program.getFullName();

                        // Provide more specific error messages based on the exception
                        if (ex.getMessage().contains("Connection refused") || ex.getMessage().contains("ConnectException")) {
                            errorMessage += "\n\nThe server appears to be offline or unreachable. Please check that the server is running and that you can access it from your network.";
                        } else if (ex.getMessage().contains("Authentication failed") || ex.getMessage().contains("401")) {
                            errorMessage += "\n\nAuthentication failed. Please check your username and password in the API Editor Settings.";
                        } else if (ex.getMessage().contains("timeout") || ex.getMessage().contains("timed out")) {
                            errorMessage += "\n\nThe connection timed out. The server might be slow or overloaded.";
                        } else if (ex.getMessage().contains("not found") || ex.getMessage().contains("404")) {
                            errorMessage += "\n\nThe program could not be found on the server. It may have been deleted or moved.";
                        } else {
                            errorMessage += "\n\nError details: " + ex.getMessage();
                        }

                        Messages.showErrorDialog(project, errorMessage, "Load Error");
                    });
                }
            }
        });
    }
}
