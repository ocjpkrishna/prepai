package com.ascorp.prepai.common.errors;

import com.ascorp.prepai.common.errors.ApiError.FieldIssue;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/** Turns every exception into the spec 4.7 error format. Unexpected failures are logged here and never shown. */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(ApiException.class)
	public ResponseEntity<ApiErrorResponse> handleApiException(ApiException exception) {
		ErrorCode code = exception.getCode();
		ApiErrorResponse body =
				ApiErrorResponse.of(code, exception.getMessage(), null, exception.getRetryAfterSeconds());
		return ResponseEntity.status(code.getStatus()).body(body);
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ApiErrorResponse> handleInvalidFields(MethodArgumentNotValidException exception) {
		List<FieldIssue> details = exception.getBindingResult().getFieldErrors().stream()
				.map(GlobalExceptionHandler::toFieldIssue)
				.toList();
		return buildResponse(ErrorCode.VALIDATION_FAILED, details, null);
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ResponseEntity<ApiErrorResponse> handleUnreadableBody() {
		return buildResponse(ErrorCode.VALIDATION_FAILED);
	}

	@ExceptionHandler(MaxUploadSizeExceededException.class)
	public ResponseEntity<ApiErrorResponse> handleUploadTooLarge() {
		return buildResponse(ErrorCode.IMAGE_TOO_LARGE);
	}

	@ExceptionHandler({MissingServletRequestPartException.class, MethodArgumentTypeMismatchException.class})
	public ResponseEntity<ApiErrorResponse> handleBadRequestParts() {
		return buildResponse(ErrorCode.VALIDATION_FAILED);
	}

	@ExceptionHandler(NoResourceFoundException.class)
	public ResponseEntity<ApiErrorResponse> handleUnknownPath() {
		return buildResponse(ErrorCode.NOT_FOUND);
	}

	@ExceptionHandler(AuthenticationException.class)
	public ResponseEntity<ApiErrorResponse> handleAuthenticationFailure() {
		return buildResponse(ErrorCode.UNAUTHENTICATED);
	}

	@ExceptionHandler(AccessDeniedException.class)
	public ResponseEntity<ApiErrorResponse> handleAccessDenied() {
		return buildResponse(ErrorCode.FORBIDDEN);
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ApiErrorResponse> handleUnexpected(Exception exception) {
		log.error("Unexpected failure while handling a request", exception);
		return buildResponse(ErrorCode.INTERNAL_ERROR);
	}

	private static FieldIssue toFieldIssue(FieldError error) {
		return new FieldIssue(error.getField(), error.getDefaultMessage());
	}

	private ResponseEntity<ApiErrorResponse> buildResponse(ErrorCode code) {
		return buildResponse(code, null, null);
	}

	private ResponseEntity<ApiErrorResponse> buildResponse(
			ErrorCode code, List<FieldIssue> details, Long retryAfterSeconds) {
		ApiErrorResponse body = ApiErrorResponse.of(code, code.getDefaultMessage(), details, retryAfterSeconds);
		return ResponseEntity.status(code.getStatus()).body(body);
	}
}
