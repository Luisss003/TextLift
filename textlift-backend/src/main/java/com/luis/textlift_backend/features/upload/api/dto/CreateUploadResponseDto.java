package com.luis.textlift_backend.features.upload.api.dto;

import com.luis.textlift_backend.features.upload.domain.UploadMode;
import com.luis.textlift_backend.features.upload.domain.UploadStatus;

import javax.validation.constraints.NotNull;
import java.util.UUID;

public record CreateUploadResponseDto(
        @NotNull
        UploadMode uploadMode,

        //Set if we need to start a new upload (cache-miss)
        UUID uploadId,
        UploadStatus status,

        //Set if cache-hit
        UUID documentId
){}