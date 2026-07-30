package com.project.estate.dto.request;

import com.project.estate.enums.Gender;
import com.project.estate.util.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import static com.project.estate.enums.Gender.*;

public record UserRequest(

        @NotBlank(message = "User name is required")
        String username,

        @NotBlank(message = "First name is required")
        String firstName,

        @NotBlank(message = "Last name is required")
        String lastName,

        @NotBlank(message = "Email is required")
        @Email(message = "Email format is invalid")
        @UniqueEmail
        String email,

        @NotBlank(message = "Password is required")
        String password,

        @NotBlank(message = "Phone number is required")
        String phone,

        @NotBlank(message = "Address is required")
        String address,

        @GenderSubset(anyOf = {MALE, FEMALE, OTHER})
        Gender gender

) {

}
