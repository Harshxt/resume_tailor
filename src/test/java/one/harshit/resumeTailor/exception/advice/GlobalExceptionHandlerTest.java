package one.harshit.resumeTailor.exception.advice;

import one.harshit.resumeTailor.controller.dto.GenericResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler exceptionHandler;

    @BeforeEach
    void setUp() {
        exceptionHandler = new GlobalExceptionHandler();
    }

    @Test
    @DisplayName("Test 16: Handle IllegalArgumentException returns 400 Bad Request with error message")
    void handleIllegalArgumentException_ReturnsBadRequest() {
        // Given
        String errorMessage = "Uploaded file is empty";
        IllegalArgumentException exception = new IllegalArgumentException(errorMessage);

        // When
        ResponseEntity<GenericResponse<Void>> response = exceptionHandler.handleIllegalArgumentException(exception);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().success()).isFalse();
        assertThat(response.getBody().message()).isEqualTo(errorMessage);
        assertThat(response.getBody().data()).isNull();
        assertThat(response.getBody().timestamp()).isNotNull();
    }

    @Test
    @DisplayName("Test 17: Handle SecurityException returns 403 Forbidden with security error message")
    void handleSecurityException_ReturnsForbidden() {
        // Given
        String errorMessage = "Cannot store file outside target directory.";
        SecurityException exception = new SecurityException(errorMessage);

        // When
        ResponseEntity<GenericResponse<Void>> response = exceptionHandler.handleSecurityException(exception);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().success()).isFalse();
        assertThat(response.getBody().message()).isEqualTo(errorMessage);
        assertThat(response.getBody().data()).isNull();
    }

    @Test
    @DisplayName("Test 18: Handle MaxUploadSizeExceededException returns 413 Content Too Large")
    void handleMaxUploadSizeExceeded_ReturnsPayloadTooLarge() {
        // Given (10MB limit exceeded)
        MaxUploadSizeExceededException exception = new MaxUploadSizeExceededException(10485760);

        // When
        ResponseEntity<GenericResponse<Void>> response = exceptionHandler.handleMaxUploadSizeExceeded(exception);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONTENT_TOO_LARGE);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().success()).isFalse();
        assertThat(response.getBody().message()).isEqualTo("File size exceeds the maximum allowed upload limit");
    }

    @Test
    @DisplayName("Test 19: Handle generic Exception returns 500 Internal Server Error with message or fallback")
    void handleGenericException_ReturnsInternalServerError() {
        // Case A: Exception with a message
        Exception exWithMessage = new RuntimeException("Unexpected database timeout");
        ResponseEntity<GenericResponse<Void>> responseA = exceptionHandler.handleGenericException(exWithMessage);

        assertThat(responseA.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(responseA.getBody()).isNotNull();
        assertThat(responseA.getBody().success()).isFalse();
        assertThat(responseA.getBody().message()).isEqualTo("Unexpected database timeout");

        // Case B: Exception with null message uses fallback
        Exception exWithoutMessage = new NullPointerException();
        ResponseEntity<GenericResponse<Void>> responseB = exceptionHandler.handleGenericException(exWithoutMessage);

        assertThat(responseB.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(responseB.getBody()).isNotNull();
        assertThat(responseB.getBody().success()).isFalse();
        assertThat(responseB.getBody().message()).isEqualTo("An unexpected error occurred");
    }
}