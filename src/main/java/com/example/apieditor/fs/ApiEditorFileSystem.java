package com.example.apieditor.fs;

import com.example.apieditor.api.ApiClient;
import com.example.apieditor.api.HttpApiClient;
import com.example.apieditor.model.ApiEndpoint;
import com.example.apieditor.model.RemoteProgram;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for managing the API Editor virtual file system.
 */
@Service
public final class ApiEditorFileSystem {
    private static final Logger LOG = Logger.getInstance(ApiEditorFileSystem.class);

    private final Map<String, ApiEndpoint> activeEndpoints = new ConcurrentHashMap<>();
    private final Map<String, Map<String, RemoteProgram>> programCache = new ConcurrentHashMap<>();
    private final Map<String, VirtualFile> virtualFiles = new ConcurrentHashMap<>();

    public static ApiEditorFileSystem getInstance() {
        return ApplicationManager.getApplication().getService(ApiEditorFileSystem.class);
    }

    /**
     * Connects to an API endpoint and caches its programs.
     *
     * @param endpoint The API endpoint to connect to
     * @return The list of programs from the endpoint
     * @throws IOException If an error occurs during the API call
     */
    public List<RemoteProgram> connectToEndpoint(ApiEndpoint endpoint) throws IOException {
        ApiClient apiClient = new HttpApiClient(endpoint);
        List<RemoteProgram> programs = apiClient.listPrograms();

        // Cache the endpoint and its programs
        activeEndpoints.put(endpoint.getId(), endpoint);
        Map<String, RemoteProgram> programMap = new HashMap<>();
        for (RemoteProgram program : programs) {
            programMap.put(program.getId(), program);
        }
        programCache.put(endpoint.getId(), programMap);

        return programs;
    }

    /**
     * Refreshes the programs for an API endpoint.
     *
     * @param endpoint The API endpoint to refresh
     * @return The updated list of programs
     * @throws IOException If an error occurs during the API call
     */
    public List<RemoteProgram> refreshEndpoint(ApiEndpoint endpoint) throws IOException {
        ApiClient apiClient = new HttpApiClient(endpoint);
        List<RemoteProgram> programs = apiClient.listPrograms();

        // Update the program cache
        Map<String, RemoteProgram> programMap = new HashMap<>();
        for (RemoteProgram program : programs) {
            programMap.put(program.getId(), program);
        }
        programCache.put(endpoint.getId(), programMap);

        return programs;
    }

    /**
     * Opens a remote program in the editor.
     *
     * @param project The current project
     * @param endpoint The API endpoint
     * @param program The program to open
     * @throws IOException If an error occurs during the API call
     */
    public void openProgram(Project project, ApiEndpoint endpoint, RemoteProgram program) throws IOException {
        ApiClient apiClient = new HttpApiClient(endpoint);
        RemoteProgram fullProgram = apiClient.getProgram(program.getId());

        // Create a virtual file for the program
        String key = createFileKey(endpoint.getId(), program.getId());
        VirtualFile file = virtualFiles.computeIfAbsent(key, k -> new ApiEditorVirtualFile(endpoint, fullProgram));

        // Update the file content if it already exists
        if (file instanceof ApiEditorVirtualFile) {
            ((ApiEditorVirtualFile) file).updateProgram(fullProgram);
        }

        // Open the file in the editor
        ApplicationManager.getApplication().invokeLater(() -> {
            FileEditorManager.getInstance(project).openFile(file, true);
        });
    }

    /**
     * Saves a program to the remote API.
     *
     * @param file The virtual file to save
     * @throws IOException If an error occurs during the API call
     */
    public void saveProgram(ApiEditorVirtualFile file) throws IOException {
        ApiEndpoint endpoint = file.getEndpoint();
        RemoteProgram program = file.getProgram();

        ApiClient apiClient = new HttpApiClient(endpoint);
        RemoteProgram savedProgram = apiClient.saveProgram(program);

        // Update the program in the cache
        Map<String, RemoteProgram> programMap = programCache.get(endpoint.getId());
        if (programMap != null) {
            programMap.put(savedProgram.getId(), savedProgram);
        }

        // Update the file with the saved program
        file.updateProgram(savedProgram);
    }

    /**
     * Gets a cached program.
     *
     * @param endpointId The ID of the API endpoint
     * @param programId The ID of the program
     * @return The cached program, or null if not found
     */
    @Nullable
    public RemoteProgram getCachedProgram(String endpointId, String programId) {
        Map<String, RemoteProgram> programMap = programCache.get(endpointId);
        return programMap != null ? programMap.get(programId) : null;
    }

    /**
     * Gets a cached endpoint.
     *
     * @param endpointId The ID of the API endpoint
     * @return The cached endpoint, or null if not found
     */
    @Nullable
    public ApiEndpoint getCachedEndpoint(String endpointId) {
        return activeEndpoints.get(endpointId);
    }

    /**
     * Creates a unique key for a file based on endpoint and program IDs.
     *
     * @param endpointId The ID of the API endpoint
     * @param programId The ID of the program
     * @return A unique key
     */
    @NotNull
    private String createFileKey(String endpointId, String programId) {
        return endpointId + ":" + programId;
    }
}
