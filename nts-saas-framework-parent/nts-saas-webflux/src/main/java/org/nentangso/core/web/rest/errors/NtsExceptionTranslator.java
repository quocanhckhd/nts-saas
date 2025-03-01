package org.nentangso.core.web.rest.errors;

import org.nentangso.core.service.errors.FormValidationException;
import org.nentangso.core.service.errors.NotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.ConversionNotSupportedException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Controller advice to translate the server side exceptions to client-friendly json structures.
 * The error response follows RFC7807 - Problem Details for HTTP APIs (<a href="https://tools.ietf.org/html/rfc7807">RFC7807</a>).
 */
@ConditionalOnProperty(
    prefix = "nts.web.rest.exception-translator",
    name = "enabled",
    havingValue = "true",
    matchIfMissing = true
)
@ControllerAdvice
@ConditionalOnMissingBean(name = "exceptionTranslator")
public class NtsExceptionTranslator extends ResponseEntityExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(NtsExceptionTranslator.class);

    @Value("${nts.web.rest.exception-translator.realm-name:API Authentication by nentangso.org}")
    protected String realmName;

    @ExceptionHandler({
        AuthenticationException.class,
        AccessDeniedException.class,
        ResponseStatusException.class,
        ConcurrencyFailureException.class,
        NotFoundException.class,
        BadRequestAlertException.class,
        FormValidationException.class,
    })
    protected Mono<ResponseEntity<Object>> handleNtsException(Exception ex, ServerWebExchange exchange) {
        HttpHeaders headers = new HttpHeaders();

        if (ex instanceof AuthenticationException) {
            HttpStatus status = HttpStatus.UNAUTHORIZED;
            return handleAuthentication((AuthenticationException) ex, headers, status, exchange);
        } else if (ex instanceof AccessDeniedException) {
            HttpStatus status = HttpStatus.FORBIDDEN;
            return handleAccessDenied((AccessDeniedException) ex, headers, status, exchange);
        } else if (ex instanceof ResponseStatusException) {
            return handleResponseStatus((ResponseStatusException) ex, headers, null, exchange);
        } else if (ex instanceof ConcurrencyFailureException) {
            HttpStatus status = HttpStatus.CONFLICT;
            return handleConcurrencyFailure((ConcurrencyFailureException) ex, headers, status, exchange);
        } else if (ex instanceof NotFoundException) {
            HttpStatus status = HttpStatus.NOT_FOUND;
            return handleNotFound((NotFoundException) ex, headers, status, exchange);
        } else if (ex instanceof BadRequestAlertException) {
            HttpStatus status = HttpStatus.UNPROCESSABLE_ENTITY;
            return handleBadRequestAlert((BadRequestAlertException) ex, headers, status, exchange);
        } else if (ex instanceof FormValidationException) {
            HttpStatus status = HttpStatus.UNPROCESSABLE_ENTITY;
            return handleFormValidation((FormValidationException) ex, headers, status, exchange);
        } else {
            // Unknown exception, typically a wrapper with a common MVC exception as cause
            // (since @ExceptionHandler type declarations also match first-level causes):
            // We only deal with top-level MVC exceptions here, so let's rethrow the given
            // exception for further processing through the HandlerExceptionResolver chain.
            return Mono.error(ex);
        }
    }

    private Mono<ResponseEntity<Object>> handleAuthentication(AuthenticationException ex, HttpHeaders headers, HttpStatus status, ServerWebExchange exchange) {
        log.warn(ex.getMessage());
        headers.set(HttpHeaders.WWW_AUTHENTICATE, generateAuthenticateHeader(ex, headers, status, exchange));
        return handleExceptionInternal(ex, null, headers, status, exchange);
    }

    protected String generateAuthenticateHeader(Exception ex, HttpHeaders headers, HttpStatus status, ServerWebExchange exchange) {
        return String.format("Basic realm=\"%s\"", realmName);
    }

    private Mono<ResponseEntity<Object>> handleAccessDenied(AccessDeniedException ex, HttpHeaders headers, HttpStatus status, ServerWebExchange exchange) {
        log.warn(ex.getMessage());
        headers.set(HttpHeaders.WWW_AUTHENTICATE, generateAuthenticateHeader(ex, headers, status, exchange));
        return handleExceptionInternal(ex, null, headers, status, exchange);
    }

    private Mono<ResponseEntity<Object>> handleResponseStatus(ResponseStatusException ex, HttpHeaders headers, HttpStatus status, ServerWebExchange exchange) {

        return handleExceptionInternal(ex, null, headers, ex.getStatus(), exchange);
    }

    private Mono<ResponseEntity<Object>> handleConcurrencyFailure(ConcurrencyFailureException ex, HttpHeaders headers, HttpStatus status, ServerWebExchange exchange) {
        log.warn(ex.getMessage());

        return handleExceptionInternal(ex, null, headers, status, exchange);
    }

    private Mono<ResponseEntity<Object>> handleNotFound(NotFoundException ex, HttpHeaders headers, HttpStatus status, ServerWebExchange exchange) {

        return handleExceptionInternal(ex, null, headers, status, exchange);
    }

    private Mono<ResponseEntity<Object>> handleBadRequestAlert(BadRequestAlertException ex, HttpHeaders headers, HttpStatus status, ServerWebExchange exchange) {

        return handleExceptionInternal(ex, null, headers, status, exchange);
    }

    private Mono<ResponseEntity<Object>> handleFormValidation(FormValidationException ex, HttpHeaders headers, HttpStatus status, ServerWebExchange exchange) {

        return handleExceptionInternal(ex, null, headers, status, exchange);
    }

    @Override
    protected Mono<ResponseEntity<Object>> handleConversionNotSupported(ConversionNotSupportedException ex, HttpHeaders headers, HttpStatus status, ServerWebExchange exchange) {
        return super.handleConversionNotSupported(ex, headers, HttpStatus.BAD_REQUEST, exchange);
    }

    @Override
    protected Mono<ResponseEntity<Object>> handleMethodArgumentNotValid(MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatus status, ServerWebExchange exchange) {
        return super.handleMethodArgumentNotValid(ex, headers, HttpStatus.UNPROCESSABLE_ENTITY, exchange);
    }

    @Override
    protected Mono<ResponseEntity<Object>> handleExceptionInternal(Exception ex, @Nullable Object body, HttpHeaders headers, HttpStatus status, ServerWebExchange exchange) {
        if (HttpStatus.UNAUTHORIZED.equals(status) && body == null) {
            body = Collections.singletonMap(NtsErrorConstants.KEY_ERRORS, NtsErrorConstants.MESSAGE_UNAUTHORIZED);
        } else if (HttpStatus.FORBIDDEN.equals(status) && body == null) {
            body = Collections.singletonMap(NtsErrorConstants.KEY_ERRORS, NtsErrorConstants.MESSAGE_ACCESS_DENIED);
        } else if (HttpStatus.UNPROCESSABLE_ENTITY.equals(status) && body == null) {
            Map<String, List<String>> errors = buildUnprocessableErrors(ex);
            body = Collections.singletonMap(NtsErrorConstants.KEY_ERRORS, errors);
        } else if (status.is4xxClientError() && body == null) {
            body = Collections.singletonMap(NtsErrorConstants.KEY_ERRORS, status.getReasonPhrase());
        } else if (status.is5xxServerError() && body == null) {
            body = Collections.singletonMap(NtsErrorConstants.KEY_ERRORS, ex.getMessage());
        }
        return super.handleExceptionInternal(ex, body, headers, status, exchange);
    }

    protected Map<String, List<String>> buildUnprocessableErrors(Exception ex) {
        Map<String, List<String>> errors = Collections.singletonMap(NtsErrorConstants.KEY_BASE, Collections.singletonList(NtsErrorConstants.MESSAGE_UNPROCESSABLE));
        if (ex instanceof FormValidationException && !((FormValidationException) ex).getErrors().isEmpty()) {
            errors = ((FormValidationException) ex).getErrors();
        } else if (ex instanceof BadRequestAlertException) {
            errors = Collections.singletonMap(((BadRequestAlertException) ex).getErrorKey(), Collections.singletonList(ex.getMessage()));
        }
        return errors;
    }

    @ExceptionHandler(Exception.class)
    protected Mono<ResponseEntity<Object>> handleInternalServerError(Exception ex, ServerWebExchange exchange) {
        log.error("Internal Server Error", ex);
        HttpHeaders headers = new HttpHeaders();
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        return handleExceptionInternal(ex, null, headers, status, exchange);
    }
}
