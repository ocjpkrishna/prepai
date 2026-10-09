package com.ascorp.prepai.account.user.mapper;

import com.ascorp.prepai.account.auth.model.entity.User;
import com.ascorp.prepai.account.user.model.dto.UserProfileResponse;
import org.mapstruct.Mapper;

/** Turns the private User entity into the profile DTO, so the entity never leaves the module. */
@Mapper(componentModel = "spring")
public interface UserProfileMapper {

	UserProfileResponse toProfile(User user);
}
