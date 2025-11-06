package com.book.canary.category.domain.entity;

import jakarta.persistence.Column;
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

@Getter(AccessLevel.PUBLIC)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "category_closures")
@Entity
public class CategoryClosure {

    @EmbeddedId
    private CategoryClosureId id;

    @MapsId("ancestorId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ancestor_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_ancestor")
    )
    private Category ancestor;

    @MapsId("descendantId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "descendant_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_descendant")
    )
    private Category descendant;

    @Column(name = "depth", nullable = false)
    private int depth;

    private CategoryClosure(
            CategoryClosureId id,
            Category ancestor,
            Category descendant,
            int depth) {
        this.id = id;
        this.ancestor = ancestor;
        this.descendant = descendant;
        this.depth = depth;
    }

    public static CategoryClosure create(
            CategoryClosureId id,
            Category ancestor,
            Category descendant,
            int depth) {
        return new CategoryClosure(id, ancestor, descendant, depth);
    }
}
