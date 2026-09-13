package com.lcwd.uploadservice.service;

import com.lcwd.uploadservice.dto.*;

import java.util.List;
import java.util.UUID;

public interface UploadService {
    // POST /api/uploads — resumes if an INITIATED/UPLOADING session matches the fingerprint,
    // otherwise computes chunkSize/totalParts, calls storageService.createMultipartUpload, saves session
    InitiateUploadResponse initiateUpload(InitiateUploadRequest request);


    // GET /api/uploads/{id} — loads session, calls storageService.listParts, merges into a status view
    UploadStatusResponse getStatus(UUID sessionId);


    // POST /api/uploads/{id}/parts/presign — validates partNumbers against totalParts/status,
    // delegates to storageService.presignUploadPart for each, marks session UPLOADING
    List<PresignedPartResponse> presignParts(UUID sessionId, List<Integer> partNumbers);

    // POST /api/uploads/{id}/complete — calls storageService.listParts, verifies 1..totalParts all present
    // (throws IncompleteUploadException with missing part numbers if not), then completeMultipartUpload,
    // marks session COMPLETED
    CompleteUploadResponse completeUpload(UUID sessionId);

    // DELETE /api/uploads/{id} — calls storageService.abortMultipartUpload, marks session ABORTED
    void abortUpload(UUID sessionId);

    // Used by the scheduled cleanup job — finds sessions stale beyond the configured window and aborts each
    void cleanupStaleUploads();

    StoredObject getHlsFile(UUID sessionId, String relativePath);
}
