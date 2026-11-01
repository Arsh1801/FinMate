package com.finmate.budget.repository;

import com.finmate.budget.entity.Budget;
import com.finmate.category.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BudgetRepository extends JpaRepository<Budget, Long> {

    List<Budget> findByUserIdAndBudgetYearAndBudgetMonth(Long userId, Integer year, Integer month);

    Optional<Budget> findByUserIdAndBudgetYearAndBudgetMonthAndCategory(Long userId, Integer year, Integer month, Category category);

    Optional<Budget> findByUserIdAndBudgetYearAndBudgetMonthAndCategoryIsNull(Long userId, Integer year, Integer month);

    @Query("SELECT b FROM Budget b WHERE b.user.id = :userId AND " +
           "(:categoryId IS NULL OR b.category.id = :categoryId OR (:categoryId = 0 AND b.category IS NULL)) " +
           "ORDER BY b.budgetYear DESC, b.budgetMonth DESC")
    List<Budget> findBudgetHistory(@Param("userId") Long userId, @Param("categoryId") Long categoryId);

    @Query("SELECT b FROM Budget b WHERE b.user.id = :userId AND " +
           "(b.budgetYear = :year AND b.budgetMonth = :month) AND b.category IS NOT NULL")
    List<Budget> findCategoryBudgetsForMonth(@Param("userId") Long userId,
                                            @Param("year") Integer year,
                                            @Param("month") Integer month);
}
