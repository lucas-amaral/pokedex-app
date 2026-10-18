package com.lucasamaral.pokedex.web.error;

import com.lucasamaral.pokedex.application.catalog.CatalogUnavailableException;
import com.lucasamaral.pokedex.application.catalog.PokemonNotFoundException;
import com.lucasamaral.pokedex.application.collection.CollectedPokemonNotFoundException;
import com.lucasamaral.pokedex.application.collection.CollectedPokemonVersionConflictException;
import com.lucasamaral.pokedex.domain.InvalidFieldException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.exc.MismatchedInputException;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Turns exceptions into RFC 9457 problem details ({@code application/problem+json}).
 * Spring's own MVC exceptions (bad types, missing parameters, ...) are handled by the parent class.
 */
@RestControllerAdvice
class ApiExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(CatalogUnavailableException.class)
    ProblemDetail handleCatalogUnavailable(CatalogUnavailableException exception) {
        log.warn("Catalog unavailable", exception);
        var problem = ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE,
                "The Pokémon catalog is not responding. Try again in a moment.");
        problem.setTitle("Catalog unavailable");
        return problem;
    }

    @ExceptionHandler(PokemonNotFoundException.class)
    ProblemDetail handlePokemonNotFound(PokemonNotFoundException exception) {
        var problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage());
        problem.setTitle("Pokémon not found");
        return problem;
    }

    @ExceptionHandler(CollectedPokemonNotFoundException.class)
    ProblemDetail handleCollectedPokemonNotFound(CollectedPokemonNotFoundException exception) {
        var problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage());
        problem.setTitle("Pokémon not in the collection");
        return problem;
    }

    @ExceptionHandler(CollectedPokemonVersionConflictException.class)
    ProblemDetail handleVersionConflict(CollectedPokemonVersionConflictException exception) {
        var problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT,
                "Someone else changed this Pokémon after you loaded it. Reload it and apply your changes again.");
        problem.setTitle("Pokémon changed since you loaded it");
        problem.setProperty("expectedVersion", exception.expectedVersion());
        problem.setProperty("currentVersion", exception.currentVersion());
        return problem;
    }

    @ExceptionHandler(InvalidFieldException.class)
    ProblemDetail handleInvalidField(InvalidFieldException exception) {
        return invalidFields(List.of(fieldError(exception.field(), exception.getMessage().substring(exception.field().length() + 1))));
    }

    @ExceptionHandler(UnknownFieldsException.class)
    ProblemDetail handleUnknownFields(UnknownFieldsException exception) {
        return invalidFields(exception.fields().stream().sorted()
                .map(field -> fieldError(field, "is not an editable field"))
                .toList());
    }

    @ExceptionHandler(Exception.class)
    ProblemDetail handleUnexpected(Exception exception) {
        log.error("Unexpected error", exception);
        var problem = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR,
                "Something went wrong on our side. Try again later.");
        problem.setTitle("Unexpected error");
        return problem;
    }

    @Override
    protected ResponseEntity<Object> handleHandlerMethodValidationException(
            HandlerMethodValidationException exception, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        var errors = exception.getParameterValidationResults().stream()
                .flatMap(result -> result.getResolvableErrors().stream()
                        .map(error -> Map.of(
                                "parameter", result.getMethodParameter().getParameterName(),
                                "message", String.valueOf(error.getDefaultMessage()))))
                .toList();
        var problem = ProblemDetail.forStatusAndDetail(status, "One or more request parameters are invalid.");
        problem.setTitle("Invalid request");
        problem.setProperty("errors", List.copyOf(errors));
        return handleExceptionInternal(exception, problem, headers, status, request);
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException exception, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        var errors = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> Map.of(
                        "field", error.getField(),
                        "message", String.valueOf(error.getDefaultMessage())))
                .toList();
        var problem = ProblemDetail.forStatusAndDetail(status, "One or more fields are invalid.");
        problem.setTitle("Invalid request");
        problem.setProperty("errors", List.copyOf(errors));
        return handleExceptionInternal(exception, problem, headers, status, request);
    }

    private static ProblemDetail invalidFields(List<Map<String, String>> errors) {
        var problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "One or more fields are invalid.");
        problem.setTitle("Invalid request");
        problem.setProperty("errors", errors);
        return problem;
    }

    private static Map<String, String> fieldError(String field, String message) {
        return Map.of("field", field, "message", message);
    }

    /**
     * A body Jackson could not read: either not JSON at all, or a field with the wrong type, which is then named
     * ("version must be a number") instead of a generic "Failed to read request".
     */
    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(
            HttpMessageNotReadableException exception, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        var mismatch = findCause(exception, MismatchedInputException.class);
        if (mismatch != null && !mismatch.getPath().isEmpty()) {
            var problem = invalidFields(List.of(fieldError(fieldPath(mismatch), expectedKind(mismatch.getTargetType()))));
            return handleExceptionInternal(exception, problem, headers, status, request);
        }
        var problem = ProblemDetail.forStatusAndDetail(status, "The request body is not valid JSON.");
        problem.setTitle("Invalid request");
        return handleExceptionInternal(exception, problem, headers, status, request);
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
        if (Number.class.isAssignableFrom(type) || (type.isPrimitive() && type != boolean.class)) {
            return "must be a number";
        }
        if (Collection.class.isAssignableFrom(type) || type.isArray()) {
            return "must be a list";
        }
        if (Map.class.isAssignableFrom(type)) {
            return "must be an object";
        }
        if (CharSequence.class.isAssignableFrom(type)) {
            return "must be text";
        }
        if (type == Boolean.class || type == boolean.class) {
            return "must be true or false";
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
