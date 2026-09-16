package one.harshit.resumeTailor.controller.dto;

import java.time.LocalDateTime;

public record GenericResponse<T>(
        boolean success,
        String message,
        T data,
        LocalDateTime timestamp

) {

    public GenericResponse(boolean success, String message) {
        this(success, message, null, LocalDateTime.now());
    }

    public GenericResponse(boolean success, String message, T data) {
        this(success, message, data, LocalDateTime.now());
    }

}
