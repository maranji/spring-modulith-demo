package org.maranji.app.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import org.maranji.analytics.actions.InsufficientDataException;
import org.maranji.marketdata.actions.AssetNotFoundException;

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
