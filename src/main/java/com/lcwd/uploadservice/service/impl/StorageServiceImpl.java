package com.lcwd.uploadservice.service.impl;

import com.lcwd.uploadservice.entity.PartSummary;
import com.lcwd.uploadservice.service.StorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.UploadPartPresignRequest;

import java.net.URL;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;


@Service
@RequiredArgsConstructor
public class StorageServiceImpl implements StorageService {

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;

    @Value("${minio.bucket}")
    private String bucketName;

    @Override
    public String createMultipartUpload(String objectKey, String contentType) {
        CreateMultipartUploadResponse response = s3Client.createMultipartUpload(CreateMultipartUploadRequest.builder().bucket(bucketName).key(objectKey).contentType(contentType).build());
        return response.uploadId();
    }

    @Override
    public URL presignUploadPart(String objectKey, String s3UploadId, int partNumber, Duration expiry) {
        UploadPartRequest uploadPartRequest = UploadPartRequest.builder().partNumber(partNumber).key(objectKey).uploadId(s3UploadId).bucket(bucketName).build();

        UploadPartPresignRequest presignRequest = UploadPartPresignRequest.builder().uploadPartRequest(uploadPartRequest).signatureDuration(expiry).build();

        return s3Presigner.presignUploadPart(presignRequest).url();
    }

    @Override
    public List<PartSummary> listParts(String objectKey, String s3UploadId) {
        ListPartsResponse listPartsResponse = s3Client.listParts(ListPartsRequest.builder().bucket(bucketName).key(objectKey).uploadId(s3UploadId).build());

        return listPartsResponse.parts().stream().map((e) -> new PartSummary(e.partNumber(), e.eTag(), e.size())).toList();
    }

    @Override
    public void completeMultipartUpload(String objectKey, String s3UploadId, List<PartSummary> parts) {
        s3Client.completeMultipartUpload(
                CompleteMultipartUploadRequest.builder()
                        .bucket(bucketName)
                        .key(objectKey)
                        .uploadId(s3UploadId)
                        .multipartUpload(
                                CompletedMultipartUpload.builder()
                                        .parts(parts
                                                .stream()
                                                .map((e) -> CompletedPart.builder()
                                                        .eTag(e.eTag())
                                                        .partNumber(e.partNumber())
                                                        .build()
                                                ).toList())
                                        .build())
                        .build());

    }

    @Override
    public void abortMultipartUpload(String objectKey, String s3UploadId) {

    }
}
