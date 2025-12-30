package com.luis.textlift_backend.features.upload.service;

import com.luis.textlift_backend.features.auth.domain.User;
import com.luis.textlift_backend.features.document.domain.Document;
import com.luis.textlift_backend.features.document.domain.DocumentStatus;
import com.luis.textlift_backend.features.document.repository.DocumentRepository;
import com.luis.textlift_backend.features.document.service.events.DocumentQueuedEvent;
import com.luis.textlift_backend.features.upload.api.dto.*;
import com.luis.textlift_backend.features.upload.domain.UploadMode;
import com.luis.textlift_backend.features.upload.domain.UploadSession;
import com.luis.textlift_backend.features.upload.domain.UploadStatus;
import com.luis.textlift_backend.features.upload.repository.UploadSessionRepository;
import com.luis.textlift_backend.features.auth.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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
    private final UserRepository userRepository;

    public UploadSessionService(UploadSessionRepository uploadRepo, DocumentRepository documentRepo, ApplicationEventPublisher events, UserRepository userRepository){
        this.uploadRepo = uploadRepo;
        this.documentRepo = documentRepo;
        this.events = events;
        this.userRepository = userRepository;
    }

    public CreateUploadResponseDto createUpload(CreateUploadDto req){
        //Check business rules
        if(req.sizeBytes() > 250000000){
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "File is too large...");
        }


        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Could not find user!!!"
                        ));

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
        //Else, we want to verify whether the hash exists in the DB at all (processed, but not in cache)
        Optional<Document> existing = documentRepo.findByHash(req.hash());
        if(existing.isPresent()){
            if(existing.get().getStatus() == DocumentStatus.ANNOTATIONS_READY){
                return new CreateUploadResponseDto(UploadMode.CACHE_HIT,null,null,existing.get().getId());
            }
            //Otherwise, we are in the process of generating the annotations, so ask the user to try again later
            else if(existing.get().getStatus() == DocumentStatus.ANNOTATIONS_GENERATING){
                return new CreateUploadResponseDto(UploadMode.CACHE_HIT_WAIT, null, null, null);
            }
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Document exists but is in state: " + existing.get().getStatus());
        }else{
            //Generate an entry in the Upload table
            UploadSession session = new UploadSession();
            session.setUser(user);
            session.setUploadStatus(UploadStatus.PENDING);
            session.setHash(req.hash());

            //Add to table
            UploadSession saved = uploadRepo.save(session);
            return new CreateUploadResponseDto(UploadMode.NEW_UPLOAD,saved.getId(), saved.getUploadStatus(), null);
        }
    }

    public UploadResponseDto uploadFile(UUID uploadId, MultipartFile file){
        User user = currentUser();
        System.out.println("The current user is " + user.getFullName());
        //We want to load the upload session as long as the current request is by the user that owns it
        UploadSession session = uploadRepo.findByIdAndUser_Id(uploadId, user.getId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Upload session not found"));

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
                if(file.isEmpty()){
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File is empty!!!");
                }
                if(file.getSize() > 25L * 1024 * 1024){
                    throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "File is too large...");
                }

                Files.copy(in, partPath, StandardCopyOption.REPLACE_EXISTING);

                //Validate a file type
                validatePdf(partPath);

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
        User user = currentUser();

        UploadSession session = uploadRepo.findByIdAndUser_Id(uploadId, user.getId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Upload session not found"));

        if(session.getUploadStatus() != UploadStatus.UPLOADED){
            throw new ResponseStatusException(HttpStatus.CONFLICT, "File was not fully UPLOADED!!!");
        }

        //Do additional check to dedupe.
        Optional<Document> existing = documentRepo.findByHash(session.getHash());
        if (existing.isPresent()) {
            return new UploadFinalizeResponseDto(existing.get().getId(), existing.get().getStatus());
        }

        //Next, generate an empty document and store the file path
        Document document = new Document();
        document.setStatus(DocumentStatus.READY);
        document.setFilePath("/tmp/textlift/uploads/" + uploadId + ".pdf");
        document.setOriginalFileName(session.getOriginalFileName());
        document.setHash(session.getHash());
        documentRepo.save(document);

        //Publish finalization event so that async processor can begin extracting data immediately
        events.publishEvent(new DocumentQueuedEvent(document.getId()));

        return new UploadFinalizeResponseDto(document.getId(), document.getStatus());
    }

    public StatusResponseDto pollUploadStatus(UUID uploadId){
        User user = currentUser();

        UploadSession session = uploadRepo.findByIdAndUser_Id(uploadId, user.getId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Upload session not found"));
            return new StatusResponseDto(session.getUploadStatus());
    }

    private User currentUser(){
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found!!"));
    }

    //Checks magic bytes to validate file type
    private void validatePdf(Path partPath) {
        try (InputStream s = Files.newInputStream(partPath)) {
            byte[] head = s.readNBytes(5);
            boolean isPdf = head.length == 5
                    && head[0] == '%' && head[1] == 'P' && head[2] == 'D' && head[3] == 'F' && head[4] == '-';
            if (!isPdf){
                throw new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Not a PDF");
            }
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to validate upload", e);
        }
    }
}
