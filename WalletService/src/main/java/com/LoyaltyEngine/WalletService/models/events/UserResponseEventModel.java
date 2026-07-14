package com.LoyaltyEngine.WalletService.models.events;

import com.LoyaltyEngine.WalletService.models.domain.UserStatus;
import lombok.Data;

@Data
public class UserResponseEventModel {
    Long userId;
    UserStatus userStatus;
}
