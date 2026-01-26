package com.book.canary.category.application.dto.response;

import java.util.ArrayList;
import java.util.List;


public record CategoryNodeResponse(
        Long id,
        String name,
        List<CategoryNodeResponse> child
) {
    /** 루트만 내려줄 때: child = 빈 리스트(불변) */
    public static CategoryNodeResponse of(Long id, String name) {
        return new CategoryNodeResponse(id, name, List.of());
    }

    /** 트리 조립용: child = 가변 리스트 */
    public static CategoryNodeResponse mutable(Long id, String name) {
        return new CategoryNodeResponse(id, name, new ArrayList<>());
    }

    /** 재귀적으로 child를 불변화해서 안전하게 만들기 */
    public static CategoryNodeResponse freeze(CategoryNodeResponse n) {
        var frozenChildren = n.child().stream().map(CategoryNodeResponse::freeze).toList();
        return new CategoryNodeResponse(n.id(), n.name(), frozenChildren);
    }
}
