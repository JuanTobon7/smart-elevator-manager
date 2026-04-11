package co.edu.unillanos.elevator.infrastructure.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO genérico para respuestas del API
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GenericResponseDTO<T> {
    private boolean success;
    private String message;
    private T data;
    private long timestamp;
    
    public static <T> GenericResponseDTO<T> ok(T data, String message) {
        return GenericResponseDTO.<T>builder()
                .success(true)
                .message(message)
                .data(data)
                .timestamp(System.currentTimeMillis())
                .build();
    }
    
    public static <T> GenericResponseDTO<T> error(String message) {
        return GenericResponseDTO.<T>builder()
                .success(false)
                .message(message)
                .timestamp(System.currentTimeMillis())
                .build();
    }
}
