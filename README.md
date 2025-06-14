# IntelliJ API Editor Plugin

A plugin for IntelliJ IDEA that allows reading and writing code from a remote API source, similar to the remote host feature but using an API instead of SFTP.

## Features

- Configure API endpoints with secure credential storage
- Browse and edit remote code via API
- Support for various file types based on file extensions
- One-dimensional program listing (no folders)

## Building the Plugin

### Prerequisites

- IntelliJ IDEA (Community or Ultimate)
- Java Development Kit (JDK) 11 or later
- Gradle

### Development Notes

- The plugin uses version 1.8.0 of the IntelliJ Gradle plugin.
- The project now includes a Gradle wrapper (gradlew) configured to use Gradle 7.6, which is compatible with the IntelliJ Gradle plugin version 1.8.0.
- If you encounter build errors related to compatibility between the IntelliJ Gradle plugin and your Gradle version, you may need to adjust the plugin version in build.gradle.
- Older versions (like 1.5.2, 1.5.3, and 1.6.0) may cause errors with the ArchivePublishArtifact constructor.
- Some newer versions (like 1.13.3) may cause errors with "MemoizedProvider overrides final method AbstractMinimalProvider.toString()".
- Version 1.8.0 has been tested and works successfully with the current Gradle setup.
- If you encounter issues when refreshing the Gradle project in IntelliJ IDEA, make sure to use the Gradle wrapper (gradlew) instead of the system's Gradle installation.
- The plugin is configured to use Java 11 compatibility and is compatible with IntelliJ IDEA 2022.1 through 2025.1 (build 251).
- If you encounter class version errors (e.g., "class file has wrong version"), make sure you're using Java 11 for compilation.

### Build Steps

1. Clone the repository:
   ```
   git clone https://github.com/yourusername/intellij-plugin-api-editor.git
   cd intellij-plugin-api-editor
   ```

2. Build the plugin using the Gradle wrapper:
   ```
   # On Unix-like systems
   ./gradlew buildPlugin

   # On Windows
   gradlew.bat buildPlugin
   ```

3. The plugin will be built in the `build/distributions` directory.

> **Note:** Always use the Gradle wrapper (`./gradlew` or `gradlew.bat`) instead of your system's Gradle installation to avoid compatibility issues.

## Installing the Plugin

1. Open IntelliJ IDEA
2. Go to `File > Settings > Plugins`
3. Click on the gear icon and select `Install Plugin from Disk...`
4. Navigate to the `build/distributions` directory and select the zip file
5. Restart IntelliJ IDEA

## Using the Plugin

### Configuring API Endpoints

1. Go to `File > Settings > API Editor Settings`
2. Click the `+` button to add a new API endpoint
3. Enter the endpoint details:
   - Name: A friendly name for the endpoint
   - URL: The base URL of the API (e.g., `https://api.example.com`)
   - Username: Your API username
   - Password: Your API password
4. Click `OK` to save the endpoint

### Connecting to an API Endpoint

1. Go to the `API Editor` menu in the main menu bar (it should be at the far right of the menu bar, after "Help")
2. Select `Connect to API Endpoint`
3. Choose an endpoint from the list
4. Browse and select a program to open

### Refreshing Programs

1. Go to the `API Editor` menu in the main menu bar (it should be at the far right of the menu bar, after "Help")
2. Select `Refresh Programs`
3. Choose an endpoint from the list to refresh its programs

## API Specification

If you want to create an API that is compatible with this plugin, please refer to the [API Specification](src/main/resources/api_specification.md) document.

## License

This project is licensed under the MIT License - see the LICENSE file for details.
