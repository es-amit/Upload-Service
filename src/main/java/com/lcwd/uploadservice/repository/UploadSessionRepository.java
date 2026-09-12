package com.lcwd.uploadservice.repository;

import com.lcwd.uploadservice.entity.UploadSession;
import com.lcwd.uploadservice.entity.UploadStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UploadSessionRepository extends JpaRepository<UploadSession, UUID> {
    Optional<UploadSession> findFirstByFingerprintAndStatusIn(String fingerprint, List<UploadStatus> statuses);

    List<UploadSession> findByStatusInAndUpdatedAtBefore(List<UploadStatus> statuses, Instant cutoff);
}
