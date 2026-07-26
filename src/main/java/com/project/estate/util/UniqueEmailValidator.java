package com.project.estate.util;

import com.project.estate.repository.UserRepository;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class UniqueEmailValidator implements ConstraintValidator<UniqueEmail, String> {

    private final UserRepository userRepository;

    @Override
    public boolean isValid(String email,
                           ConstraintValidatorContext context) {

        if(email == null)
            return true;

        return !userRepository.existsByEmail(email);
    }

}
