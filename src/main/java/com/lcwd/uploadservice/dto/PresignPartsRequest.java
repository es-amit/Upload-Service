package com.lcwd.uploadservice.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Positive;

import java.util.List;

public record PresignPartsRequest(
        @NotEmpty List<@Positive Integer> partNumbers
) {
}
