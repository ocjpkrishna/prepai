package com.ascorp.prepai.account.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Sign-up throttle (prepai.privacy.signups-per-ip-per-hour, from SIGNUPS_PER_IP_PER_HOUR). */
@ConfigurationProperties("prepai.privacy")
public record SignupProperties(int signupsPerIpPerHour) {
}
