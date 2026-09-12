package com.lcwd.uploadservice.dto;

import java.util.List;

public record ApiErrorResponse(String message, List<Integer> missingParts) {
}
