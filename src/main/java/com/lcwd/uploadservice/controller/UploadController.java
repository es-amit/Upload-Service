package com.lcwd.uploadservice.controller;

import com.lcwd.uploadservice.dto.*;
import com.lcwd.uploadservice.service.UploadService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/uploads")
@AllArgsConstructor
public class UploadController {

    private final UploadService uploadService;

    @PostMapping
    public ResponseEntity<InitiateUploadResponse> initiateUpload(@Valid @RequestBody InitiateUploadRequest request) {
        InitiateUploadResponse response = uploadService.initiateUpload(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{sessionId}")
    public  ResponseEntity<UploadStatusResponse> getStatus(@PathVariable String sessionId) {
        UUID id = UUID.fromString(sessionId);
        UploadStatusResponse response= uploadService.getStatus(id);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{sessionId}/parts/presign")
    public ResponseEntity<List<PresignedPartResponse>> presignParts(
            @PathVariable String sessionId,
            @Valid @RequestBody PresignPartsRequest request
    ) {
        UUID id = UUID.fromString(sessionId);
        List<PresignedPartResponse> response = uploadService.presignParts(id, request.partNumbers());
        return ResponseEntity.ok(response);
    }

    @PostMapping("{sessionId}/complete")
    public ResponseEntity<CompleteUploadResponse> completeUploadResponse(@PathVariable String sessionId) {
        UUID id = UUID.fromString(sessionId);
        CompleteUploadResponse response = uploadService.completeUpload(id);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{sessionId}")
    public ResponseEntity<Void> abortUpload(@PathVariable String sessionId) {
        UUID id = UUID.fromString(sessionId);
        uploadService.abortUpload(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{sessionId}/hls/{*subPath}")
    public void streamHls(@PathVariable UUID sessionId, @PathVariable String subPath, HttpServletResponse response) throws IOException {
        String relativePath = subPath.startsWith("/") ? subPath.substring(1) : subPath;
        StoredObject file = uploadService.getHlsFile(sessionId, relativePath);
        response.setContentLengthLong(file.contentLength());
        response.setContentType(file.contentType());
        try (InputStream in  = file.content()) {
            in.transferTo(response.getOutputStream());
        }
    }
}

