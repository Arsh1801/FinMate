package com.finmate.category.repository;

import com.finmate.category.entity.Category;
import com.finmate.category.entity.CategoryType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    @Query("SELECT c FROM Category c WHERE " +
           "(c.user IS NULL OR c.user.id = :userId) AND " +
           "(:type IS NULL OR c.type = :type) AND " +
           "(:includeInactive = true OR c.isActive = true) " +
           "ORDER BY c.isSystem DESC, c.name ASC")
    List<Category> findAccessibleCategories(@Param("userId") Long userId,
                                            @Param("type") CategoryType type,
                                            @Param("includeInactive") boolean includeInactive);

    @Query("SELECT c FROM Category c WHERE " +
           "(:type IS NULL OR c.type = :type) AND " +
           "(:includeInactive = true OR c.isActive = true) " +
           "ORDER BY c.isSystem DESC, c.name ASC")
    List<Category> findAllCategoriesAdmin(@Param("type") CategoryType type,
                                          @Param("includeInactive") boolean includeInactive);

    @Query("SELECT c FROM Category c WHERE " +
           "LOWER(c.name) = LOWER(:name) AND c.type = :type AND " +
           "((:userId IS NULL AND c.user IS NULL) OR (c.user.id = :userId))")
    Optional<Category> findDuplicate(@Param("name") String name,
                                     @Param("type") CategoryType type,
                                     @Param("userId") Long userId);

    Optional<Category> findByNameIgnoreCaseAndIsSystemTrue(String name);
}
