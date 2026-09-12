package com.lcwd.uploadservice.controller;

import com.lcwd.uploadservice.dto.InitiateUploadRequest;
import com.lcwd.uploadservice.dto.InitiateUploadResponse;
import com.lcwd.uploadservice.service.UploadService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}
