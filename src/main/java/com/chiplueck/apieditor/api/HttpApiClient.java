package com.chiplueck.apieditor.api;

import com.chiplueck.apieditor.model.ApiEndpoint;
import com.chiplueck.apieditor.model.RemoteProgram;
import com.chiplueck.apieditor.services.ApiEndpointService;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.intellij.openapi.diagnostic.Logger;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/**
 * Implementation of ApiClient that communicates with the remote API via HTTP.
 */
public class HttpApiClient implements ApiClient {
    private static final Logger LOG = Logger.getInstance(HttpApiClient.class);
    private static final String PROGRAMS_ENDPOINT = "/";
    private static final String PROGRAM_ENDPOINT = "/%s";
    private static final int TIMEOUT_MS = 10000;

    private final ApiEndpoint endpoint;
    private final Gson gson;
    private final ApiEndpointService endpointService;

    public HttpApiClient(ApiEndpoint endpoint) {
        this.endpoint = endpoint;
        this.gson = new Gson();
        this.endpointService = ApiEndpointService.getInstance();
    }

    @Override
    public List<RemoteProgram> listPrograms() throws IOException {
        String response = sendRequest("GET", PROGRAMS_ENDPOINT, null);
        JsonObject jsonObject = gson.fromJson(response, JsonObject.class);

        if (!jsonObject.has("programs")) {
            throw new IOException("Invalid response format: 'programs' field not found");
        }

        JsonArray programsArray = jsonObject.getAsJsonArray("programs");
        List<RemoteProgram> programs = new ArrayList<>();

        for (JsonElement element : programsArray) {
            RemoteProgram program = gson.fromJson(element, RemoteProgram.class);
            programs.add(program);
        }

        return programs;
    }

    @Override
    public RemoteProgram getProgram(String programId) throws IOException {
        String endpoint = String.format(PROGRAM_ENDPOINT, programId);
        String response = sendRequest("GET", endpoint, null);
        JsonObject jsonObject = gson.fromJson(response, JsonObject.class);

        if (!jsonObject.has("program")) {
            throw new IOException("Invalid response format: 'program' field not found");
        }

        JsonObject programObject = jsonObject.getAsJsonObject("program");
        return gson.fromJson(programObject, RemoteProgram.class);
    }

    @Override
    public RemoteProgram saveProgram(RemoteProgram program) throws IOException {
        String endpoint = String.format(PROGRAM_ENDPOINT, program.getId());
        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("content", program.getContent());

        String response = sendRequest("PUT", endpoint, requestBody.toString());
        JsonObject jsonObject = gson.fromJson(response, JsonObject.class);

        if (!jsonObject.has("program")) {
            throw new IOException("Invalid response format: 'program' field not found");
        }

        JsonObject programObject = jsonObject.getAsJsonObject("program");
        return gson.fromJson(programObject, RemoteProgram.class);
    }

    /**
     * Sends an HTTP request to the API.
     *
     * @param method The HTTP method (GET, PUT, etc.)
     * @param path The API path
     * @param requestBody The request body (for PUT/POST requests)
     * @return The response body as a string
     * @throws IOException If an error occurs during the request
     */
    private String sendRequest(String method, String path, String requestBody) throws IOException {
        URL url = new URL(endpoint.getUrl() + path);
        LOG.info("Sending " + method + " request to " + url);

        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod(method);
            connection.setConnectTimeout(TIMEOUT_MS);
            connection.setReadTimeout(TIMEOUT_MS);

            // Set headers
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Accept", "application/json");

            // Set authentication header
            String username = endpoint.getUsername();
            String password = endpointService.getPassword(endpoint);
            if (username != null && password != null) {
                String auth = username + ":" + password;
                String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));
                connection.setRequestProperty("Authorization", "Basic " + encodedAuth);
                LOG.debug("Added authentication header for user: " + username);
            } else {
                LOG.warn("Missing credentials for endpoint: " + endpoint.getName());
            }

            // Write request body if needed
            if (requestBody != null) {
                connection.setDoOutput(true);
                try (OutputStream os = connection.getOutputStream()) {
                    byte[] input = requestBody.getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                    LOG.debug("Wrote request body: " + requestBody);
                }
            }

            // Handle response
            int responseCode = connection.getResponseCode();
            LOG.info("Received response code: " + responseCode + " from " + url);

            if (responseCode >= 200 && responseCode < 300) {
                try (BufferedReader br = new BufferedReader(
                        new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                    StringBuilder response = new StringBuilder();
                    String responseLine;
                    while ((responseLine = br.readLine()) != null) {
                        response.append(responseLine.trim());
                    }
                    String responseStr = response.toString();
                    LOG.debug("Received response: " + responseStr);
                    return responseStr;
                }
            } else {
                String errorMessage;
                try (BufferedReader br = new BufferedReader(
                        new InputStreamReader(connection.getErrorStream(), StandardCharsets.UTF_8))) {
                    StringBuilder response = new StringBuilder();
                    String responseLine;
                    while ((responseLine = br.readLine()) != null) {
                        response.append(responseLine.trim());
                    }
                    errorMessage = response.toString();
                }

                String detailedError = "API request failed with status " + responseCode;
                if (responseCode == 401) {
                    detailedError += ": Authentication failed. Please check your username and password.";
                } else if (responseCode == 403) {
                    detailedError += ": Access forbidden. You don't have permission to access this resource.";
                } else if (responseCode == 404) {
                    detailedError += ": Resource not found. The requested endpoint does not exist.";
                } else if (responseCode >= 500) {
                    detailedError += ": Server error. Please try again later or contact the API administrator.";
                }

                if (!errorMessage.isEmpty()) {
                    detailedError += " Server message: " + errorMessage;
                }

                LOG.error(detailedError);
                throw new IOException(detailedError);
            }
        } catch (IOException e) {
            String errorMsg = "Connection error with endpoint " + endpoint.getName() + " (" + url + "): " + e.getMessage();
            LOG.error(errorMsg, e);
            throw new IOException(errorMsg, e);
        }
    }
}
