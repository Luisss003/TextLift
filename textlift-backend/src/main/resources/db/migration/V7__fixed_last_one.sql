ALTER TABLE upload_session
DROP CONSTRAINT upload_session_upload_status_check;

ALTER TABLE upload_session
    ADD CONSTRAINT upload_session_upload_status_check
        CHECK (upload_status IN ('UPLOADING','UPLOADED','PENDING','FAILED', 'PREMATURE_HIT'));