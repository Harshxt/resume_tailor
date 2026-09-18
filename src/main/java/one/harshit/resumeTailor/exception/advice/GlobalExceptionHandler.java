package one.harshit.resumeTailor.exception.advice;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import one.harshit.resumeTailor.controller.dto.GenericResponse;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<GenericResponse<Void>> handleIllegalArgumentException(IllegalArgumentException ex) {
        log.warn("Bad request: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new GenericResponse<>(false, ex.getMessage()));
    }

    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<GenericResponse<Void>> handleSecurityException(SecurityException ex) {
        log.warn("Security violation: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new GenericResponse<>(false, ex.getMessage()));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<GenericResponse<Void>> handleMaxUploadSizeExceeded(MaxUploadSizeExceededException ex) {
        log.warn("File upload size exceeded: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONTENT_TOO_LARGE)
                .body(new GenericResponse<>(false, "File size exceeds the maximum allowed upload limit"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<GenericResponse<Void>> handleGenericException(Exception ex) {
        log.error("An unexpected error occurred: ", ex);
        String message = ex.getMessage() != null ? ex.getMessage() : "An unexpected error occurred";
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new GenericResponse<>(false, message));
    }
}

