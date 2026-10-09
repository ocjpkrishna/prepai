package com.ascorp.prepai.billing.subscription.mapper;

import com.ascorp.prepai.billing.subscription.model.dto.CheckoutResponse;
import com.ascorp.prepai.billing.subscription.model.entity.Subscription;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/** Turns the private Subscription entity into DTOs, so the entity never leaves the module. */
@Mapper(componentModel = "spring")
public interface SubscriptionMapper {

	@Mapping(target = "subscriptionId", source = "id")
	@Mapping(target = "planId", expression = "java(subscription.getPlan().name().toLowerCase())")
	CheckoutResponse toCheckout(Subscription subscription);
}
