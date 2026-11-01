package com.finmate.goal.repository;

import com.finmate.goal.entity.GoalContribution;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface GoalContributionRepository extends JpaRepository<GoalContribution, Long> {

    @Query("SELECT COALESCE(SUM(c.amount), 0) FROM GoalContribution c WHERE c.goal.id = :goalId")
    BigDecimal sumAmountByGoalId(@Param("goalId") Long goalId);

    Page<GoalContribution> findByGoalIdOrderByContributionDateDescCreatedAtDesc(Long goalId, Pageable pageable);

    List<GoalContribution> findByGoalIdOrderByContributionDateDescCreatedAtDesc(Long goalId);

    @Query("SELECT COALESCE(SUM(c.amount), 0) FROM GoalContribution c " +
           "WHERE c.goal.user.id = :userId AND c.goal.status IN ('ACTIVE', 'COMPLETED')")
    BigDecimal sumTotalSavingsForUser(@Param("userId") Long userId);
}
