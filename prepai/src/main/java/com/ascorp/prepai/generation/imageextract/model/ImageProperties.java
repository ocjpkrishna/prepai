package com.ascorp.prepai.generation.imageextract.model;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Settings of the photo input (`prepai.image.*`, spec 3.1.1): the largest accepted upload in bytes. */
@ConfigurationProperties("prepai.image")
public record ImageProperties(long maxBytes) {
}
