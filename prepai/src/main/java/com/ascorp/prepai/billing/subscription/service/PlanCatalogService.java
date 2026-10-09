package com.ascorp.prepai.billing.subscription.service;

import com.ascorp.prepai.billing.subscription.model.dto.PlanDto;
import com.ascorp.prepai.common.errors.ApiException;
import com.ascorp.prepai.common.errors.ErrorCode;
import com.ascorp.prepai.common.model.enums.Plan;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;

/** The plans on sale and their prices in rupees (spec 1.4); the limits come from the Plan enum. */
@Service
public class PlanCatalogService {

	private static final int PRO_PRICE_INR = 199;
	private static final int PRO_PLUS_PRICE_INR = 399;

	public List<PlanDto> plans() {
		return Arrays.stream(Plan.values()).map(this::toDto).toList();
	}

	/** Resolves a plan id from the API ("pro", "pro_plus"); only paid plans can be bought. */
	public Plan paidPlan(String planId) {
		Plan plan = Arrays.stream(Plan.values())
				.filter(candidate -> candidate.name().equalsIgnoreCase(planId))
				.findFirst()
				.orElseThrow(() -> new ApiException(ErrorCode.VALIDATION_FAILED, "Unknown plan."));
		if (plan == Plan.FREE) {
			throw new ApiException(ErrorCode.VALIDATION_FAILED, "The free plan needs no payment.");
		}
		return plan;
	}

	private PlanDto toDto(Plan plan) {
		return new PlanDto(plan.name().toLowerCase(Locale.ROOT), displayName(plan), price(plan),
				plan.getSessionsPerDay(), plan.getMaxSessionMinutes());
	}

	private int price(Plan plan) {
		return switch (plan) {
			case FREE -> 0;
			case PRO -> PRO_PRICE_INR;
			case PRO_PLUS -> PRO_PLUS_PRICE_INR;
		};
	}

	private String displayName(Plan plan) {
		return switch (plan) {
			case FREE -> "Free";
			case PRO -> "Pro";
			case PRO_PLUS -> "Pro+";
		};
	}
}
