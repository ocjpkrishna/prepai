package com.ascorp.prepai.generation.quality.model;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Settings of the nightly verifier (spec 8.1), bound from `prepai.quality.*`. */
@ConfigurationProperties("prepai.quality")
public record QualityProperties(int batchSize, String verifierModel) {
}
