package com.luis.textlift_backend.features.auth.repository;

import com.luis.textlift_backend.features.auth.domain.User;
import com.luis.textlift_backend.features.upload.domain.UploadSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmail(String email);
    Optional<User> findByVerificationCode(String verificationCode);

}