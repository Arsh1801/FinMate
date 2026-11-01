package com.finmate.goal.repository;

import com.finmate.goal.entity.GoalStatus;
import com.finmate.goal.entity.SavingsGoal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SavingsGoalRepository extends JpaRepository<SavingsGoal, Long> {

    List<SavingsGoal> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<SavingsGoal> findByUserIdAndStatusOrderByCreatedAtDesc(Long userId, GoalStatus status);

    @Query("SELECT g FROM SavingsGoal g WHERE g.user.id = :userId AND LOWER(g.name) = LOWER(:name) AND g.status <> 'ARCHIVED'")
    Optional<SavingsGoal> findActiveByName(@Param("userId") Long userId, @Param("name") String name);
}
