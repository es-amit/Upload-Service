package com.lcwd.uploadservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "upload_sessions", indexes = @Index(name = "idx_fingerprint_status", columnList = "fingerprint,status"))
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UploadSession {
    @Id
    @Builder.Default
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id = UUID.randomUUID();

    @Column(name = "file_name", nullable = false)
    private String fileName;

    @Column(name = "content_type", nullable = false)
    private String contentType;

    @Column(name = "file_size", nullable = false)
    private long fileSize;

    @Column(name = "chunk_size", nullable = false)
    private long chunkSize;

    @Column(name = "total_parts", nullable = false)
    private int totalParts;

    @Column(name = "object_key", nullable = false)
    private String objectKey;

    @Column(name = "s3_upload_id", nullable = false)
    private String s3UploadId;

    @Column(name = "fingerprint", nullable = false)
    private String fingerprint;

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private UploadStatus status;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @Column(name = "hls_master_key", length = 500)
    private String hlsMasterKey;

    @Column(name = "transcode_error", length = 1000)
    private String transcodeError;
}
