package com.megamart.cartwishlist.exception;

import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import jakarta.validation.ConstraintViolationException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

	private static final String STATUS = "status";
	private static final String ERROR = "error";
	private static final String DETAILS = "details";

	@Override
	protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
		Map<String, Object> body = new HashMap<>();
		body.put(STATUS, HttpStatus.BAD_REQUEST.value());
		Map<String, String> errors = new HashMap<>();
		ex.getBindingResult().getAllErrors().forEach(err -> {
			String field = (err instanceof FieldError fieldError) ? fieldError.getField() : err.getObjectName();
			String msg = err.getDefaultMessage();
			errors.put(field, msg);
		});
		body.put("errors", errors);
		return ResponseEntity.badRequest().body(body);
	}

	@Override
	protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
		return ResponseEntity.badRequest().body(Map.of(
			STATUS, HttpStatus.BAD_REQUEST.value(),
			ERROR, "Malformed JSON or invalid value",
			DETAILS, ex.getMostSpecificCause().getMessage()
		));
	}

	@ExceptionHandler(MethodArgumentTypeMismatchException.class)
	public ResponseEntity<Object> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
		return ResponseEntity.badRequest().body(Map.of(
			STATUS, HttpStatus.BAD_REQUEST.value(),
			ERROR, "Parameter type mismatch",
			"parameter", ex.getName(),
			"requiredType", Optional.ofNullable(ex.getRequiredType()).map(Class::getSimpleName).orElse(null)
		));
	}

	@ExceptionHandler(ConstraintViolationException.class)
	public ResponseEntity<Object> handleConstraintViolation(ConstraintViolationException ex) {
		return ResponseEntity.badRequest().body(Map.of(
			STATUS, HttpStatus.BAD_REQUEST.value(),
			ERROR, "Constraint violation",
			DETAILS, ex.getMessage()
		));
	}

	@ExceptionHandler(ItemNotFoundException.class)
	public ResponseEntity<Object> handleNotFound(ItemNotFoundException ex) {
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
			STATUS, HttpStatus.NOT_FOUND.value(),
			ERROR, ex.getMessage()
		));
	}

	@ExceptionHandler(DataIntegrityViolationException.class)
	public ResponseEntity<Object> handleConflict(DataIntegrityViolationException ex) {
		return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
			STATUS, HttpStatus.CONFLICT.value(),
			ERROR, "Data integrity violation",
			DETAILS, ex.getMostSpecificCause().getMessage()
		));
	}

	@ExceptionHandler({DataAccessResourceFailureException.class})
	public ResponseEntity<Object> handleDbDown(Exception ex) {
		return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of(
			STATUS, HttpStatus.SERVICE_UNAVAILABLE.value(),
			ERROR, "Database unavailable"
		));
	}

	@Override
	protected ResponseEntity<Object> handleHttpRequestMethodNotSupported(HttpRequestMethodNotSupportedException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
		return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(Map.of(
			STATUS, HttpStatus.METHOD_NOT_ALLOWED.value(),
			ERROR, "Method not allowed",
			"method", ex.getMethod()
		));
	}

	@ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<Object> handleIllegalArgument(IllegalArgumentException ex) {
		return ResponseEntity.badRequest().body(Map.of(
			STATUS, HttpStatus.BAD_REQUEST.value(),
			ERROR, ex.getMessage()
		));
	}

	@ExceptionHandler(InsufficientStockException.class)
	public ResponseEntity<Object> handleInsufficientStock(InsufficientStockException ex) {
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
			STATUS, HttpStatus.BAD_REQUEST.value(),
			ERROR, ex.getMessage()
		));
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<Object> handleAny(Exception ex) {
		String errorId = UUID.randomUUID().toString();
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
			STATUS, HttpStatus.INTERNAL_SERVER_ERROR.value(),
			"errorId", errorId,
			ERROR, "Internal server error"
		));
	}
}
