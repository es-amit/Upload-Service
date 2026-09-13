package com.lcwd.uploadservice.service;

import com.lcwd.uploadservice.entity.PartSummary;

import java.net.URL;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

public interface StorageService {

    // Starts a multipart upload, returns S3 uploadId
    String createMultipartUpload(String objectKey, String contentType);

    // Presigns a single part PUT URL for an existing multipart upload
    URL presignUploadPart(String objectKey, String s3UploadId, int partNumber, Duration expiry);

    // List Parts already stored in S3 for this uploadId
    List<PartSummary> listParts(String objectKey, String s3UploadId);

    // Finalizes the object from the given parts (ETags come from listParts, not the client)
    void completeMultipartUpload(String objectKey, String s3UploadId, List<PartSummary> parts);

    // Cancels and releases storage for an in-progress multipart upload
    void abortMultipartUpload(String objectKey, String s3UploadId);

    /// FFMPEG Helpers
    // Download the uploaded video
    URL presignDownload(String objectKey, Duration expiry);

    void uploadFile(String objectKey, Path file, String contentType);
}
