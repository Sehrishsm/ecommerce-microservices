package com.ecommerce.order.exception;

import com.ecommerce.order.service.OrderNotFoundException;

import jakarta.validation.ConstraintViolationException;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(OrderNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleOrderNotFound(
            OrderNotFoundException exception) {

        return buildResponse(
                HttpStatus.NOT_FOUND,
                exception.getMessage()
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidationErrors(
            MethodArgumentNotValidException exception) {

        String message = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> error.getField() + ": "
                        + error.getDefaultMessage())
                .collect(Collectors.joining("; "));

        return buildResponse(HttpStatus.BAD_REQUEST, message);
    }

    @ExceptionHandler({
            IllegalArgumentException.class,
            ConstraintViolationException.class,
            HttpMessageNotReadableException.class
    })
    public ResponseEntity<Map<String, String>> handleBadRequest(
            Exception exception) {

        return buildResponse(
                HttpStatus.BAD_REQUEST,
                "Invalid request: " + exception.getMessage()
        );
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, String>> handleInvalidState(
            IllegalStateException exception) {

        return buildResponse(
                HttpStatus.CONFLICT,
                exception.getMessage()
        );
    }

    private ResponseEntity<Map<String, String>> buildResponse(
            HttpStatus status,
            String message) {

        Map<String, String> body = new LinkedHashMap<>();
        body.put("status", status.name());
        body.put("message", message);

        return ResponseEntity.status(status).body(body);
    }
}
