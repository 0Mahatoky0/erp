package com.tovo.erp.common;

import org.springframework.http.HttpStatus;

/**
 * Erreur metier explicite (RM-068) : le message explique systematiquement
 * pourquoi l'operation est refusee.
 */
public class BusinessException extends RuntimeException {

    private final String code;
    private final HttpStatus status;

    public BusinessException(String code, String message) {
        this(code, message, HttpStatus.BAD_REQUEST);
    }

    public BusinessException(String code, String message, HttpStatus status) {
        super(message);
        this.code = code;
        this.status = status;
    }

    public String getCode() {
        return code;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public static BusinessException notFound(String message) {
        return new BusinessException("RESOURCE_INTROUVABLE", message, HttpStatus.NOT_FOUND);
    }

    public static BusinessException forbidden(String message) {
        return new BusinessException("ACCES_REFUSE", message, HttpStatus.FORBIDDEN);
    }

    /** Identifiants de connexion invalides (RM-001). */
    public static BusinessException identifiantsInvalides() {
        return new BusinessException("IDENTIFIANTS_INVALIDES",
                "Adresse e-mail ou mot de passe incorrect.", HttpStatus.UNAUTHORIZED);
    }
}
