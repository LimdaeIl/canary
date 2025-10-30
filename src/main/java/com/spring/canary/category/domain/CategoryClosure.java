package com.spring.canary.category.domain;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "categories_closure")
@Entity
public class CategoryClosure {

    @EmbeddedId
    private CategoryClosureId categoryClosureId;

    private int level;

    @MapsId("ancestorId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ancestor_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_closure_ancestor"))
    private Category ancestor;

    @MapsId("descendantId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "descendant_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_closure_descendant"))
    private Category descendant;

    private CategoryClosure(CategoryClosureId categoryClosureId, int level, Category ancestor,
            Category descendant) {
        this.categoryClosureId = categoryClosureId;
        this.level = level;
        this.ancestor = ancestor;
        this.descendant = descendant;
    }

    public static CategoryClosure create(
            CategoryClosureId categoryClosureId,
            int level,
            Category ancestor,
            Category descendant) {
        return new CategoryClosure(categoryClosureId, level, ancestor, descendant);
    }


}
