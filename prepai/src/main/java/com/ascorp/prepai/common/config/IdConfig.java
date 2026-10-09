package com.ascorp.prepai.common.config;

import java.util.UUID;
import java.util.function.Supplier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** The source of new IDs, injected so tests can replace random IDs with predictable ones. */
@Configuration
public class IdConfig {

	@Bean
	public Supplier<UUID> uuidSupplier() {
		return UUID::randomUUID;
	}
}
