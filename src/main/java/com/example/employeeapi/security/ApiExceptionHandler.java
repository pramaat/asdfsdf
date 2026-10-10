package com.example.employeeapi.security;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class ApiExceptionHandler {
    private static final String VALIDATION_DETAIL = "The request contains invalid fields";
    private static final String MALFORMED_REQUEST_DETAIL = "The request body is missing or malformed";

    @ExceptionHandler(DuplicateKeyException.class)
    public ProblemDetail handleDuplicateAccount() {
        return problem(HttpStatus.CONFLICT, "An account with this email already exists");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleInvalidRequest(MethodArgumentNotValidException exception) {
        ProblemDetail problem = problem(HttpStatus.BAD_REQUEST, VALIDATION_DETAIL);
        Map<String, String> errors = new LinkedHashMap<>();
        exception.getBindingResult().getFieldErrors().forEach(error ->
                errors.putIfAbsent(error.getField(), error.getDefaultMessage()));
        problem.setProperty("errors", errors);
        return problem;
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ProblemDetail handleUnreadableRequest() {
        return problem(HttpStatus.BAD_REQUEST, MALFORMED_REQUEST_DETAIL);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ProblemDetail handleRequestFailure(ResponseStatusException exception) {
        HttpStatus status = HttpStatus.valueOf(exception.getStatusCode().value());
        String detail = exception.getReason() == null ? status.getReasonPhrase() : exception.getReason();
        return problem(status, detail);
    }

    private ProblemDetail problem(HttpStatus status, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(status.getReasonPhrase());
        return problem;
    }
}
