package com.finmate.transaction.repository;

import com.finmate.transaction.entity.RecurringRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface RecurringRuleRepository extends JpaRepository<RecurringRule, Long> {

    List<RecurringRule> findByUserIdOrderByCreatedAtDesc(Long userId);

    @Query("SELECT r FROM RecurringRule r WHERE r.isActive = true AND r.nextRunDate <= :date")
    List<RecurringRule> findDueRules(@Param("date") LocalDate date);
}
