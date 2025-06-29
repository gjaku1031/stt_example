# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is a Spring Boot application that provides Google Speech-to-Text (STT) functionality via a REST API. The application uses Google Cloud Speech-to-Text v2 API to convert audio files to text.

## Key Technologies

- **Spring Boot 3.5.3** - Main framework
- **Java 21** - Language version
- **Google Cloud Speech v2 API** - Speech recognition service
- **Gradle** - Build tool

## Build and Development Commands

```bash
# Build the project
./gradlew build

# Run tests
./gradlew test

# Run a specific test
./gradlew test --tests "TestClassName.testMethodName"

# Run the application
./gradlew bootRun

# Clean build artifacts
./gradlew clean

# Generate JAR file
./gradlew bootJar
```

## Architecture Overview

### Core Components

1. **SttApplication** (src/main/java/com/gco/stt/SttApplication.java) - Spring Boot entry point

2. **GoogleCloudConfig** (src/main/java/com/gco/stt/config/GoogleCloudConfig.java) - Configures Google Cloud Speech client with credentials from environment variables

3. **SpeechRecorderController** (src/main/java/com/gco/stt/controller/SpeechRecorderController.java) - REST endpoint that accepts audio file uploads and returns transcribed text

### Configuration

The application uses environment variables via a `.env` file:
- `GOOGLE_CREDENTIALS_JSON` - Google Cloud service account credentials in JSON format

Key application properties:
- `gcp.project-id` - Google Cloud project ID
- `gcp.location` - Google Cloud location (default: global)
- File upload limits: 10MB max file size

### API Endpoint

- `POST /api/speech/upload` - Accepts multipart file upload with parameter name "audio"
  - Returns: `SpeechResponse` containing success status, message, and transcript

## Important Notes

- The application expects Google Cloud credentials to be provided as a JSON string in the `GOOGLE_CREDENTIALS_JSON` environment variable
- Audio processing uses the "long" model optimized for longer audio files
- Language is set to Korean (ko-KR) by default in the controller
- The application creates dynamic recognizer IDs for each request