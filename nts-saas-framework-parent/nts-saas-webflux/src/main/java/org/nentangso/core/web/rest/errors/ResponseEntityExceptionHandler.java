/*
 * Copyright 2002-2021 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.nentangso.core.web.rest.errors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.ConversionNotSupportedException;
import org.springframework.beans.TypeMismatchException;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.lang.Nullable;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * A class with an {@code @ExceptionHandler} method that handles all Spring
 * WebFlux raised exceptions by returning a {@link ResponseEntity} with
 * RFC 7807 formatted error details in the body.
 *
 * <p>Convenient as a base class of an {@link ControllerAdvice @ControllerAdvice}
 * for global exception handling in an application. Subclasses can override
 * individual methods that handle a specific exception, override
 * {@link #handleExceptionInternal} to override common handling of all exceptions,
 * or {@link #createResponseEntity} to intercept the final step of creating the
 */
public abstract class ResponseEntityExceptionHandler {

    /**
     * Log category to use when no mapped handler is found for a request.
     *
     * @see #pageNotFoundLogger
     */
    public static final String PAGE_NOT_FOUND_LOG_CATEGORY = "org.springframework.web.servlet.PageNotFound";

    /**
     * Specific logger to use when no mapped handler is found for a request.
     *
     * @see #PAGE_NOT_FOUND_LOG_CATEGORY
     */
    protected static final Log pageNotFoundLogger = LogFactory.getLog(PAGE_NOT_FOUND_LOG_CATEGORY);

    /**
     * Common logger for use in subclasses.
     */
    protected final Log logger = LogFactory.getLog(getClass());


    /**
     * Provides handling for standard Spring MVC exceptions.
     *
     * @param ex       the target exception
     * @param exchange the current request and response
     * @return a {@code Mono} with the {@code ResponseEntity} for the response
     */
    @ExceptionHandler({
        ConversionNotSupportedException.class,
        TypeMismatchException.class,
        HttpMessageNotReadableException.class,
        HttpMessageNotWritableException.class,
        MethodArgumentNotValidException.class,
        BindException.class,
    })
    @Nullable
    public final Mono<ResponseEntity<Object>> handleException(Exception ex, ServerWebExchange exchange) {
        HttpHeaders headers = new HttpHeaders();

        if (ex instanceof ConversionNotSupportedException) {
            HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
            return handleConversionNotSupported((ConversionNotSupportedException) ex, headers, status, exchange);
        } else if (ex instanceof TypeMismatchException) {
            HttpStatus status = HttpStatus.BAD_REQUEST;
            return handleTypeMismatch((TypeMismatchException) ex, headers, status, exchange);
        } else if (ex instanceof HttpMessageNotReadableException) {
            HttpStatus status = HttpStatus.BAD_REQUEST;
            return handleHttpMessageNotReadable((HttpMessageNotReadableException) ex, headers, status, exchange);
        } else if (ex instanceof HttpMessageNotWritableException) {
            HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
            return handleHttpMessageNotWritable((HttpMessageNotWritableException) ex, headers, status, exchange);
        } else if (ex instanceof MethodArgumentNotValidException) {
            HttpStatus status = HttpStatus.BAD_REQUEST;
            return handleMethodArgumentNotValid((MethodArgumentNotValidException) ex, headers, status, exchange);
        } else if (ex instanceof BindException) {
            HttpStatus status = HttpStatus.BAD_REQUEST;
            return handleBindException((BindException) ex, headers, status, exchange);
        } else {
            // Unknown exception, typically a wrapper with a common MVC exception as cause
            // (since @ExceptionHandler type declarations also match first-level causes):
            // We only deal with top-level MVC exceptions here, so let's rethrow the given
            // exception for further processing through the HandlerExceptionResolver chain.
            return Mono.error(ex);
        }
    }

    /**
     * Customize the response for ConversionNotSupportedException.
     * <p>This method delegates to {@link #handleExceptionInternal}.
     *
     * @param ex       the exception
     * @param headers  the headers to be written to the response
     * @param status   the selected response status
     * @param exchange the current request and response
     * @return a {@code Mono} with the {@code ResponseEntity} for the response
     */
    protected Mono<ResponseEntity<Object>> handleConversionNotSupported(
        ConversionNotSupportedException ex, HttpHeaders headers, HttpStatus status, ServerWebExchange exchange) {

        return handleExceptionInternal(ex, null, headers, status, exchange);
    }

    /**
     * Customize the response for TypeMismatchException.
     * <p>This method delegates to {@link #handleExceptionInternal}.
     *
     * @param ex       the exception
     * @param headers  the headers to be written to the response
     * @param status   the selected response status
     * @param exchange the current request and response
     * @return a {@code Mono} with the {@code ResponseEntity} for the response
     */
    protected Mono<ResponseEntity<Object>> handleTypeMismatch(
        TypeMismatchException ex, HttpHeaders headers, HttpStatus status, ServerWebExchange exchange) {

        return handleExceptionInternal(ex, null, headers, status, exchange);
    }

    /**
     * Customize the response for HttpMessageNotReadableException.
     * <p>This method delegates to {@link #handleExceptionInternal}.
     *
     * @param ex       the exception
     * @param headers  the headers to be written to the response
     * @param status   the selected response status
     * @param exchange the current request and response
     * @return a {@code Mono} with the {@code ResponseEntity} for the response
     */
    protected Mono<ResponseEntity<Object>> handleHttpMessageNotReadable(
        HttpMessageNotReadableException ex, HttpHeaders headers, HttpStatus status, ServerWebExchange exchange) {

        return handleExceptionInternal(ex, null, headers, status, exchange);
    }

    /**
     * Customize the response for HttpMessageNotWritableException.
     * <p>This method delegates to {@link #handleExceptionInternal}.
     *
     * @param ex       the exception
     * @param headers  the headers to be written to the response
     * @param status   the selected response status
     * @param exchange the current request and response
     * @return a {@code Mono} with the {@code ResponseEntity} for the response
     */
    protected Mono<ResponseEntity<Object>> handleHttpMessageNotWritable(
        HttpMessageNotWritableException ex, HttpHeaders headers, HttpStatus status, ServerWebExchange exchange) {

        return handleExceptionInternal(ex, null, headers, status, exchange);
    }

    /**
     * Customize the response for MethodArgumentNotValidException.
     * <p>This method delegates to {@link #handleExceptionInternal}.
     *
     * @param ex       the exception
     * @param headers  the headers to be written to the response
     * @param status   the selected response status
     * @param exchange the current request and response
     * @return a {@code Mono} with the {@code ResponseEntity} for the response
     */
    protected Mono<ResponseEntity<Object>> handleMethodArgumentNotValid(
        MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatus status, ServerWebExchange exchange) {

        return handleExceptionInternal(ex, null, headers, status, exchange);
    }

    /**
     * Customize the response for BindException.
     * <p>This method delegates to {@link #handleExceptionInternal}.
     *
     * @param ex       the exception
     * @param headers  the headers to be written to the response
     * @param status   the selected response status
     * @param exchange the current request and response
     * @return a {@code Mono} with the {@code ResponseEntity} for the response
     */
    protected Mono<ResponseEntity<Object>> handleBindException(
        BindException ex, HttpHeaders headers, HttpStatus status, ServerWebExchange exchange) {

        return handleExceptionInternal(ex, null, headers, status, exchange);
    }

    /**
     * A single place to customize the response body of all exception types.
     *
     * @param ex       the exception
     * @param body     the body for the response
     * @param headers  the headers for the response
     * @param status   the response status
     * @param exchange the current request and response
     * @return a {@code Mono} with the {@code ResponseEntity} for the response
     */
    protected Mono<ResponseEntity<Object>> handleExceptionInternal(
        Exception ex, @Nullable Object body, HttpHeaders headers, HttpStatus status, ServerWebExchange exchange) {

        if (exchange.getResponse().isCommitted()) {
            return Mono.error(ex);
        }
        return createResponseEntity(body, headers, status, exchange);
    }


    /**
     * Create the {@link ResponseEntity} to use from the given body, headers,
     * and statusCode. Subclasses can override this method to inspect and possibly
     * modify the body, headers, or statusCode, e.g. to re-create an instance of
     *
     * @param body     the body to use for the response
     * @param headers  the headers to use for the response
     * @param status   the status code to use for the response
     * @param exchange the current request and response
     * @return a {@code Mono} with the created {@code ResponseEntity}
     */
    protected Mono<ResponseEntity<Object>> createResponseEntity(
        @Nullable Object body, HttpHeaders headers, HttpStatus status, ServerWebExchange exchange) {

        return Mono.just(new ResponseEntity<>(body, headers, status));
    }
}
