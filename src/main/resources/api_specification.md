# API Specification for IntelliJ API Editor Plugin

This document describes the API requirements for servers that want to be compatible with the IntelliJ API Editor plugin.

## Overview

The IntelliJ API Editor plugin allows users to browse, edit, and save code stored on a remote server via a REST API. The plugin is designed to be similar to the built-in remote host feature in IntelliJ IDEA, but uses an API instead of SFTP.

## Authentication

The API should support HTTP Basic Authentication. The plugin will send the username and password provided by the user in the standard HTTP Authorization header:

```
Authorization: Basic <base64-encoded-credentials>
```

Where `<base64-encoded-credentials>` is the Base64 encoding of `username:password`.

## API Endpoints

The API must implement the following endpoints:

### 1. List Programs

**Endpoint:** `GET /`

**Description:** Returns a list of all programs available on the server.

**Response Format:**
```json
{
  "programs": [
    {
      "id": "unique-program-id-1",
      "name": "program1",
      "extension": "java",
      "lastModified": 1623456789000
    },
    {
      "id": "unique-program-id-2",
      "name": "program2",
      "extension": "py",
      "lastModified": 1623456790000
    }
  ]
}
```

**Fields:**
- `id`: A unique identifier for the program
- `name`: The name of the program (without extension)
- `extension`: The file extension (without the leading dot)
- `lastModified`: The last modification timestamp in milliseconds since epoch

### 2. Get Program

**Endpoint:** `GET /{programId}`

**Description:** Returns the content of a specific program.

**Response Format:**
```json
{
  "program": {
    "id": "unique-program-id-1",
    "name": "program1",
    "extension": "java",
    "content": "public class Program1 {\n    public static void main(String[] args) {\n        System.out.println(\"Hello, World!\");\n    }\n}",
    "lastModified": 1623456789000
  }
}
```

**Fields:**
- Same as List Programs, plus:
- `content`: The full text content of the program

### 3. Save Program

**Endpoint:** `PUT /{programId}`

**Description:** Saves the content of a program.

**Request Format:**
```json
{
  "content": "public class Program1 {\n    public static void main(String[] args) {\n        System.out.println(\"Hello, Updated World!\");\n    }\n}"
}
```

**Response Format:**
```json
{
  "program": {
    "id": "unique-program-id-1",
    "name": "program1",
    "extension": "java",
    "content": "public class Program1 {\n    public static void main(String[] args) {\n        System.out.println(\"Hello, Updated World!\");\n    }\n}",
    "lastModified": 1623456799000
  }
}
```

## Error Handling

The API should return appropriate HTTP status codes for different error conditions:

- `200 OK`: Successful request
- `400 Bad Request`: Invalid request parameters
- `401 Unauthorized`: Authentication failed
- `403 Forbidden`: Authentication succeeded but the user doesn't have permission
- `404 Not Found`: The requested resource was not found
- `500 Internal Server Error`: An unexpected error occurred on the server

Error responses should include a JSON body with an error message:

```json
{
  "error": "Error message describing what went wrong"
}
```

## Notes for Implementers

1. The API is one-dimensional; only programs are listed and not folders.
2. The program's file extension is included, and the IntelliJ plugin will open the appropriate editor type based on the file extension.
3. All API responses should use UTF-8 encoding.
4. The API should support CORS if it will be accessed from web applications.
5. For security, consider implementing rate limiting and other security measures.
