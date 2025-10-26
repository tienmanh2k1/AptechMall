package com.aptech.aptechMall.Exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // ===== External API Exceptions =====

    @ExceptionHandler(WebClientResponseException.class)
    public ResponseEntity<Map<String, Object>> handleWebClientException(
            WebClientResponseException ex) {

        log.error("API call failed: {}", ex.getMessage());

        Map<String, Object> error = new HashMap<>();
        error.put("status", ex.getStatusCode().value());
        error.put("error", ex.getStatusText());
        error.put("message", ex.getResponseBodyAsString());

        return ResponseEntity
                .status(ex.getStatusCode())
                .body(error);
    }

    // ===== User Exceptions =====

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleUserNotFoundException(
            UserNotFoundException ex) {

        log.error("User not found: {}", ex.getMessage());

        Map<String, Object> error = new HashMap<>();
        error.put("status", 404);
        error.put("error", "User Not Found");
        error.put("message", ex.getMessage());

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(error);
    }

    // ===== Cart Exceptions =====

    @ExceptionHandler(CartNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleCartNotFoundException(
            CartNotFoundException ex) {

        log.error("Cart not found: {}", ex.getMessage());

        Map<String, Object> error = new HashMap<>();
        error.put("status", 404);
        error.put("error", "Cart Not Found");
        error.put("message", ex.getMessage());

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(error);
    }

    @ExceptionHandler(CartItemNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleCartItemNotFoundException(
            CartItemNotFoundException ex) {

        log.error("Cart item not found: {}", ex.getMessage());

        Map<String, Object> error = new HashMap<>();
        error.put("status", 404);
        error.put("error", "Cart Item Not Found");
        error.put("message", ex.getMessage());

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(error);
    }

    @ExceptionHandler(EmptyCartException.class)
    public ResponseEntity<Map<String, Object>> handleEmptyCartException(
            EmptyCartException ex) {

        log.error("Empty cart: {}", ex.getMessage());

        Map<String, Object> error = new HashMap<>();
        error.put("status", 400);
        error.put("error", "Empty Cart");
        error.put("message", ex.getMessage());

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(error);
    }

    // ===== Order Exceptions =====

    @ExceptionHandler(OrderNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleOrderNotFoundException(
            OrderNotFoundException ex) {

        log.error("Order not found: {}", ex.getMessage());

        Map<String, Object> error = new HashMap<>();
        error.put("status", 404);
        error.put("error", "Order Not Found");
        error.put("message", ex.getMessage());

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(error);
    }

    @ExceptionHandler(OrderNotCancellableException.class)
    public ResponseEntity<Map<String, Object>> handleOrderNotCancellableException(
            OrderNotCancellableException ex) {

        log.error("Order not cancellable: {}", ex.getMessage());

        Map<String, Object> error = new HashMap<>();
        error.put("status", 400);
        error.put("error", "Order Not Cancellable");
        error.put("message", ex.getMessage());

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(error);
    }

    // ===== Validation Exceptions =====

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationException(
            MethodArgumentNotValidException ex) {

        log.error("Validation failed: {}", ex.getMessage());

        Map<String, String> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        error -> error.getDefaultMessage() != null ? error.getDefaultMessage() : "Invalid value",
                        (existing, replacement) -> existing
                ));

        Map<String, Object> error = new HashMap<>();
        error.put("status", 400);
        error.put("error", "Validation Failed");
        error.put("message", "Invalid input data");
        error.put("fieldErrors", fieldErrors);

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(error);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgumentException(
            IllegalArgumentException ex) {

        log.error("Illegal argument: {}", ex.getMessage());

        Map<String, Object> error = new HashMap<>();
        error.put("status", 400);
        error.put("error", "Bad Request");
        error.put("message", ex.getMessage());

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(error);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalStateException(
            IllegalStateException ex) {

        log.error("Illegal state: {}", ex.getMessage());

        Map<String, Object> error = new HashMap<>();
        error.put("status", 400);
        error.put("error", "Bad Request");
        error.put("message", ex.getMessage());

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(error);
    }

    // ===== Generic Exception =====

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(
            Exception ex) {

        log.error("Unexpected error: {}", ex.getMessage(), ex);

        Map<String, Object> error = new HashMap<>();
        error.put("status", 500);
        error.put("error", "Internal Server Error");
        error.put("message", ex.getMessage());

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(error);
    }
}