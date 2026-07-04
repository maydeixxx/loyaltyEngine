package com.LoyaltyEngine.UserService.services.interfaces;

import com.LoyaltyEngine.UserService.models.User;
import com.LoyaltyEngine.UserService.models.domain.UserDomain;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserMapper {
    User domainToEntity(UserDomain domain);

    UserDomain entityToDomain(User entity);
}
