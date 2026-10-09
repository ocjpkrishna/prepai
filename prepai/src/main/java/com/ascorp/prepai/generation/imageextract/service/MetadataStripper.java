package com.ascorp.prepai.generation.imageextract.service;

import com.ascorp.prepai.generation.imageextract.model.ImageFormat;

/**
 * Removes the metadata of one photo format (EXIF with GPS, XMP, comments, text chunks) and keeps the picture. A
 * structurally broken file is refused, not repaired. One implementation per format; the sanitiser picks by format.
 */
interface MetadataStripper {

	ImageFormat format();

	byte[] strip(byte[] bytes);
}
