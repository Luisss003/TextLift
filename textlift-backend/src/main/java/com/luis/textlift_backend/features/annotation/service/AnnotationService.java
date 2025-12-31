package com.luis.textlift_backend.features.annotation.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.luis.textlift_backend.features.annotation.domain.Annotation;
import com.luis.textlift_backend.features.annotation.domain.AnnotationNote;
import com.luis.textlift_backend.features.annotation.repository.AnnotationNoteRepository;
import com.luis.textlift_backend.features.annotation.repository.AnnotationRepository;
import com.luis.textlift_backend.features.annotation.api.dto.AnnotationFetchResponseDto;
import com.luis.textlift_backend.features.auth.domain.User;
import com.luis.textlift_backend.features.auth.repository.UserRepository;
import com.luis.textlift_backend.features.document.domain.Document;
import com.luis.textlift_backend.features.document.domain.DocumentStatus;
import com.luis.textlift_backend.features.document.repository.DocumentRepository;
import com.luis.textlift_backend.features.textbook.domain.Textbook;
import com.luis.textlift_backend.features.textbook.repository.TextbookRepository;
import com.luis.textlift_backend.features.upload.repository.UploadSessionRepository;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.converter.StructuredOutputConverter;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class AnnotationService {
    private final ChatClient chatClient;
    private final DocumentRepository documentRepository;
    private final TextbookRepository textbookRepository;
    private final AnnotationRepository annotationRepository;
    private final AnnotationNoteRepository annotationNoteRepository;
    private final UserRepository userRepository;
    private final UploadSessionRepository uploadSessionRepository;
    private record aiResponse(String note, String reason, String quote, String location) {}


    public AnnotationService(ChatClient.Builder chatClientBuilder, DocumentRepository documentRepository,
                             TextbookRepository textbookRepository, AnnotationRepository annotationRepository,
                             AnnotationNoteRepository annotationNoteRepository,
                             UserRepository userRepository,
                             UploadSessionRepository uploadSessionRepository) {
        this.chatClient = chatClientBuilder.build();
        this.documentRepository = documentRepository;
        this.textbookRepository = textbookRepository;
        this.annotationRepository = annotationRepository;
        this.annotationNoteRepository = annotationNoteRepository;
        this.userRepository = userRepository;
        this.uploadSessionRepository = uploadSessionRepository;
    }

    public void generateAnnotations(UUID textbookId, UUID documentId) {
        //First, we want to extract the textbook text and append it to the following prompt
        Document documentObj = documentRepository.findById(documentId)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Could not find document!!!"
                        ));

        Textbook texObj = textbookRepository.findById(textbookId)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Could not find textbook!!!"
                        ));

        //We want to split the textbook into individual chunks with enough context such that
        //AI does not take a long time to generate annotations
        try (BufferedReader reader = new BufferedReader(new FileReader(documentObj.getFilePath()))) {
            int targetChars = 12 * 1024;
            List<aiResponse> all = new ArrayList<>();
            StringBuilder sb = new StringBuilder(targetChars + 2048);

            String line;
            int count = 0;

            while ((line = reader.readLine()) != null) {

                // If adding this line would exceed the chunk size, send the current chunk first
                if (sb.length() + line.length() + 1 > targetChars && !sb.isEmpty()) {
                    String chunk = sb.toString();
                    sb.setLength(0);
                    var typeRef = new ParameterizedTypeReference<List<aiResponse>>() {};
                    List<aiResponse> chunkNotes = this.chatClient.prompt()
                            .user(buildPrompt(chunk))
                            .call()
                            .entity(typeRef);

                    if (chunkNotes != null) all.addAll(chunkNotes);
                }

                // Actually accumulate text
                sb.append(line).append('\n');
            }

            // flush remainder AFTER EOF
            if (!sb.isEmpty()) {
                var typeRef = new ParameterizedTypeReference<List<aiResponse>>() {};
                List<aiResponse> chunkNotes = this.chatClient.prompt()
                        .user(buildPrompt(sb.toString()))
                        .call()
                        .entity(typeRef);

                if (chunkNotes != null) all.addAll(chunkNotes);
            }
            //Now, process the returned JSON, and create AnnotationNote objects, and map to our annotation obj
            Annotation annotation = new Annotation();
            annotation.setTextbook(texObj);
            annotation.setVersion(annotation.getVersion() + 1);
            annotationRepository.save(annotation);

            for (aiResponse note : all) {
                AnnotationNote x = new AnnotationNote();
                x.setAnnotation(annotation);
                x.setNote(note.note());
                x.setReason(note.reason());
                x.setQuote(note.quote());
                x.setLocation(note.location());
                annotationNoteRepository.save(x);
            }

            documentObj.setStatus(DocumentStatus.ANNOTATIONS_READY);
            documentRepository.save(documentObj);
        }catch(IOException e) {
            e.printStackTrace();
        }
    }

    public void deleteExtractedText(UUID documentId) {
        documentRepository.findById(documentId).ifPresent(doc -> {
            String path = doc.getFilePath();
            if (path != null && !path.isBlank()) {
                try { Files.deleteIfExists(Path.of(path)); } catch (IOException ignored) {}
            }
        });
    }

    public AnnotationFetchResponseDto getNotesByDocId(UUID docId) {
        //Fetch annotation which is parent of annotation notes
        Document document = documentRepository.findById(docId)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Could not find document associated with this ID..."
                        ));
        ensureUserOwnsUploadForDocument(document);
        //Assumption that this is only ever called if the document has been fully processed
        return new AnnotationFetchResponseDto(document.getTextbook().getAnnotation().getNotes());
    }

    private void ensureUserOwnsUploadForDocument(Document document) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User user = userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));

        String hash = document.getHash();
        if (hash == null || !uploadSessionRepository.existsByUser_IdAndHash(user.getId(), hash)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You do not have access to this document");
        }
    }

    private String buildPrompt(String chunk) {
        return """
            You are an information extraction engine.
            
            Task: From the TEXT, extract only statements that are outdated, wrong, or questionable.
            If none exist, return [].
            
            Output rules (MUST follow exactly):
            - Return ONLY valid JSON (no prose).
            - Return a JSON array (even if one item).
            - Each item is an object with exactly these 4 keys, all STRING values:
              - note
              - reason
              - quote
              - location
            - Do not include markdown, backticks, or code fences.
            
            TEXT:
            """ + chunk;
    }

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static String sanitizeModelJson(String s){
        if(s == null) return null;
        s = s.replace("\\_", "_");
        s = s.replace("\\0", "\\u0000");

        return s;
    }
}
