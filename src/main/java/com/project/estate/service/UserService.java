package com.project.estate.service;


import com.project.estate.dto.request.UserRequest;
import com.project.estate.dto.response.UserResponse;
import com.project.estate.entity.User;
import com.project.estate.enums.ErrorCode;
import com.project.estate.exception.AppException;
import com.project.estate.mapper.UserMapper;
import com.project.estate.messaging.dto.EmailVerificationMessage;
import com.project.estate.messaging.producer.EmailProducer;
import com.project.estate.repository.UserRepository;
import com.project.estate.service.redis.VerificationTokenService;
import com.project.estate.enums.UserStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;

    private final UserMapper userMapper;

    private final PasswordEncoder passwordEncoder;

    private final VerificationTokenService verificationTokenService;

    private final EmailProducer emailProducer;

    public List<User> getUser() {
       return userRepository.findAll();
    }

    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public Page<UserResponse> getUsers(Specification<User> specification,
                                       Pageable pageable) {
        return userRepository
                .findAll(specification,pageable)
                .map(userMapper::toUserResponse);
    }

    @Cacheable(value = "users", key = "#userId")
    public UserResponse getUserById(String userId) {
        var user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        log.info("Access to database");
        log.info("User found: {}", user);
        return userMapper.toUserResponse(user);
    }


    public UserResponse createUser(UserRequest userRequest) {
        var user = userMapper.toUser(userRequest);
        user.setPassword(
                passwordEncoder.encode(user.getPassword())
        );
        user.setStatus(UserStatus.INACTIVE);
        userRepository.save(user);

        String token = UUID.randomUUID().toString();

        verificationTokenService.save(
                token,
                user.getEmail()
        );

        if (user.getId() != null) {
            try {
                emailProducer.send(
                        new EmailVerificationMessage(
                                user.getId(),
                                user.getEmail(),
                                token
                        )
                );
            } catch (Exception e) {
                log.error("Error sending confirmation email: {}", e.getMessage());
            }
        }
        return userMapper.toUserResponse(user);
    }

    public void deleteUser(String userId) {
        userRepository.deleteById(userId);
    }


}
