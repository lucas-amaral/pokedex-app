package com.example.tasks.controller;

import com.example.tasks.entity.InvalidFieldException;
import com.example.tasks.entity.InvalidParameterException;
import com.example.tasks.entity.InvalidStatusChangeException;
import com.example.tasks.repository.TaskVersionConflictException;
import com.example.tasks.service.InvalidCredentialsException;
import com.example.tasks.service.TaskNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.TypeMismatchException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.exc.MismatchedInputException;
import tools.jackson.databind.exc.UnrecognizedPropertyException;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Every error leaves the API as RFC 9457 problem details. Invalid input lists each problem in {@code errors}, by
 * {@code field} (body) or {@code parameter} (path or query). Template Method: Spring's own MVC exceptions are
 * handled by the parent class, with the hooks overridden below.
 */
@RestControllerAdvice
class ApiExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);
    private static final String INVALID_REQUEST = "Invalid request";
    private static final Set<Class<?>> WHOLE_NUMBERS = Set.of(int.class, Integer.class, long.class, Long.class);

    @ExceptionHandler(InvalidFieldException.class)
    ProblemDetail handleInvalidField(InvalidFieldException exception) {
        return invalid("One or more fields are invalid.", List.of(error("field", exception.field(), exception.getMessage())));
    }

    @ExceptionHandler(InvalidParameterException.class)
    ProblemDetail handleInvalidParameter(InvalidParameterException exception) {
        return invalid("One or more request parameters are invalid.",
                List.of(error("parameter", exception.parameter(), exception.getMessage())));
    }

    @ExceptionHandler(TaskNotFoundException.class)
    ProblemDetail handleTaskNotFound(TaskNotFoundException exception) {
        var problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage());
        problem.setTitle("Task not found");
        return problem;
    }

    /** 422, not 409: the request is well formed and up to date, but the rule forbids it (decided before implementation). */
    @ExceptionHandler(InvalidStatusChangeException.class)
    ProblemDetail handleInvalidStatusChange(InvalidStatusChangeException exception) {
        var problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_CONTENT, exception.getMessage());
        problem.setTitle("Status change not allowed");
        problem.setProperty("allowed", exception.allowed());
        return problem;
    }

    @ExceptionHandler(TaskVersionConflictException.class)
    ProblemDetail handleVersionConflict(TaskVersionConflictException exception) {
        var problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT,
                "Someone changed this task after you loaded it. Reload it and apply your change again.");
        problem.setTitle("Task changed since you loaded it");
        problem.setProperty("expectedVersion", exception.expectedVersion());
        exception.currentVersion().ifPresent(current -> problem.setProperty("currentVersion", current));
        return problem;
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    ProblemDetail handleInvalidCredentials(InvalidCredentialsException exception) {
        var problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, "Wrong username or password.");
        problem.setTitle("Login failed");
        return problem;
    }

    /** Anything unforeseen: logged in full, answered without internals. */
    @ExceptionHandler(Exception.class)
    ProblemDetail handleUnexpected(Exception exception) {
        log.error("Unexpected error", exception);
        var problem = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, "Something went wrong on our side.");
        problem.setTitle("Unexpected error");
        return problem;
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException exception, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        var errors = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> error("field", error.getField(), String.valueOf(error.getDefaultMessage())))
                .toList();
        return handleExceptionInternal(exception, invalid("One or more fields are invalid.", errors), headers, status, request);
    }

    @Override
    protected ResponseEntity<Object> handleHandlerMethodValidationException(
            HandlerMethodValidationException exception, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        var errors = exception.getParameterValidationResults().stream()
                .flatMap(result -> result.getResolvableErrors().stream()
                        .map(error -> error("parameter", result.getMethodParameter().getParameterName(),
                                String.valueOf(error.getDefaultMessage()))))
                .toList();
        return handleExceptionInternal(exception, invalid("One or more request parameters are invalid.", errors),
                headers, status, request);
    }

    /** A path or query value that does not convert, such as an id that is not a UUID. */
    @Override
    protected ResponseEntity<Object> handleTypeMismatch(
            TypeMismatchException exception, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        if (!(exception instanceof MethodArgumentTypeMismatchException mismatch)) {
            return super.handleTypeMismatch(exception, headers, status, request);
        }
        var errors = List.of(error("parameter", mismatch.getName(), expectedKind(mismatch.getRequiredType())));
        return handleExceptionInternal(exception, invalid("One or more request parameters are invalid.", errors),
                headers, status, request);
    }

    /** A body Jackson could not read: names the field when it can, instead of a generic "Failed to read request". */
    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(
            HttpMessageNotReadableException exception, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        var unknown = findCause(exception, UnrecognizedPropertyException.class);
        var mismatch = findCause(exception, MismatchedInputException.class);
        ProblemDetail problem;
        if (unknown != null) {
            problem = invalid("One or more fields are invalid.", List.of(error("field", fieldPath(unknown), "is not a known field")));
        } else if (mismatch != null && !mismatch.getPath().isEmpty()) {
            problem = invalid("One or more fields are invalid.",
                    List.of(error("field", fieldPath(mismatch), expectedKind(mismatch.getTargetType()))));
        } else {
            problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "The request body is not valid JSON.");
            problem.setTitle(INVALID_REQUEST);
        }
        return handleExceptionInternal(exception, problem, headers, status, request);
    }

    private static ProblemDetail invalid(String detail, List<Map<String, String>> errors) {
        var problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detail);
        problem.setTitle(INVALID_REQUEST);
        problem.setProperty("errors", errors);
        return problem;
    }

    private static Map<String, String> error(String kind, String name, String message) {
        return Map.of(kind, name, "message", message);
    }

    private static String fieldPath(JacksonException exception) {
        return exception.getPath().stream()
                .map(JacksonException.Reference::getPropertyName)
                .filter(Objects::nonNull)
                .collect(Collectors.joining("."));
    }

    private static String expectedKind(Class<?> type) {
        if (type == null) {
            return "has the wrong type";
        }
        if (WHOLE_NUMBERS.contains(type)) {
            return "must be a whole number";
        }
        if (type == UUID.class) {
            return "must be a UUID";
        }
        if (type == LocalDate.class) {
            return "must be a date like 2030-01-31";
        }
        if (type.isEnum()) {
            return "must be one of " + List.of(type.getEnumConstants());
        }
        if (Collection.class.isAssignableFrom(type)) {
            return "must be a list";
        }
        if (CharSequence.class.isAssignableFrom(type)) {
            return "must be text";
        }
        return "has the wrong type";
    }

    private static <T extends Throwable> T findCause(Throwable throwable, Class<T> type) {
        for (var cause = throwable; cause != null; cause = cause.getCause()) {
            if (type.isInstance(cause)) {
                return type.cast(cause);
            }
        }
        return null;
    }
}
