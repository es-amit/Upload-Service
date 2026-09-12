package com.lcwd.uploadservice.dto;

public record PartSummary(
        int partNumber,
        String eTag,
        long size
) {
}
