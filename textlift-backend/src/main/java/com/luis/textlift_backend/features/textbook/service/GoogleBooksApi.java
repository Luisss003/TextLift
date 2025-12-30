package com.luis.textlift_backend.features.textbook.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import com.luis.textlift_backend.features.textbook.api.dto.GoogleApiResponseDto;
import com.luis.textlift_backend.features.textbook.api.dto.TextbookLookupDto;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

@Component
public class GoogleBooksApi {
    private final String apiEndpoint = "https://www.googleapis.com/books/v1/volumes?q=isbn:";
    private final RestTemplate restTemplate;

    public GoogleBooksApi(RestTemplate restTemplate){
        this.restTemplate = restTemplate;
    }

    public Optional<TextbookLookupDto> searchByIsbn(String isbn) {
        try {
            GoogleApiResponseDto body =
                    restTemplate.getForObject(apiEndpoint + isbn, GoogleApiResponseDto.class);

            if (body == null || body.items() == null || body.items().isEmpty()) {
                return Optional.empty();
            }

            GoogleApiResponseDto.Item first = body.items().get(0);
            GoogleApiResponseDto.VolumeInfo vi = first.volumeInfo();
            if (vi == null) {
                return Optional.empty();
            }

            String thumbnail = (vi.imageLinks() != null) ? vi.imageLinks().thumbnail() : null;
            List<String> authors = (vi.authors() != null) ? vi.authors() : List.of();
            String snippet = (first.searchInfo() != null) ? first.searchInfo().textSnippet() : null;

            TextbookLookupDto dto = new TextbookLookupDto(
                    first.id(),
                    vi.title(),
                    authors,
                    vi.publisher(),
                    vi.publishedDate(),
                    vi.pageCount(),
                    thumbnail,
                    vi.description(),
                    snippet
            );

            return Optional.of(dto);

        } catch (RestClientResponseException e) {
            // Google returns 200 with totalItems=0 a lot, but just in case:
            // 404/400/etc -> treat as "not found"
            return Optional.empty();
        } catch (RestClientException e) {
            // Network / timeout / parsing issues -> also best-effort
            return Optional.empty();
        }
    }

}
