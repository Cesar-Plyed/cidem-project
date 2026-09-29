package edu.cidemags.integrity_core.safeguard.web;

import edu.cidemags.integrity_core.safeguard.service.SafeguardException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.core.Ordered;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.LinkedHashMap;
import java.util.Map;

@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(assignableTypes = SafeguardController.class)
public class SafeguardExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(SafeguardExceptionHandler.class);

    @ExceptionHandler(SafeguardException.class)
    public ProblemDetail handleSafeguard(SafeguardException ex) {
        HttpStatus status = switch (ex.getReason()) {
            case NOT_FOUND -> HttpStatus.NOT_FOUND;
            case FORBIDDEN -> HttpStatus.FORBIDDEN;
            case INVALID_STATE -> HttpStatus.CONFLICT;
        };
        return problem(status, ex.getReason().name(), ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(fe -> errors.putIfAbsent(fe.getField(), fe.getDefaultMessage()));
        ProblemDetail pd = problem(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Invalid request");
        pd.setProperty("errors", errors);
        return pd;
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, IllegalArgumentException.class,
        MethodArgumentTypeMismatchException.class})
    public ProblemDetail handleBadRequest(Exception ex) {
        return problem(HttpStatus.BAD_REQUEST, "BAD_REQUEST", "Malformed or invalid request body");
    }

    /** Carrera en constraints únicos (p. ej. doble start concurrente sobre el mismo attempt). */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail handleIntegrity(DataIntegrityViolationException ex) {
        log.warn("Integrity violation in SafeGuard: {}", ex.getMostSpecificCause().getMessage());
        return problem(HttpStatus.CONFLICT, "CONFLICT", "Concurrent modification, retry the request");
    }

    private static ProblemDetail problem(HttpStatus status, String code, String detail) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(status, detail);
        pd.setProperty("code", code);
        return pd;
    }
}