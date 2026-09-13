package com.lcwd.uploadservice.entity;

public enum UploadStatus {
    INITIATED,
    UPLOADING,
    COMPLETED,
    ABORTED,

    // For Video Processing
    PROCESSING,
    READY,
    FAILED,
}
