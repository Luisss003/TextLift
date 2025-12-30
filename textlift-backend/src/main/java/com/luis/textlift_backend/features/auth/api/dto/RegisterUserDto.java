package com.luis.textlift_backend.features.auth.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

public record RegisterUserDto (
        @NotBlank @Email String email,
        @NotBlank @Size(min=6) String password,
        @NotBlank @Size(min=3, max=30) String fullName
){}
