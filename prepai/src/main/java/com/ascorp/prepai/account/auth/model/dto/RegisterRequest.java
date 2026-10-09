package com.ascorp.prepai.account.auth.model.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/**
 * Sign-up. The password limit of 72 bytes is BCrypt's own limit. The birth date is used only for the age flag and is
 * never stored; a guardian email is required for under-18s (spec 2.7).
 */
public record RegisterRequest(
		@NotBlank @Email @Size(max = 255) String email,
		@NotBlank @Size(min = 8, max = 72) String password,
		@Size(max = 255) String name,
		@NotNull @PastOrPresent LocalDate birthDate,
		@AssertTrue boolean acceptsTerms,
		@Email @Size(max = 255) String guardianEmail) {
}
