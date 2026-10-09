package com.ascorp.prepai.generation.imageextract.model;

/** A photo that passed the type and size checks, with its metadata removed. It lives in memory only (spec 2.7). */
public record SanitizedImage(ImageFormat format, byte[] bytes) {
}
