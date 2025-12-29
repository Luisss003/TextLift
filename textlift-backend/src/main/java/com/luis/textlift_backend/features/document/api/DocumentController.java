package com.luis.textlift_backend.features.document.api;

import com.luis.textlift_backend.features.document.api.dto.GetUserUploadsResponseDto;
import com.luis.textlift_backend.features.document.service.DocumentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/documents")
public class DocumentController {
    private final DocumentService documentService;

    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }

    @GetMapping("/uploads")
    public ResponseEntity<List<GetUserUploadsResponseDto>> getUserUploadedDocuments(){
        List<GetUserUploadsResponseDto> getUserUploadsResponseDto = documentService.getUserUploadedDocuments();
        return ResponseEntity.ok(getUserUploadsResponseDto);
    }
}
