package com.luis.textlift_backend.features.upload.repository;

import com.luis.textlift_backend.features.upload.domain.UploadSession;
import com.luis.textlift_backend.features.upload.domain.UploadStatus;
import jakarta.validation.constraints.NotBlank;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UploadSessionRepository extends JpaRepository<UploadSession, UUID> {
    Optional<UploadSession> findFirstByHashAndUploadStatusIn(@NotBlank String hash, List<UploadStatus> pending);

    @Query("select us.hash from UploadSession us where us.user.id = :userId")
    Optional<List<String>> findHashByUserId(UUID userId);

    //Ensures that users can only finalize/upload file/or check the status of an upload if they own the upload session
    Optional<UploadSession> findByIdAndUser_Id(UUID id, UUID userId);

    boolean existsByUser_IdAndHash(UUID userId, String hash);}
