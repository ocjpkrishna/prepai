package com.ascorp.prepai.quota.usage.model.dto;

import com.ascorp.prepai.common.model.enums.Plan;

/** The student's plan and today's use (spec 4.3). A null session limit means unlimited (PRO_PLUS). */
public record UsageResponse(Plan plan, long sessionsToday, Integer sessionLimit, int maxSessionMinutes) {
}
