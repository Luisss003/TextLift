package com.luis.textlift_backend.features.upload.repository;

import com.luis.textlift_backend.features.upload.domain.UploadSession;
import com.luis.textlift_backend.features.upload.domain.UploadStatus;
import jakarta.validation.constraints.NotBlank;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UploadSessionRepository extends JpaRepository<UploadSession, UUID> {
    Optional<UploadSession> findFirstByHashAndUploadStatusIn(@NotBlank String hash, List<UploadStatus> pending);
}
