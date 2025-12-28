package com.luis.textlift_backend.features.upload.service;

import com.luis.textlift_backend.features.document.domain.Document;
import com.luis.textlift_backend.features.document.domain.DocumentStatus;
import com.luis.textlift_backend.features.document.repository.DocumentRepository;
import com.luis.textlift_backend.features.document.service.events.DocumentQueuedEvent;
import com.luis.textlift_backend.features.upload.api.dto.*;
import com.luis.textlift_backend.features.upload.domain.UploadMode;
import com.luis.textlift_backend.features.upload.domain.UploadSession;
import com.luis.textlift_backend.features.upload.domain.UploadStatus;
import com.luis.textlift_backend.features.upload.repository.UploadSessionRepository;
import jakarta.transaction.Transactional;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class UploadSessionService {

    private final UploadSessionRepository uploadRepo;
    private final DocumentRepository documentRepo;
    private final ApplicationEventPublisher events;
    //private final FileHashCache hashCache;

    public UploadSessionService(UploadSessionRepository uploadRepo, DocumentRepository documentRepo, ApplicationEventPublisher events){
        this.uploadRepo = uploadRepo;
        this.documentRepo = documentRepo;
        this.events = events;
    }

    public CreateUploadResponseDto createUpload(CreateUploadDto req){
        //Check business rules
        if(req.sizeBytes() > 500000000){
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "File is too large...");
        }
/*
        //Check cache to see if we've processed this file recently
        Optional<UUID> cacheDocId = hashCache.getDocumentId(req.hash());
        if(cacheDocId.isPresent()){
            return new CreateUploadResponseDto(UploadMode.CACHE_HIT,null,null,cacheDocId.get());
        }


*/

        Optional<UploadSession> existingUpload =
                uploadRepo.findFirstByHashAndUploadStatusIn(req.hash(),
                        List.of(UploadStatus.PENDING, UploadStatus.UPLOADING));

        if (existingUpload.isPresent()) {
            return new CreateUploadResponseDto(
                    UploadMode.CACHE_HIT_WAIT,
                    existingUpload.get().getId(),
                    existingUpload.get().getUploadStatus(),
                    null
            );
        }
        //Else, we want to verify whether the hash exist in the DB at all (processed, but not in cache)
        Optional<Document> existing = documentRepo.findByHash(req.hash());
        if(existing.isPresent()){
            if(existing.get().getStatus() == DocumentStatus.ANNOTATIONS_READY){
                return new CreateUploadResponseDto(UploadMode.CACHE_HIT,null,null,existing.get().getId());
            }
            //Otherwise, we are in the proecss of generating the annotations, so ask the user to try again later
            else if(existing.get().getStatus() == DocumentStatus.ANNOTATIONS_GENERATING){
                return new CreateUploadResponseDto(UploadMode.CACHE_HIT_WAIT, null, null, null);
            }
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Document exists but is in state: " + existing.get().getStatus());
        }else{
            //Generate an entry in the Upload table
            UploadSession session = new UploadSession();
            session.setUploadStatus(UploadStatus.PENDING);
            session.setMd5(req.hash());

            //Add to table
            UploadSession saved = uploadRepo.save(session);
            return new CreateUploadResponseDto(UploadMode.NEW_UPLOAD,saved.getId(), saved.getUploadStatus(), null);
        }
    }

    public UploadResponseDto uploadFile(UUID uploadId, MultipartFile file){
        //First, load session via ID
        UploadSession session = uploadRepo.findById(uploadId)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Could not find upload session!!!"
                        ));
        if(session.getUploadStatus() != UploadStatus.PENDING){
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Upload session has expired!!!");
        }

        //Update the session status
        session.setUploadStatus(UploadStatus.UPLOADING);

        try{
            //Generate a temp file path and write the directory to host
            Path dir = Path.of("/tmp/textlift/uploads/");
            Files.createDirectories(dir);

            //Specify .part for file uploading, then final filename
            Path finalPath = dir.resolve(uploadId + ".pdf");
            Path partPath = dir.resolve(uploadId + ".pdf.part");

            try(InputStream in = file.getInputStream()){
                long bytes = Files.copy(in, partPath, StandardCopyOption.REPLACE_EXISTING);
                Files.move(partPath, finalPath,
                        StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE);

                //Update session status
                session.setUploadStatus(UploadStatus.UPLOADED);
                session.setOriginalFileName(file.getOriginalFilename());
                uploadRepo.save(session);

                return new UploadResponseDto(session.getId(), session.getUploadStatus());
            }

        } catch(IOException e){
            //Persist failure so that if user polls upload, they can see that
            //the upload failed and try again.
            session.setUploadStatus(UploadStatus.FAILED);
            uploadRepo.save(session);
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to store uploaded file",
                    e
            );
        }


    }

    @Transactional
    public UploadFinalizeResponseDto finalizeUpload(UUID uploadId){
        //First, confirm that file was completely uploaded
        UploadSession session = uploadRepo.findById(uploadId)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Could not find upload session!!!"
                        ));

        if(session.getUploadStatus() != UploadStatus.UPLOADED){
            throw new ResponseStatusException(HttpStatus.CONFLICT, "File was not fully UPLOADED!!!");
        }

        //Next, generate an empty document and store the file path
        Document document = new Document();
        document.setStatus(DocumentStatus.READY);
        document.setFilePath("/tmp/textlift/uploads/" + uploadId + ".pdf");
        document.setOriginalFileName(session.getOriginalFileName());
        document.setHash(session.getMd5());

        documentRepo.save(document);

        //Publish finalization event so that async processor can begin extracting data immediately
        events.publishEvent(new DocumentQueuedEvent(document.getId()));

        return new UploadFinalizeResponseDto(document.getId(), document.getStatus());
    }

    public StatusResponseDto pollUploadStatus(UUID uploadId){
        UploadSession uploadSession = uploadRepo.findById(uploadId)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Could not find upload session!!!"
                        ));
        return new StatusResponseDto(uploadSession.getUploadStatus());
    }
}
