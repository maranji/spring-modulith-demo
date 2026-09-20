package com.example.app.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.example.analytics.InsufficientDataException;
import com.example.marketdata.AssetNotFoundException;

/**
 * Traduce le eccezioni di dominio (definite nei moduli Market Data e
 * Analytics) in risposte HTTP in formato RFC 7807 (Problem Details).
 */
@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(AssetNotFoundException.class)
    ProblemDetail handleAssetNotFound(AssetNotFoundException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(InsufficientDataException.class)
    ProblemDetail handleInsufficientData(InsufficientDataException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ProblemDetail handleBadRequest(IllegalArgumentException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
    }
}
