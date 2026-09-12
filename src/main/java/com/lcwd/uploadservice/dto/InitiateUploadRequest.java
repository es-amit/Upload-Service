package com.lcwd.uploadservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public record InitiateUploadRequest(
        @NotBlank String fileName,
        @NotBlank String contentType,
        @Positive long fileSize,
        @NotBlank String fingerprint
) {
}
