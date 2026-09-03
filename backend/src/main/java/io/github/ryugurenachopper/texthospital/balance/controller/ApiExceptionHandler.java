package io.github.ryugurenachopper.texthospital.balance.controller;

import io.github.ryugurenachopper.texthospital.balance.governance.RandomResamplingException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.io.IOException;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(RandomResamplingException.class)
    public ResponseEntity<ApiError> handleResampling(RandomResamplingException exception) {
        return ResponseEntity.badRequest().body(new ApiError(exception.getReason(), exception.getMessage()));
    }

    @ExceptionHandler({IllegalArgumentException.class, HttpMessageNotReadableException.class})
    public ResponseEntity<ApiError> handleInvalidInput(Exception exception) {
        String message = exception instanceof HttpMessageNotReadableException
                ? "Request body is not valid JSON" : exception.getMessage();
        return ResponseEntity.badRequest().body(new ApiError("invalid_input", message));
    }

    @ExceptionHandler(IOException.class)
    public ResponseEntity<ApiError> handleExampleRead(IOException exception) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiError("example_unavailable", "Synthetic example could not be loaded"));
    }

    public record ApiError(String reason, String message) {
    }
}
