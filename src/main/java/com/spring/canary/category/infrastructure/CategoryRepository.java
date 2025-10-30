package com.spring.canary.category.infrastructure;


import com.spring.canary.category.domain.Category;
import java.util.List;
import org.springframework.data.repository.CrudRepository;

public interface CategoryRepository extends CrudRepository<Category, Long> {

    boolean existsByName(String name);

    boolean existsByNameIn(List<String> name);


}
