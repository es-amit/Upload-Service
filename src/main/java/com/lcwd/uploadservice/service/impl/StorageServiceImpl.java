package com.lcwd.uploadservice.service.impl;

import com.lcwd.uploadservice.entity.PartSummary;
import com.lcwd.uploadservice.service.StorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CreateMultipartUploadResponse;

import java.net.URL;
import java.time.Duration;
import java.util.List;


@Service
@RequiredArgsConstructor
public class StorageServiceImpl implements StorageService {

    private final S3Client s3Client;

    @Value("${minio.bucket}")
    private String bucketName;

    @Override
    public String createMultipartUpload(String objectKey, String contentType) {
         CreateMultipartUploadResponse response = s3Client.createMultipartUpload(
                CreateMultipartUploadRequest.builder()
                        .bucket(bucketName)
                        .key(objectKey)
                        .contentType(contentType)
                        .build()
        );
         return response.uploadId();
    }

    @Override
    public URL presignUploadPart(String objectKey, String s3UploadId, int partNumber, Duration expiry) {
        return null;
    }

    @Override
    public List<PartSummary> listParts(String objectKey, String s3UploadId) {
        return List.of();
    }

    @Override
    public void completeMultipartUpload(String objectKey, String s3UploadId, List<PartSummary> parts) {

    }

    @Override
    public void abortMultipartUpload(String objectKey, String s3UploadId) {

    }
}
