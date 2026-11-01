package com.finmate.goal.service;

import com.finmate.common.util.DateUtils;
import com.finmate.goal.dto.SavingsGoalDto;
import com.finmate.goal.entity.GoalStatus;
import com.finmate.goal.entity.SavingsGoal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class GoalCalculationEngineTest {

    private GoalCalculationEngine engine;

    @BeforeEach
    void setUp() {
        engine = new GoalCalculationEngine();
    }

    @Test
    @DisplayName("PRD Section 6.7 Worked Example: Laptop goal ₹60000 with ₹45000 saved (75% completion)")
    void testLaptopGoalWorkedExample() {
        SavingsGoal goal = new SavingsGoal();
        goal.setId(20L);
        goal.setName("New Laptop");
        goal.setTargetAmount(new BigDecimal("60000.00"));
        goal.setCreatedDate(DateUtils.today().minusDays(60));
        goal.setTargetDate(DateUtils.today().plusDays(90));
        goal.setStatus(GoalStatus.ACTIVE);

        BigDecimal saved = new BigDecimal("45000.00");
        SavingsGoalDto result = engine.calculateGoalMetrics(goal, saved);

        assertEquals("60000.00", result.getTargetAmount());
        assertEquals("45000.00", result.getSavedAmount());
        assertEquals("15000.00", result.getRemainingAmount());
        assertEquals(new BigDecimal("75.00"), result.getCompletionPercent());
        assertTrue(result.getDaysLeft() > 0);
        assertNotNull(result.getRequiredMonthlySaving());
        assertTrue(result.isOnTrack());
    }

    @Test
    @DisplayName("Goal 100% Completed: Saved meets target")
    void testGoalFullyFunded() {
        SavingsGoal goal = new SavingsGoal();
        goal.setId(21L);
        goal.setName("Emergency Fund");
        goal.setTargetAmount(new BigDecimal("50000.00"));
        goal.setCreatedDate(DateUtils.today().minusDays(30));
        goal.setTargetDate(DateUtils.today().plusDays(30));
        goal.setStatus(GoalStatus.ACTIVE);

        BigDecimal saved = new BigDecimal("55000.00");
        SavingsGoalDto result = engine.calculateGoalMetrics(goal, saved);

        assertEquals("0.00", result.getRemainingAmount());
        assertEquals(new BigDecimal("100.00"), result.getCompletionPercent());
        assertEquals("0.00", result.getRequiredMonthlySaving());
    }
}
