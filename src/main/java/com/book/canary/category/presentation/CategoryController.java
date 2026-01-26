package com.book.canary.category.presentation;

import com.book.canary.category.application.CategoryService;
import com.book.canary.category.application.dto.request.CreateCategoryRequest;
import com.book.canary.category.application.dto.request.UpdateCategoryNameRequest;
import com.book.canary.category.application.dto.response.CategoryTreeResponse;
import com.book.canary.category.application.dto.response.CreateCategoryResponse;
import com.book.canary.category.application.dto.response.UpdateCategoryNameResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RequestMapping("/api/v1/categories")
@RestController
public class CategoryController {

    private final CategoryService categoryService;

    @PostMapping
    public ResponseEntity<CreateCategoryResponse> create(
            @RequestBody @Valid CreateCategoryRequest request
    ) {
        CreateCategoryResponse response = categoryService.create(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @GetMapping
    public ResponseEntity<CategoryTreeResponse> get(
            @RequestParam(name = "categoryId", required = false) Long categoryId
    ) {
        CategoryTreeResponse responses = categoryService.get(categoryId);

        return ResponseEntity.ok(responses);
    }

    @PatchMapping("/{categoryId}")
    public ResponseEntity<UpdateCategoryNameResponse> updateName(
            @PathVariable Long categoryId,
            @RequestBody @Valid UpdateCategoryNameRequest request
    ) {
        UpdateCategoryNameResponse response = categoryService.updateName(categoryId, request);

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{categoryId}")
    public ResponseEntity<Void> delete(
            @PathVariable Long categoryId
    ) {
        categoryService.delete(categoryId);

        return  ResponseEntity.noContent().build();
    }
}
