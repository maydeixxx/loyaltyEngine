package com.LoyaltyEngine.UserService.models.dto;

import lombok.Getter;

@Getter
public class UpdateUserDTO {
    private String email;
    private String firstName;
    private String lastName;
    private String password;
}
