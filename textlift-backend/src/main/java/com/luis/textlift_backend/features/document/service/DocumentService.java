package com.luis.textlift_backend.features.document.service;

import com.luis.textlift_backend.features.auth.domain.User;
import com.luis.textlift_backend.features.auth.repository.UserRepository;
import com.luis.textlift_backend.features.document.api.dto.GetUserUploadsResponseDto;
import com.luis.textlift_backend.features.document.domain.Document;
import com.luis.textlift_backend.features.document.domain.DocumentStatus;
import com.luis.textlift_backend.features.document.domain.ExtractedMetadata;
import com.luis.textlift_backend.features.document.repository.DocumentRepository;
import com.luis.textlift_backend.features.document.service.events.DocumentReadyForIdEvent;
import com.luis.textlift_backend.features.upload.domain.UploadSession;
import com.luis.textlift_backend.features.upload.repository.UploadSessionRepository;
import jakarta.transaction.Transactional;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.io.RandomAccessReadBufferedFile;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class DocumentService {
    private final DocumentRepository documentRepository;
    private final ApplicationEventPublisher events;
    private final UserRepository userRepository;
    private final UploadSessionRepository uploadSessionRepository;

    public DocumentService(DocumentRepository documentRepository, ApplicationEventPublisher events, UserRepository userRepository, UploadSessionRepository uploadSessionRepository) {
        this.documentRepository = documentRepository;
        this.events = events;
        this.userRepository = userRepository;
        this.uploadSessionRepository = uploadSessionRepository;
    }


    //We want an async function that upon listening for an event,
    //starts the process of extracting metadata/text.
    @Transactional
    public void processDocument(UUID documentId){
        //At this point, we should have hash, original filename, and file path to PDF file
        //First, fetch the document obj associated with ID
        Document docObj = documentRepository.findById(documentId)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Could not find document!!!"
                        ));

        //Ensure document in ready mode
        if(docObj.getStatus() != DocumentStatus.READY){
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Document is not ready to process!!!");
        }

        //Now we need to load the doc into memory
        try (PDDocument pdfFile =
                     Loader.loadPDF(new RandomAccessReadBufferedFile(docObj.getFilePath()))
        ) {
            //Call upon methods to extract the metadata + store the PDF text as a temp .txt file
            ExtractedMetadata metadata = extractMetadata(pdfFile);
            //After extracting this data, use textbook repo to create a textbook and fill fields.
            String textFilePath = extractText(pdfFile, documentId, docObj.getFilePath());

            //Update document obj's filepath to now reflect the .txt file
            docObj.setFilePath(textFilePath);
            documentRepository.save(docObj);

            events.publishEvent(new DocumentReadyForIdEvent(metadata, documentId));
        } catch (IOException e) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to read PDF",
                    e
            );
        }
    }

    public ExtractedMetadata extractMetadata(PDDocument pdf){
        PDDocumentInformation pdd = pdf.getDocumentInformation();
        return new ExtractedMetadata(pdd.getAuthor(), pdd.getTitle(), pdd.getSubject(), pdd.getKeywords());
    }

    // Returns filepath of where text was written
    public String extractText(PDDocument pdf, UUID documentId, String oldFilePath) throws IOException {
        //New file path where PDF text persists
        Path newFile = Paths.get("/tmp/textlift/uploads/" + documentId + ".txt");
        // Path of a PDF file (deleted after extraction)
        Path oldFile = Paths.get(oldFilePath);

        // Extract text from PDF
        PDFTextStripper stripper = new PDFTextStripper();
        int totalPages = pdf.getNumberOfPages();
        int window = 20;

        // Buffer and write 20 pages at a time into a text file to reduce memory usage
        try (BufferedWriter writer = Files.newBufferedWriter(newFile, StandardCharsets.UTF_8)) {
            for (int start = 1; start <= totalPages; start += window) {
                int end = Math.min(start + window - 1, totalPages);
                stripper.setStartPage(start);
                stripper.setEndPage(end);
                String chunk = stripper.getText(pdf);
                writer.write(chunk);
            }
            return newFile.toString();
        } catch (IOException e) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Unable to extract text from file",
                    e
            );
        }
        finally {
            //After writing text to file, we want to delete the PDF file since we don't need it anymore
            Files.deleteIfExists(oldFile);
        }
    }

    public List<GetUserUploadsResponseDto> getUserUploadedDocuments(){
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Could not find user!!!"
                        ));

        Optional<List<String>> hashes = uploadSessionRepository.findHashByUserId(user.getId());
        if(hashes.isEmpty()){
            return List.of();
        }

        //After getting the hashes of the files associted with the user, now we need to get the actual documents
        List<Document> documents = new ArrayList<>();
        for(String hash : hashes.get()){
            List<Document> currHashDocs = documentRepository.getDocumentsByHash(hash);
            if(!(currHashDocs.isEmpty())){
                documents.addAll(currHashDocs);
            }
        }

        List<GetUserUploadsResponseDto> responseDtos = new ArrayList<>();

        for (Document doc : documents) {
            try {
                var tb = doc.getTextbook();
                if (tb == null) {
                    responseDtos.add(new GetUserUploadsResponseDto("UNKNOWN", DocumentStatus.FAILED_TO_IDENTIFY_ISBN, doc.getId()));
                }else{
                    responseDtos.add(new GetUserUploadsResponseDto(tb.getTextbookName(), doc.getStatus(), doc.getId()));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return responseDtos;

    }

    @Transactional
    public void deleteUserDocument(UUID documentId){
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Could not find user!!!"
                        ));

        Document document = documentRepository.findById(documentId)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Could not find document!!!"
                        ));

        String hash = document.getHash();
        long deleted = uploadSessionRepository.deleteByUser_IdAndHash(user.getId(), hash);
        if (deleted == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Upload session not found");
        }

    }
}
