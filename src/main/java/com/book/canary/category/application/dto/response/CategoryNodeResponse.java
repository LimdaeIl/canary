package com.book.canary.category.application.dto.response;

import java.util.ArrayList;
import java.util.List;

public record CategoryNodeResponse(
        Long id,
        String name,
        List<CategoryNodeResponse> child
) {
    public static CategoryNodeResponse mutable(Long id, String name) {
        return new CategoryNodeResponse(id, name, new ArrayList<>());
    }

    public static CategoryNodeResponse of(Long id, String name) {
        return new CategoryNodeResponse(id, name, List.of());
    }

    public static CategoryNodeResponse of(Long id, String name, List<CategoryNodeResponse> child) {
        return new CategoryNodeResponse(id, name, child);
    }
}