
package com.ecommerce.auth.exception;

import java.util.Map;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

        @ExceptionHandler(IllegalArgumentException.class)
        public ResponseEntity<Map<String, String>> handleBadRequest(
                        IllegalArgumentException exception) {
                HttpStatus status = exception.getMessage()
                                .startsWith("An account with this email")
                                                ? HttpStatus.CONFLICT
                                                : HttpStatus.BAD_REQUEST;

                return ResponseEntity.status(status)
                                .body(Map.of("error", exception.getMessage()));
        }

        @ExceptionHandler(InvalidCredentialsException.class)
        public ResponseEntity<Map<String, String>> handleInvalidCredentials(
                        InvalidCredentialsException exception) {

                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                                .body(Map.of("error", "Invalid email or password"));
        }

        @ExceptionHandler(MethodArgumentNotValidException.class)
        public ResponseEntity<Map<String, String>> handleValidation(
                        MethodArgumentNotValidException exception) {
                String message = exception.getBindingResult()
                                .getFieldErrors()
                                .stream()
                                .findFirst()
                                .map(error -> error.getDefaultMessage())
                                .orElse("Invalid request");

                return ResponseEntity.badRequest()
                                .body(Map.of("error", message));
        }

        @ExceptionHandler(DataIntegrityViolationException.class)
        public ResponseEntity<Map<String, String>> handleDatabaseConflict(
                        DataIntegrityViolationException exception) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                                .body(Map.of("error", "An account with this email already exists"));
        }
}
