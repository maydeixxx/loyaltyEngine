package com.LoyaltyEngine.UserService.models.eventModels;

import com.LoyaltyEngine.UserService.models.domain.UserStatus;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserResponseEventModel {
    Long userId;
    UserStatus userStatus;
}
