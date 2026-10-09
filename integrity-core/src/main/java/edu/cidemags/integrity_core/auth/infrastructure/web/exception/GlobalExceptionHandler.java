package edu.cidemags.integrity_core.auth.infrastructure.web.exception;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import edu.cidemags.integrity_core.auth.domain.exception.CountryNotFoundException;
import edu.cidemags.integrity_core.auth.domain.exception.DomainException;
import edu.cidemags.integrity_core.auth.domain.exception.DuplicatePhoneNumberException;
import edu.cidemags.integrity_core.auth.domain.exception.EmailAlreadyRegisteredException;
import edu.cidemags.integrity_core.auth.domain.exception.InvalidAccessTokenException;
import edu.cidemags.integrity_core.auth.domain.exception.InvalidCredentialsException;
import edu.cidemags.integrity_core.auth.domain.exception.InvalidRefreshTokenException;
import edu.cidemags.integrity_core.auth.domain.exception.InvalidRoleAssignmentException;
import edu.cidemags.integrity_core.auth.domain.exception.UserNotFoundException;
import edu.cidemags.integrity_core.auth.infrastructure.web.dto.ErrorResponse;
import edu.cidemags.integrity_core.evaluator.domain.exception.ExamAccessCodeNotFoundException;
import edu.cidemags.integrity_core.evaluator.domain.exception.ExamClosedException;
import edu.cidemags.integrity_core.evaluator.domain.exception.ExamNotFoundException;
import edu.cidemags.integrity_core.evaluator.domain.exception.QuestionBankNotFoundException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final org.slf4j.Logger log
            = org.slf4j.LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(EmailAlreadyRegisteredException.class)
    public ResponseEntity<ErrorResponse> handleEmailAlreadyRegistered(EmailAlreadyRegisteredException ex) {
        return build(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(DuplicatePhoneNumberException.class)
    public ResponseEntity<ErrorResponse> handleDuplicatePhoneNumber(DuplicatePhoneNumberException ex) {
        return build(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler({InvalidCredentialsException.class, InvalidRefreshTokenException.class,
        InvalidAccessTokenException.class})
    public ResponseEntity<ErrorResponse> handleUnauthorized(DomainException ex) {
        return build(HttpStatus.UNAUTHORIZED, ex.getMessage());
    }

    @ExceptionHandler({CountryNotFoundException.class, UserNotFoundException.class})
    public ResponseEntity<ErrorResponse> handleNotFound(DomainException ex) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler({ExamNotFoundException.class, QuestionBankNotFoundException.class})
    public ResponseEntity<ErrorResponse> handleEvaluatorNotFound(
            edu.cidemags.integrity_core.evaluator.domain.exception.DomainException ex) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(ExamAccessCodeNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleInvalidExamAccessCode(ExamAccessCodeNotFoundException ex) {
        return build(HttpStatus.UNAUTHORIZED, ex.getMessage());
    }

    @ExceptionHandler(ExamClosedException.class)
    public ResponseEntity<ErrorResponse> handleClosedExam(ExamClosedException ex) {
        return build(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(InvalidRoleAssignmentException.class)
    public ResponseEntity<ErrorResponse> handleInvalidRoleAssignment(InvalidRoleAssignmentException ex) {
        return build(HttpStatus.FORBIDDEN, ex.getMessage());
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex) {
        return build(HttpStatus.FORBIDDEN, "No tienes permisos para realizar esta accion");
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        // LinkedHashMap: conserva el orden de los campos en la respuesta.
        Map<String, String> fieldErrors = new java.util.LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(fieldError -> fieldErrors.putIfAbsent(fieldError.getField(), fieldError.getDefaultMessage()));

        ErrorResponse body = ErrorResponse.ofValidation(
                HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(), fieldErrors);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    // Violaciones de restricciones unicas (p. ej. duplicados) -> 409 en vez de 500.
    @ExceptionHandler(org.springframework.dao.DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrity(
            org.springframework.dao.DataIntegrityViolationException ex) {
        log.warn("Violacion de integridad de datos: {}", ex.getMessage());
        return build(HttpStatus.CONFLICT, "La operacion entra en conflicto con datos existentes");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex) {
        log.error("Error no controlado", ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR,
                "Ocurrio un error inesperado. Intenta de nuevo mas tarde.");
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleIllegalState(IllegalStateException ex) {
        return build(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(org.springframework.http.converter.HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleUnreadable(
            org.springframework.http.converter.HttpMessageNotReadableException ex) {
        Throwable cause = ex.getCause();
        log.warn("Cuerpo ilegible: {}", cause != null ? cause.getMessage() : ex.getMessage());
        return build(HttpStatus.BAD_REQUEST, "El cuerpo de la petición no es válido");
    }

    private ResponseEntity<ErrorResponse> build(HttpStatus status, String message) {
        ErrorResponse body = ErrorResponse.of(status.value(), status.getReasonPhrase(), message);
        return ResponseEntity.status(status).body(body);
    }
}
