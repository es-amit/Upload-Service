package com.lcwd.uploadservice.dto;

import com.lcwd.uploadservice.entity.UploadStatus;

import java.util.UUID;

public record CompleteUploadResponse(UUID sessionId, String objectKey, UploadStatus status) {
}
