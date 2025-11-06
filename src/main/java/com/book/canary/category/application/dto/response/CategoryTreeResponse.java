package com.book.canary.category.application.dto.response;

import java.util.List;

public record CategoryTreeResponse(
        List<CategoryNodeResponse> category
) {
    public static CategoryTreeResponse of(List<CategoryNodeResponse> child) {
        return new CategoryTreeResponse(child);
    }
}