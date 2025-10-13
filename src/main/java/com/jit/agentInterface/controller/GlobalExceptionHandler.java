package com.jit.agentInterface.controller;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.jit.agentInterface.service.ServiceException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.NoSuchElementException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static record ErrorResponse(
            @JsonFormat(shape = JsonFormat.Shape.STRING) Instant timestamp,
            String path,
            int status,
            String error,
            String message
    ) {}

    private ResponseEntity<ErrorResponse> build(HttpStatus status, String message, HttpServletRequest req) {
        return ResponseEntity.status(status).body(new ErrorResponse(Instant.now(), req.getRequestURI(), status.value(), status.getReasonPhrase(), message));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest req) {
        String msg = ex.getBindingResult().getFieldErrors().stream().findFirst()
                .map(fe -> fe.getField()+": "+fe.getDefaultMessage()).orElse("Validation error");
        return build(HttpStatus.BAD_REQUEST, msg, req);
    }

    @ExceptionHandler(ServiceException.class)
    public ResponseEntity<ErrorResponse> handleService(ServiceException ex, HttpServletRequest req) {
        return build(ex.getStatus(), ex.getMessage(), req);
    }

    @ExceptionHandler({IllegalArgumentException.class})
    public ResponseEntity<ErrorResponse> handleBadRequest(RuntimeException ex, HttpServletRequest req) { return build(HttpStatus.BAD_REQUEST, ex.getMessage(), req); }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex, HttpServletRequest req) { return build(HttpStatus.FORBIDDEN, ex.getMessage(), req); }

    @ExceptionHandler({BadCredentialsException.class, UsernameNotFoundException.class, JwtException.class})
    public ResponseEntity<ErrorResponse> handleAuth(Exception ex, HttpServletRequest req) { return build(HttpStatus.UNAUTHORIZED, ex.getMessage(), req); }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(NoSuchElementException ex, HttpServletRequest req) { return build(HttpStatus.NOT_FOUND, ex.getMessage(), req); }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex, HttpServletRequest req) { return build(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage(), req); }
}
