package com.healthcamp.common;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Translates request errors into clear, consistent JSON error responses.
 *
 * <p>Validation failures (Bean Validation) return {@code 400} with a per-field
 * {@code errors} list. Malformed or missing request bodies also return {@code 400}.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
        List<Map<String, String>> errors = new ArrayList<>();
        for (FieldError fe : ex.getBindingResult().getFieldErrors()) {
            errors.add(Map.of("field", fe.getField(), "message", fe.getDefaultMessage()));
        }
        return ResponseEntity.badRequest().body(errorBody("Validation failed", errors));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleMalformedBody(HttpMessageNotReadableException ex) {
        return ResponseEntity.badRequest().body(errorBody("Malformed request body", null));
    }

    private Map<String, Object> errorBody(String message, List<Map<String, String>> errors) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", Instant.now().toString());
        body.put("status", HttpStatus.BAD_REQUEST.value());
        body.put("error", "Bad Request");
        body.put("message", message);
        if (errors != null) {
            body.put("errors", errors);
        }
        return body;
    }
}
