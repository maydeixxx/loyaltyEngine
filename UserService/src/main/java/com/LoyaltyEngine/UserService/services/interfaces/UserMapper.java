package com.LoyaltyEngine.UserService.services.interfaces;

import com.LoyaltyEngine.UserService.models.User;
import com.LoyaltyEngine.UserService.models.domain.UserDomain;
import com.LoyaltyEngine.UserService.models.dto.UserDTO;
import org.springframework.stereotype.Component;

@Component
public interface UserMapper {
    default User domainToEntity(UserDomain domain) {
        User user = new User();
        user.setCreatedAt(domain.getCreatedAt());
        user.setEmail(domain.getEmail());
        user.setFirstName(domain.getFirstName());
        user.setLastName(domain.getLastName());
        user.setId(domain.getId().value());
        user.setHashedPassword(domain.getPasswordHash().value());
        user.setRole(domain.getRole());
        user.setUpdatedAt(domain.getUpdatedAt());

        return user;
    }

    default UserDomain entityToDomain(User entity) {
        return UserDomain.restoreFromExisting(
                entity.getId(),
                entity.getEmail(),
                entity.getFirstName(),
                entity.getLastName(),
                entity.getHashedPassword(),
                entity.getRole(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    default UserDTO domainToDto(UserDomain domain) {
        return new UserDTO(
          domain.getId().value(),
          domain.getEmail(),
          domain.getFirstName(),
          domain.getLastName()
        );
    }
}
