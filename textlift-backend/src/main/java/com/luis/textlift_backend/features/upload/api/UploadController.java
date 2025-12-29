package com.luis.textlift_backend.features.upload.api;

import com.luis.textlift_backend.features.upload.api.dto.*;
import com.luis.textlift_backend.features.upload.service.UploadSessionService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/upload")
public class UploadController {

    private final UploadSessionService uploadService;
    public UploadController(UploadSessionService uploadService) {
        this.uploadService = uploadService;
    }

    //First, upload file metadata to start an upload stream
    @PostMapping("")
    public ResponseEntity<CreateUploadResponseDto> uploadMetadata(@Valid @RequestBody CreateUploadDto req)
    {
        //Will either return upload ID for a new upload or return a document ID which can
        //then be used to fetch annotations.
        CreateUploadResponseDto response = this.uploadService.createUpload(req);
        return ResponseEntity.status(201).body(response);
    }

    //Next, actually begin to upload document bytes based on uploadID
    //Returns uploadId if upload successful
    @PostMapping(value = "/{uploadId}/file", consumes="multipart/form-data")
    public ResponseEntity<UploadResponseDto> uploadFile(@PathVariable UUID uploadId,
                                        @RequestParam("file") MultipartFile file){
        UploadResponseDto response = this.uploadService.uploadFile(uploadId, file);
        return ResponseEntity.status(200).body(response);
    }

    //Lastly, we can create a finalized endpoint, which creates a document obj
    //and maps it to the uploadID. Now in the document side, they can process the
    //actual document
    @PostMapping("/{uploadId}/finalize")
    public ResponseEntity<UploadFinalizeResponseDto> finalizeUpload(@PathVariable UUID uploadId){
        UploadFinalizeResponseDto response = this.uploadService.finalizeUpload(uploadId);
        return ResponseEntity.status(200).body(response);
    }

    //Poll status of upload for long uploads; frontend can do this in intervals
    @GetMapping("/{uploadId}/status")
    public ResponseEntity<StatusResponseDto> pollUploadStatus(@PathVariable UUID uploadId){
        StatusResponseDto response = this.uploadService.pollUploadStatus(uploadId);
        return ResponseEntity.status(200).body(response);
    }

}
