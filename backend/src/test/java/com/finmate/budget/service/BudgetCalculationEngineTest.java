package com.finmate.budget.service;

import com.finmate.budget.dto.BudgetDto;
import com.finmate.budget.entity.Budget;
import com.finmate.budget.entity.BudgetStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class BudgetCalculationEngineTest {

    private BudgetCalculationEngine engine;

    @BeforeEach
    void setUp() {
        engine = new BudgetCalculationEngine();
    }

    @Test
    @DisplayName("PRD Section 6.6 Worked Example: Food budget ₹6000 with ₹5200 spent (86.67% warning)")
    void testFoodBudgetWorkedExample() {
        Budget budget = new Budget();
        budget.setId(10L);
        budget.setBudgetYear(2024);
        budget.setBudgetMonth(10);
        budget.setLimitAmount(new BigDecimal("6000.00"));
        budget.setAlertThreshold(80);

        BigDecimal spent = new BigDecimal("5200.00");
        BudgetDto result = engine.calculateBudgetMetrics(budget, spent);

        assertEquals("6000.00", result.getLimitAmount());
        assertEquals("5200.00", result.getSpentAmount());
        assertEquals("800.00", result.getRemainingAmount());
        assertEquals(new BigDecimal("86.67"), result.getUtilizationPercent());
        assertEquals(BudgetStatus.WARNING, result.getStatus());
        assertFalse(result.isExceeded());
        assertEquals("0.00", result.getOverspentAmount());
    }

    @Test
    @DisplayName("Budget Exceeded: Spent exceeds limit")
    void testBudgetExceeded() {
        Budget budget = new Budget();
        budget.setId(11L);
        budget.setBudgetYear(2024);
        budget.setBudgetMonth(10);
        budget.setLimitAmount(new BigDecimal("6000.00"));
        budget.setAlertThreshold(80);

        BigDecimal spent = new BigDecimal("6500.00");
        BudgetDto result = engine.calculateBudgetMetrics(budget, spent);

        assertEquals("6500.00", result.getSpentAmount());
        assertEquals("-500.00", result.getRemainingAmount());
        assertEquals(new BigDecimal("108.33"), result.getUtilizationPercent());
        assertEquals(BudgetStatus.EXCEEDED, result.getStatus());
        assertTrue(result.isExceeded());
        assertEquals("500.00", result.getOverspentAmount());
    }

    @Test
    @DisplayName("Budget Safe: Spent is below alert threshold")
    void testBudgetSafe() {
        Budget budget = new Budget();
        budget.setId(12L);
        budget.setBudgetYear(2024);
        budget.setBudgetMonth(10);
        budget.setLimitAmount(new BigDecimal("6000.00"));
        budget.setAlertThreshold(80);

        BigDecimal spent = new BigDecimal("3000.00");
        BudgetDto result = engine.calculateBudgetMetrics(budget, spent);

        assertEquals("3000.00", result.getSpentAmount());
        assertEquals("3000.00", result.getRemainingAmount());
        assertEquals(new BigDecimal("50.00"), result.getUtilizationPercent());
        assertEquals(BudgetStatus.SAFE, result.getStatus());
        assertFalse(result.isExceeded());
    }
}
