package com.spring.canary.category.application;

import com.spring.canary.category.application.request.CreateCategoriesRequest;
import com.spring.canary.category.application.request.CreateCategoryRequest;
import com.spring.canary.category.application.response.GetCategoriesResponse;
import com.spring.canary.category.application.response.GetCategoryResponse;
import com.spring.canary.category.domain.Category;
import com.spring.canary.category.domain.CategoryClosure;
import com.spring.canary.category.domain.CategoryClosureId;
import com.spring.canary.category.domain.exception.CategoryErrorCode;
import com.spring.canary.category.domain.exception.CategoryException;
import com.spring.canary.category.infrastructure.CategoryClosureRepository;
import com.spring.canary.category.infrastructure.CategoryRepository;
import com.spring.canary.category.infrastructure.mybatis.CategoryMapper;
import com.spring.canary.snowflake.Snowflake;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
public class CategoryService {

    private final Snowflake snowflake;
    private final CategoryRepository categoryRepository;
    private final CategoryClosureRepository categoryClosureRepository;
    private final CategoryMapper mapper;

    private void validateCategoriesByName(List<String> names) {
        if (categoryRepository.existsByNameIn(names)) {
            throw new CategoryException(CategoryErrorCode.DUPLICATE_NAME);
        }
    }

    private void validateDuplicateName(String name) {
        if (categoryRepository.existsByName(name)) {
            throw new CategoryException(CategoryErrorCode.DUPLICATE_NAME);
        }
    }

    private boolean isLeaf(Long ancestorId) {
        return !categoryClosureRepository
                .existsByCategoryClosureId_AncestorIdAndLevel(ancestorId, 1);
    }

    private void createClosureRows(Category newNode, Category parent) {
        // 자기 자신의 경로 생성(id, id, 0)
        CategoryClosure self = CategoryClosure.create(
                CategoryClosureId.create(newNode.getCategoryId(), newNode.getCategoryId()),
                0,
                newNode,
                newNode
        );
        categoryClosureRepository.save(self);

        // 루트 노드인 경우 여기서 끝
        if (parent == null) {
            return;
        }

        // 부모의 모든 조상(자기 자신 포함, level=0 포함)
        List<CategoryClosure> ancestors = categoryClosureRepository
                .findAllByCategoryClosureId_DescendantId(parent.getCategoryId());

        // 각 조상에 대해 (AncestorId, CategoryId, level + 1) 추가
        List<CategoryClosure> toSave = ancestors.stream()
                .map(categoryClosure -> CategoryClosure.create(
                        CategoryClosureId.create(
                                categoryClosure.getAncestor().getCategoryId(),
                                newNode.getCategoryId()
                        ),
                        categoryClosure.getLevel() + 1,
                        categoryClosure.getAncestor(),
                        newNode
                ))
                .toList();

        categoryClosureRepository.saveAll(toSave);
    }


    @Transactional
    public GetCategoryResponse createByJpa(CreateCategoriesRequest request) {

        for (String name : request.names()) {
            if (categoryRepository.existsByName(name)) {
                throw new CategoryException(CategoryErrorCode.DUPLICATE_NAME);
            }
        }

        List<Category> categories = request.names().stream()
                .map(name -> Category.create(snowflake.nextId(), name))
                .toList();

        categoryRepository.saveAll(categories);

        return GetCategoriesResponse.from(categories);
    }


    @Transactional
    public GetCategoryResponse createByMyBatis(CreateCategoriesRequest request) {
        validateCategoriesByName(request.names());

        List<Category> categories = request.names().stream()
                .map(String::trim)
                .map(n -> Category.create(snowflake.nextId(), n))
                .toList();

        mapper.insertCategories(categories);
        return GetCategoriesResponse.from(categories);
    }

    private Category findCategoryById(Long categoryId) {
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new CategoryException(CategoryErrorCode.NOT_FOUND));
    }

    @Transactional
    public GetCategoryResponse create(CreateCategoryRequest request) {
        // 공백, 중복 검사
        final String name = request.name().trim();
        validateDuplicateName(name);

        final Long parentId = request.parentId();
        Category parent = null;

        // 부모 카테고리 존재하는 경우
        if (parentId != null) {
            parent = findCategoryById(parentId);

            // 중간 노드에 삽입 금지
            if (!isLeaf(parent.getCategoryId())) {
                throw new CategoryException(CategoryErrorCode.NOT_LEAF_PARENT);
            }
        }

        // 새 카테고리 생성
        Category created = categoryRepository.save(
                Category.create(snowflake.nextId(), name)
        );

        // 클로저 테이블 갱신
        createClosureRows(created, parent);

        return GetCategoryResponse.of(created, parentId);
    }
}