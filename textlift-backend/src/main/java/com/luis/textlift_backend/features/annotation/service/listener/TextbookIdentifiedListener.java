package com.luis.textlift_backend.features.annotation.service.listener;

import com.luis.textlift_backend.features.annotation.service.AnnotationService;
import com.luis.textlift_backend.features.textbook.service.events.TextbookIdentifiedEvent;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Service
public class TextbookIdentifiedListener {
    private final AnnotationService annotationService;
    public TextbookIdentifiedListener(AnnotationService annotationService) {
        this.annotationService = annotationService;
    }

    @Async("pipeLineExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onIdentified(TextbookIdentifiedEvent event){
        annotationService.generateAnnotations(event.textbookId(), event.documentId());
        annotationService.deleteExtractedText(event.documentId());
    }
}
