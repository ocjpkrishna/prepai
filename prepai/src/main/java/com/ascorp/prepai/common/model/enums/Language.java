package com.ascorp.prepai.common.model.enums;

import com.fasterxml.jackson.annotation.JsonProperty;

/** The language of a lesson. The API sends lowercase codes ("en", "hi"), so the JSON names are fixed here. */
public enum Language {

	@JsonProperty("en")
	EN,

	@JsonProperty("hi")
	HI
}
