package com.lcwd.uploadservice.dto;

import com.lcwd.uploadservice.entity.UploadStatus;

import java.util.List;
import java.util.UUID;

public record UploadStatusResponse(
        UUID sessionId,
        String fileName,
        long fileSize,
        long chunkSize,
        int totalParts,
        UploadStatus status,
        List<Integer> uploadedParts,
        long bytesUploaded
) {
}
