package com.enything.pdf.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

import java.time.Instant;
import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(BadRequestException.class)
	public ResponseEntity<ApiErrorResponse> handleBadRequest(
		BadRequestException ex,
		HttpServletRequest request
	) {
		return build(HttpStatus.BAD_REQUEST, ex.getMessage(), request.getRequestURI(), null);
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ApiErrorResponse> handleValidation(
		MethodArgumentNotValidException ex,
		HttpServletRequest request
	) {
		List<String> details = ex.getBindingResult()
			.getFieldErrors()
			.stream()
			.map(this::toDetail)
			.toList();

		return build(HttpStatus.BAD_REQUEST, "Validation failed", request.getRequestURI(), details);
	}

	@ExceptionHandler({MissingServletRequestPartException.class, MissingServletRequestParameterException.class})
	public ResponseEntity<ApiErrorResponse> handleMissingPart(
		Exception ex,
		HttpServletRequest request
	) {
		return build(HttpStatus.BAD_REQUEST, ex.getMessage(), request.getRequestURI(), null);
	}

	@ExceptionHandler(PdfProcessingException.class)
	public ResponseEntity<ApiErrorResponse> handlePdfProcessing(
		PdfProcessingException ex,
		HttpServletRequest request
	) {
		return build(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage(), request.getRequestURI(), null);
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ApiErrorResponse> handleUnexpected(
		Exception ex,
		HttpServletRequest request
	) {
		return build(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected server error", request.getRequestURI(), null);
	}

	private String toDetail(FieldError error) {
		String msg = error.getDefaultMessage() == null ? "invalid value" : error.getDefaultMessage();
		return error.getField() + ": " + msg;
	}

	private ResponseEntity<ApiErrorResponse> build(
		HttpStatus status,
		String message,
		String path,
		List<String> details
	) {
		ApiErrorResponse body = new ApiErrorResponse(
			Instant.now().toString(),
			status.value(),
			status.getReasonPhrase(),
			message,
			path,
			details
		);
		return ResponseEntity.status(status).body(body);
	}

}
