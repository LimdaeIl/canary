package com.spring.canary.category.domain;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "categories")
@Entity
public class Category {

    @Id
    @Column(name = "category_id", nullable = false, updatable = false, unique = true)
    private Long categoryId;

    @Column(name = "name")
    private String name;

    private Category(Long categoryId, String name) {
        this.categoryId = categoryId;
        this.name = name;
    }

    public static Category create(Long categoryId, String name) {
        return new Category(categoryId, name);
    }
}
