package com.spring.canary.category.presentation;

import com.spring.canary.category.application.CategoryService;
import com.spring.canary.category.application.request.CreateCategoriesRequest;
import com.spring.canary.category.application.request.CreateCategoryRequest;
import com.spring.canary.category.application.response.GetCategoryResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RequiredArgsConstructor
@RequestMapping("/api/v1/categories")
@RestController
public class CategoryController {

    private final CategoryService categoryService;

    @PostMapping("/mybatis")
    public ResponseEntity<GetCategoryResponse> createByMybatis(
            @RequestBody @Valid CreateCategoriesRequest request
    ) {

        GetCategoryResponse response = categoryService.createByMyBatis(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @PostMapping("/jpa")
    public ResponseEntity<GetCategoryResponse> createByJpa(
            @RequestBody @Valid CreateCategoriesRequest request
    ) {

        GetCategoryResponse response = categoryService.createByJpa(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @PostMapping
    public ResponseEntity<GetCategoryResponse> create(
            @RequestBody @Valid CreateCategoryRequest request
    ) {
        GetCategoryResponse response = categoryService.create(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }
    
}
