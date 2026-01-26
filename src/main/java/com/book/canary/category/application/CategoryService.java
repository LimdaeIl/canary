package com.book.canary.category.application;

import static com.book.canary.category.application.dto.response.CreateCategoryResponse.BreadcrumbNode;

import com.book.canary.category.application.dto.request.CreateCategoryRequest;
import com.book.canary.category.application.dto.request.UpdateCategoryNameRequest;
import com.book.canary.category.application.dto.response.CategoryNodeResponse;
import com.book.canary.category.application.dto.response.CategoryTreeResponse;
import com.book.canary.category.application.dto.response.CreateCategoryResponse;
import com.book.canary.category.application.dto.response.UpdateCategoryNameResponse;
import com.book.canary.category.domain.entity.Category;
import com.book.canary.category.domain.entity.CategoryClosure;
import com.book.canary.category.domain.entity.CategoryClosureId;
import com.book.canary.category.domain.repository.CategoryClosureRepository;
import com.book.canary.category.domain.repository.CategoryRepository;
import com.book.canary.category.domain.repository.CategoryRepository.Row;
import java.text.Collator;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@RequiredArgsConstructor
@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final CategoryClosureRepository categoryClosureRepository;

    private static final int MAX_DEPTH = 4;
    private static final Long ROOTS_ONLY = 0L;

    @Transactional
    public CreateCategoryResponse create(CreateCategoryRequest request) {
        // 이름 공백(DTO)
        final String name = request.name().trim();

        if (categoryRepository.existsByName(name)) {
            throw new IllegalArgumentException(
                    "Category with name %s already exists".formatted(name));
        }

        Category category = categoryRepository.save(Category.create(request.name()));

        categoryClosureRepository.save(
                CategoryClosure.create(
                        CategoryClosureId.create(category.getId(), category.getId()),
                        category,
                        category,
                        0));

        int depth = 0;
        List<BreadcrumbNode> breadcrumb = new ArrayList<>();

        if (request.parentId() != null) {

            if (!categoryRepository.existsById(request.parentId())) {
                throw new IllegalArgumentException(
                        "Parent ID %s does not exist".formatted(request.parentId())
                );
            }

            int topDepthByParentId = categoryClosureRepository.findTopDepthByParentId(
                    (request.parentId())
            );

            if (topDepthByParentId + 1 > MAX_DEPTH) {
                throw new IllegalArgumentException("category max depth is " + MAX_DEPTH);
            }

            List<CategoryClosure> ancestorsByParentId =
                    categoryClosureRepository.findByAllDescendant(request.parentId());

            // 새로운 카테고리의 부모들
            List<CategoryClosure> newPaths = getCategoryClosures(ancestorsByParentId, category);
            categoryClosureRepository.saveAll(newPaths);

            depth = topDepthByParentId + 1;
            for (CategoryClosure cc : ancestorsByParentId) {
                breadcrumb.add(new BreadcrumbNode(
                        cc.getAncestor().getId(), cc.getAncestor().getName()));
            }
            breadcrumb.add(new BreadcrumbNode(category.getId(), category.getName()));
        }

        return new CreateCategoryResponse(
                category.getId(),
                category.getName(),
                request.parentId(),
                depth,
                breadcrumb
        );
    }

    private static List<CategoryClosure> getCategoryClosures(
            List<CategoryClosure> ancestorsByParentId, Category category) {
        List<CategoryClosure> newPaths = new ArrayList<>(ancestorsByParentId.size());

        for (CategoryClosure ancestor : ancestorsByParentId) {
            // ancestor, descendant, depth

            CategoryClosure parent = CategoryClosure.create(
                    CategoryClosureId.create(
                            ancestor.getId().getAncestorId(),
                            category.getId()
                    ),
                    ancestor.getAncestor(),
                    category,
                    ancestor.getDepth() + 1
            );
            newPaths.add(parent);
        }
        return newPaths;
    }

    @Transactional(readOnly = true)
    public CategoryTreeResponse get(Long categoryId) {
        if (ROOTS_ONLY.equals(categoryId)) {
            return getRootsOnly();
        }
        if (categoryId == null) {
            return getWholeTree();
        }
        return getSubTree(categoryId);
    }

    /**
     * 0: 루트만 (child = [])
     */
    private CategoryTreeResponse getRootsOnly() {
        List<Category> roots = categoryRepository.findAllRoot();
        var nodes = roots.stream()
                .map(c -> CategoryNodeResponse.of(c.getId(), c.getName()))
                .toList();
        return CategoryTreeResponse.of(nodes);
    }

    /**
     * null: 전체 트리 (child 재귀 포함)
     */
    private CategoryTreeResponse getWholeTree() {
        List<Row> rows = categoryRepository.findAllWithParent();
        var roots = buildTree(rows, /*sort*/ true, /*freeze*/ true);
        return CategoryTreeResponse.of(roots);
    }

    /**
     * 양수: 특정 노드 기준 서브트리
     */
    private CategoryTreeResponse getSubTree(Long rootId) {
        if (!categoryRepository.existsById(rootId)) {
            return CategoryTreeResponse.of(List.of());
        }
        List<Row> rows = categoryRepository.findSubtreeWithParent(rootId);
        var roots = buildTree(rows, /*sort*/ true, /*freeze*/ true);
        return CategoryTreeResponse.of(roots);
    }

    /**
     * 공통 트리 빌더 - rows: (id, name, parentId) - O(n)으로 parent-child 연결 - 정렬/불변화 옵션 제공
     */
    private List<CategoryNodeResponse> buildTree(List<Row> rows, boolean sort, boolean freeze) {
        if (rows.isEmpty()) {
            return List.of();
        }

        // 1) id -> 가변 노드 맵
        Map<Long, CategoryNodeResponse> map = new HashMap<>(rows.size());
        for (Row r : rows) {
            map.put(r.getId(), CategoryNodeResponse.mutable(r.getId(), r.getName()));
        }

        // 2) 부모-자식 연결 + 루트 수집
        List<CategoryNodeResponse> roots = new ArrayList<>();
        for (Row r : rows) {
            var child = map.get(r.getId());
            if (r.getParentId() == null) {
                roots.add(child);
            } else {
                var parent = map.get(r.getParentId());
                if (parent != null) {
                    parent.child().add(child);
                }
            }
        }

        // 3) (선택) 이름 정렬
        if (sort) {
            var collator = Collator.getInstance(Locale.KOREAN);
            var byName = Comparator.comparing(CategoryNodeResponse::name, collator);
            roots.sort(byName);
            var dq = new ArrayDeque<>(roots);
            while (!dq.isEmpty()) {
                var node = dq.pop();
                node.child().sort(byName);
                node.child().forEach(dq::push);
            }
        }

        // 4) (선택) 불변화
        if (freeze) {
            return roots.stream().map(CategoryNodeResponse::freeze).toList();
        }
        return roots;
    }

    @Transactional
    public UpdateCategoryNameResponse updateName(Long categoryId,
            UpdateCategoryNameRequest request) {
        final String name = request.name().trim();

        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Category not found By ID: %s".formatted(
                                categoryId)));

        if (categoryRepository.existsByName(name)) {
            throw new IllegalArgumentException(
                    "Category with name %s already exists".formatted(name));
        }

        category.updateName(name);

        return UpdateCategoryNameResponse.from(category);
    }

    @Transactional
    public void delete(Long categoryId) {
        // 1) 존재 확인
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Category not found By ID: %s".formatted(categoryId)));

        // 2) 리프 검사 (경쟁 조건에 민감하면 isLeafForUpdate() 사용)
        boolean leaf = categoryClosureRepository.isLeaf(categoryId) == 1;
        // boolean leaf = categoryClosureRepository.isLeafForUpdate(categoryId);
        if (!leaf) {
            throw new IllegalStateException("Only leaf category can be deleted. id=" + categoryId);
        }

        // 3) 클로저 경로 먼저 삭제 (FK 제약을 피하기 위해)
        int i = categoryClosureRepository.deleteAllPathsOfNode(categoryId);
        log.info("category delete count: {} id: {}", i, categoryId);

        // 4) 카테고리 삭제
        categoryRepository.delete(category);
    }
}

