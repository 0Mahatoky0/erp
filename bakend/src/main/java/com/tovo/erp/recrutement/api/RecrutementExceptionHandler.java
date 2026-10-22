package com.tovo.erp.recrutement.api;

import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestControllerAdvice
public class RecrutementExceptionHandler {
    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<Map<String, String>> badRequest(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
    }

    @ExceptionHandler(IllegalStateException.class)
    ResponseEntity<Map<String, String>> conflict(IllegalStateException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", e.getMessage()));
    }
}
