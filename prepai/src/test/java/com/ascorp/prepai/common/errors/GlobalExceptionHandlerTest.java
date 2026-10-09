package com.ascorp.prepai.common.errors;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class GlobalExceptionHandlerTest {

	private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

	@Test
	void apiExceptionKeepsItsCodeMessageAndRetryDelay() {
		ApiException exception = new ApiException(ErrorCode.RATE_LIMITED, "Slow down, try in a minute.", 60L);

		ResponseEntity<ApiErrorResponse> response = handler.handleApiException(exception);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
		assertThat(response.getBody().error().code()).isEqualTo(ErrorCode.RATE_LIMITED);
		assertThat(response.getBody().error().message()).isEqualTo("Slow down, try in a minute.");
		assertThat(response.getBody().error().retryAfterSeconds()).isEqualTo(60L);
	}

	@Test
	void unexpectedFailureShowsOnlyAGenericInternalError() {
		ResponseEntity<ApiErrorResponse> response = handler.handleUnexpected(new IllegalStateException("db password"));

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
		assertThat(response.getBody().error().code()).isEqualTo(ErrorCode.INTERNAL_ERROR);
		assertThat(response.getBody().error().message()).doesNotContain("db password");
	}

	@Test
	void unreadableBodyIsAValidationFailure() {
		ResponseEntity<ApiErrorResponse> response = handler.handleUnreadableBody();

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
		assertThat(response.getBody().error().code()).isEqualTo(ErrorCode.VALIDATION_FAILED);
	}

	@Test
	void anOversizedUploadIsImageTooLarge() {
		ResponseEntity<ApiErrorResponse> response = handler.handleUploadTooLarge();

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONTENT_TOO_LARGE);
		assertThat(response.getBody().error().code()).isEqualTo(ErrorCode.IMAGE_TOO_LARGE);
	}

	@Test
	void aMissingPartOrABadQueryValueIsAValidationFailure() {
		ResponseEntity<ApiErrorResponse> response = handler.handleBadRequestParts();

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
	}
}
