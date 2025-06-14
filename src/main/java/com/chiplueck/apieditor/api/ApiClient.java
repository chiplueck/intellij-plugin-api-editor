package com.chiplueck.apieditor.api;

import com.chiplueck.apieditor.model.RemoteProgram;

import java.io.IOException;
import java.util.List;

/**
 * Interface for interacting with the remote API.
 */
public interface ApiClient {
    /**
     * Lists all programs available on the remote API.
     *
     * @return A list of RemoteProgram objects
     * @throws IOException If an error occurs during the API call
     */
    List<RemoteProgram> listPrograms() throws IOException;

    /**
     * Gets the content of a specific program.
     *
     * @param programId The ID of the program to retrieve
     * @return The RemoteProgram with its content populated
     * @throws IOException If an error occurs during the API call
     */
    RemoteProgram getProgram(String programId) throws IOException;

    /**
     * Saves the content of a program to the remote API.
     *
     * @param program The program to save
     * @return The updated RemoteProgram
     * @throws IOException If an error occurs during the API call
     */
    RemoteProgram saveProgram(RemoteProgram program) throws IOException;
}
