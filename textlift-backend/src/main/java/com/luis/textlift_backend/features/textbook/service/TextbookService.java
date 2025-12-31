package com.luis.textlift_backend.features.textbook.service;

import com.luis.textlift_backend.features.document.domain.Document;
import com.luis.textlift_backend.features.document.domain.DocumentStatus;
import com.luis.textlift_backend.features.document.repository.DocumentRepository;
import com.luis.textlift_backend.features.textbook.domain.Textbook;
import com.luis.textlift_backend.features.textbook.repository.TextbookRepository;
import com.luis.textlift_backend.features.textbook.service.events.TextbookIdentifiedEvent;
import jakarta.transaction.Transactional;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;
import java.util.UUID;

import static org.apache.logging.log4j.util.Strings.isBlank;

@Service
public class TextbookService {
    private final TextbookRepository textbookRepository;
    private final DocumentRepository documentRepository;
    private final ApplicationEventPublisher events;
    private final GoogleBooksApi googleBooksApi;

    public TextbookService(TextbookRepository textbookRepository,
                           DocumentRepository documentRepository,
                           ApplicationEventPublisher events,
                           GoogleBooksApi googleBooksApi) {
        this.textbookRepository = textbookRepository;
        this.documentRepository = documentRepository;
        this.events = events;
        this.googleBooksApi = googleBooksApi;
    }

    @Transactional
    public void identifyTextbook(UUID documentId) {
        Document documentObj = documentRepository.findById(documentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Document not found"));


        // Extract ISBN, fall back to placeholder textbook if missing
        Optional<String> frontText = loadFrontMatterText(documentObj);
        Optional<String> isbn13 = frontText.flatMap(IsbnExtractor::extractBestIsbn13);
        Textbook textbook = resolveOrCreateTextbook(isbn13);


        // Link the document to the textbook
        if (documentObj.getTextbook() == null || !documentObj.getTextbook().getId().equals(textbook.getId())) {
            documentObj.setTextbook(textbook);
        }
        if (documentObj.getStatus() != DocumentStatus.TEXTBOOK_IDENTIFIED) {
            documentObj.setStatus(DocumentStatus.TEXTBOOK_IDENTIFIED);
        }
        documentRepository.save(documentObj); // optional; doc is managed in txn

        // Given the textbook (new or from DB)
        // we want to call Google Books API if the metadata fields (title, authors, edition)
        // are blank since that implies we haven't processed this before
        if(isbn13.isPresent()
                && (textbook.getTextbookName() == null || textbook.getTextbookName().isBlank()
                || textbook.getEdition() == null || textbook.getEdition().isBlank()
                || textbook.getAuthors() == null || textbook.getAuthors().isEmpty())){
            googleBooksApi.searchByIsbn(isbn13.get()).ifPresent(dto -> {
                if (isBlank(textbook.getTextbookName()) && !isBlank(dto.title())) {
                    textbook.setTextbookName(dto.title());
                }
                if ((textbook.getAuthors() == null || textbook.getAuthors().isEmpty())
                        && dto.authors() != null && !dto.authors().isEmpty()) {
                    textbook.setAuthors(dto.authors());
                }
            });

            textbookRepository.save(textbook);
        }

        //Lastly, we want to only kickstart an annotation generation if this textbook hasn't already had annotation
        if(textbook.getAnnotation() == null){
            events.publishEvent(new TextbookIdentifiedEvent(textbook.getId(), documentObj.getId()));
        }
    }

    private Textbook resolveOrCreateTextbook(Optional<String> isbn13) {
        if (isbn13.isPresent()) {
            return textbookRepository.findByIsbn(isbn13.get())
                    .orElseGet(() -> {
                        Textbook created = new Textbook();
                        created.setIsbn(isbn13.get());
                        return textbookRepository.save(created);
                    });
        }

        Textbook placeholder = new Textbook();
        placeholder.setIsbn("UNKNOWN_ISBN_" + UUID.randomUUID());
        placeholder.setTextbookName("UNKNOWN_TEXTBOOK");
        return textbookRepository.save(placeholder);
    }

    private Optional<String> loadFrontMatterText(Document doc) {
        String path = doc.getFilePath();
        if (path == null || path.isBlank()) return Optional.empty();

        int limitChars = 200_000;
        char[] buf = new char[limitChars];

        try (var reader = java.nio.file.Files.newBufferedReader(java.nio.file.Path.of(path))) {
            int read = reader.read(buf);
            if (read <= 0) return Optional.empty();
            return Optional.of(new String(buf, 0, read));
        } catch (Exception e) {
            return Optional.empty();
        }
    }



}
