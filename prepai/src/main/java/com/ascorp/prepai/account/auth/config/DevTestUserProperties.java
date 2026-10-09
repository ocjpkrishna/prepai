package com.ascorp.prepai.account.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** The local test student (`prepai.dev-test-user.*`). Unset outside the local profile. */
@ConfigurationProperties("prepai.dev-test-user")
public record DevTestUserProperties(String email, String password) {
}
