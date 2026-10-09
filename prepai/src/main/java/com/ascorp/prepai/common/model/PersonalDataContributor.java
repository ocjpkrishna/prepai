package com.ascorp.prepai.common.model;

import java.util.UUID;

/**
 * A module that holds a student's personal data. It exports its section for the data download and erases its rows
 * when the account is purged (spec 2.7). Modules register by declaring a bean of this type.
 */
public interface PersonalDataContributor {

	/** The key of this module's section in the export, for example "lessons". */
	String section();

	Object export(UUID userId);

	void erase(UUID userId);
}
