package com.book.canary.category.application;

import static com.book.canary.category.application.dto.response.CreateCategoryResponse.BreadcrumbNode;

import com.book.canary.category.application.dto.request.CreateCategoryRequest;
import com.book.canary.category.application.dto.response.CategoryNodeResponse;
import com.book.canary.category.application.dto.response.CategoryTreeResponse;
import com.book.canary.category.application.dto.response.CreateCategoryResponse;
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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final CategoryClosureRepository categoryClosureRepository;

    private static final int MAX_DEPTH = 4;

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

    @Transactional(readOnly = true)
    public CategoryTreeResponse get(Long categoryId) {

        if (categoryId != null && categoryId == 0L) {
            return getOnlyRoots();
        }

        if (categoryId == null) {
            return getTree();
        }

        // return getSubTree(categoryId);

        return CategoryTreeResponse.of(List.of());
    }

    private CategoryTreeResponse getOnlyRoots() {
        List<Category> roots = categoryRepository.findAllRoot();
        List<CategoryNodeResponse> nodes = roots.stream()
                .map(c -> CategoryNodeResponse.of(c.getId(), c.getName()))
                .toList();
        return new CategoryTreeResponse(nodes);
    }

    private CategoryTreeResponse getTree() {
        List<Row> rows = categoryRepository.findAllByRelationShip();

        HashMap<Long, CategoryNodeResponse> map = new HashMap<>(rows.size());

        for (Row row : rows) {
            map.put(row.getId(), CategoryNodeResponse.mutable(row.getId(), row.getName()));
        }

        ArrayList<CategoryNodeResponse> roots = new ArrayList<>();
        for (Row row : rows) {
            CategoryNodeResponse child = map.get(row.getId());
            if (row.getParentId() == null) {
                roots.add(child);
            }

            if (row.getParentId() != null) {
                CategoryNodeResponse parent = map.get(row.getParentId());
                if (parent != null) {
                    parent.child().add(child);
                }
            }
        }
        Collator collator = Collator.getInstance(Locale.KOREAN);
        Comparator<CategoryNodeResponse> byName =
                Comparator.comparing(CategoryNodeResponse::name, collator);
        roots.sort(byName);
        ArrayDeque<CategoryNodeResponse> deque = new ArrayDeque<>(roots);

        while (!deque.isEmpty()) {
            CategoryNodeResponse node = deque.pop();
            node.child().sort(byName);
            node.child().forEach(deque::push);
        }

        List<CategoryNodeResponse> immutable = roots.stream().map(this::freeze).toList();

        return CategoryTreeResponse.of(immutable);
    }

    private CategoryNodeResponse freeze(CategoryNodeResponse n) {
        var frozenChildren = n.child().stream().map(this::freeze).toList();
        return new CategoryNodeResponse(n.id(), n.name(), frozenChildren);
    }


}
