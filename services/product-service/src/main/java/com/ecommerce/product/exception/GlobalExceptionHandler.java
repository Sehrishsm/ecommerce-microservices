
package com.ecommerce.product.exception;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

        @ExceptionHandler(IllegalArgumentException.class)
        public ResponseEntity<Map<String, String>> handleIllegalArgument(
                        IllegalArgumentException exception) {

                String message = exception.getMessage();

                if (message != null
                                && (message.startsWith("Inventory not found")
                                                || message.startsWith("Product not found")
                                                || message.startsWith("Category not found")
                                                || message.startsWith("Reservation not found"))) {

                        return ResponseEntity
                                        .status(HttpStatus.NOT_FOUND)
                                        .body(Map.of("error", message));
                }

                return ResponseEntity
                                .badRequest()
                                .body(Map.of("error",
                                                message == null ? "Invalid request" : message));
        }

        @ExceptionHandler(MethodArgumentNotValidException.class)
        public ResponseEntity<Map<String, String>> handleValidationError(
                        MethodArgumentNotValidException exception) {

                String message = exception.getBindingResult()
                                .getFieldErrors()
                                .stream()
                                .findFirst()
                                .map(error -> error.getDefaultMessage())
                                .orElse("Invalid request");

                return ResponseEntity
                                .badRequest()
                                .body(Map.of("error", message));
        }
}
