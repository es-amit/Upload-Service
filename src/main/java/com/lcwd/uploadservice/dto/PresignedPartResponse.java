package com.lcwd.uploadservice.dto;

import java.time.Instant;

public record PresignedPartResponse(
        int partNumber,
        String url,
        Instant expiresAt
) {
}
