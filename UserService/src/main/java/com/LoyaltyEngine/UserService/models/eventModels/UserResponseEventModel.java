package com.LoyaltyEngine.UserService.models.eventModels;

import com.LoyaltyEngine.UserService.models.domain.enums.UserStatus;
import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class UserResponseEventModel {
    UUID userId;
    UserStatus userStatus;
}
