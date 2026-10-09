package com.ascorp.prepai.common.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Public settings of the app (prepai.app.*): where links point, which origins may call the API, the sender. */
@ConfigurationProperties("prepai.app")
public record AppProperties(String baseUrl, List<String> corsAllowedOrigins, String mailFrom) {
}
