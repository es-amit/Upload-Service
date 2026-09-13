package com.lcwd.uploadservice.service;

import java.util.UUID;

public interface TranscodeService {

    // Downloads the completed upload, transcodes it into an HLS rendition ladder
    // (360p/480p/720p/1080p, skipping any rung taller than the source), uploads the
    // output back to storage, and updates the session to READY/FAILED.
    // Runs asynchronously — the caller (UploadService#completeUpload) doesn't wait on it.
    void transcode(UUID sessionId);
}
