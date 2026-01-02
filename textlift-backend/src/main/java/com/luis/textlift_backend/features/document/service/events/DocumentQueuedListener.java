package com.luis.textlift_backend.features.document.service.events;

import com.luis.textlift_backend.features.document.service.DocumentService;
import com.luis.textlift_backend.features.textbook.service.TextbookService;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class DocumentQueuedListener {
    private final DocumentService documentService;
    private final TextbookService textbookService;
    public DocumentQueuedListener(DocumentService documentService, TextbookService textbookService) {
        this.documentService = documentService;
        this.textbookService = textbookService;
    }


    //Only begin work after DB has been updated
    @Async("pipeLineExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onQueued(DocumentQueuedEvent event){
        documentService.processDocument(event.documentId());
    }

    @Async("pipeLineExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onReadyToId(DocumentReadyForIdEvent event){
        textbookService.identifyTextbook(event.documentId());
    }
}
