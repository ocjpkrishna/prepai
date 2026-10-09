package com.ascorp.prepai.account.privacy.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Turns on the scheduled purge job (see PurgeJob). */
@Configuration
@EnableScheduling
public class PurgeConfig {
}
