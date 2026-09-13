-- status is a native MySQL ENUM, not VARCHAR — V2 added the new HLS columns but missed
-- widening this, so PROCESSING/READY/FAILED get rejected by MySQL ("Data truncated for
-- column 'status'") the moment the app tries to save one of them.
ALTER TABLE upload_sessions
    MODIFY COLUMN status ENUM('INITIATED','UPLOADING','COMPLETED','ABORTED','PROCESSING','READY','FAILED') NOT NULL;
