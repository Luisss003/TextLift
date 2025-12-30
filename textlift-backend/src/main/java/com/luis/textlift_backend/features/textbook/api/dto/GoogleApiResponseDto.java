package com.luis.textlift_backend.features.textbook.api.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GoogleApiResponseDto(
        String kind,
        Integer totalItems,
        List<Item> items
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Item(
            String kind,
            String id,
            VolumeInfo volumeInfo,
            SearchInfo searchInfo
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record VolumeInfo(
            String title,
            List<String> authors,
            String publisher,
            String publishedDate,
            String description,
            Integer pageCount,
            ImageLinks imageLinks
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ImageLinks(
            String smallThumbnail,
            String thumbnail
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record SearchInfo(
            String textSnippet
    ) {}
}
