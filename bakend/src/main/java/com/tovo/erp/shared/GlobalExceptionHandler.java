package com.tovo.erp.shared;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

/**
 * Traduit les exceptions en reponses HTTP explicites (RM-068).
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiError> handleBusiness(BusinessException ex, HttpServletRequest request) {
        return build(ex.getCode(), ex.getMessage(), ex.getStatus(), request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex,
                                                      HttpServletRequest request) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(erreur -> erreur.getField() + " : " + erreur.getDefaultMessage())
                .collect(Collectors.joining(" ; "));
        return build("DONNEES_INVALIDES", message, HttpStatus.BAD_REQUEST, request);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        return build("ACCES_REFUSE", "Vous n'avez pas les droits necessaires pour cette operation.",
                HttpStatus.FORBIDDEN, request);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiError> handleAuthentication(AuthenticationException ex,
                                                         HttpServletRequest request) {
        return build("NON_AUTHENTIFIE", ex.getMessage(), HttpStatus.UNAUTHORIZED, request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnexpected(Exception ex, HttpServletRequest request) {
        log.error("Erreur inattendue sur {} {}", request.getMethod(), request.getRequestURI(), ex);
        return build("ERREUR_INTERNE", "Une erreur interne est survenue. Merci de reessayer plus tard.",
                HttpStatus.INTERNAL_SERVER_ERROR, request);
    }

    private ResponseEntity<ApiError> build(String code, String message, HttpStatus status,
                                           HttpServletRequest request) {
        ApiError error = new ApiError(code, message, status.value(), request.getRequestURI());
        return ResponseEntity.status(status).body(error);
    }
}
