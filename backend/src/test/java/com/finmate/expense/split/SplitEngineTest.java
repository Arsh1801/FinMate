package com.finmate.expense.split;

import com.finmate.common.exception.ApiException;
import com.finmate.expense.entity.SplitType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SplitEngineTest {

    private SplitEngine splitEngine;

    @BeforeEach
    void setUp() {
        EqualSplitStrategy equalStrategy = new EqualSplitStrategy();
        ExactSplitStrategy exactStrategy = new ExactSplitStrategy();
        PercentageSplitStrategy percentageStrategy = new PercentageSplitStrategy();
        SplitStrategyFactory factory = new SplitStrategyFactory(List.of(equalStrategy, exactStrategy, percentageStrategy));
        splitEngine = new SplitEngine(factory);
    }

    @Test
    @DisplayName("PRD Section 6.4 Worked Example: Equal split of ₹5000 among 3 participants with remainder paise")
    void testEqualSplitWorkedExample() {
        BigDecimal totalAmount = new BigDecimal("5000.00");
        List<SplitItemInput> inputs = List.of(
                new SplitItemInput(1L, null, null),
                new SplitItemInput(2L, null, null),
                new SplitItemInput(3L, null, null)
        );

        List<CalculatedSplitResult> results = splitEngine.computeSplits(SplitType.EQUAL, totalAmount, inputs);

        assertNotNull(results);
        assertEquals(3, results.size());

        // 500,000 paise / 3 = 166,666 with remainder 2 paise
        // Deterministic allocation: User 1 gets 1666.67, User 2 gets 1666.67, User 3 gets 1666.66
        assertEquals(1L, results.get(0).getUserId());
        assertEquals(new BigDecimal("1666.67"), results.get(0).getShareAmount());

        assertEquals(2L, results.get(1).getUserId());
        assertEquals(new BigDecimal("1666.67"), results.get(1).getShareAmount());

        assertEquals(3L, results.get(2).getUserId());
        assertEquals(new BigDecimal("1666.66"), results.get(2).getShareAmount());

        // Invariant: sum of shares equals totalAmount
        BigDecimal sum = results.stream()
                .map(CalculatedSplitResult::getShareAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertEquals(totalAmount, sum);
    }

    @Test
    @DisplayName("Exact Split: Successful distribution and validation")
    void testExactSplitSuccess() {
        BigDecimal totalAmount = new BigDecimal("5000.00");
        List<SplitItemInput> inputs = List.of(
                new SplitItemInput(1L, new BigDecimal("2000.00"), null),
                new SplitItemInput(2L, new BigDecimal("2000.00"), null),
                new SplitItemInput(3L, new BigDecimal("1000.00"), null)
        );

        List<CalculatedSplitResult> results = splitEngine.computeSplits(SplitType.EXACT, totalAmount, inputs);
        assertEquals(3, results.size());

        BigDecimal sum = results.stream()
                .map(CalculatedSplitResult::getShareAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertEquals(totalAmount, sum);
    }

    @Test
    @DisplayName("Exact Split: Throws exception when shares do not sum to total")
    void testExactSplitMismatch() {
        BigDecimal totalAmount = new BigDecimal("5000.00");
        List<SplitItemInput> inputs = List.of(
                new SplitItemInput(1L, new BigDecimal("2000.00"), null),
                new SplitItemInput(2L, new BigDecimal("2000.00"), null),
                new SplitItemInput(3L, new BigDecimal("999.00"), null)
        );

        assertThrows(ApiException.class, () -> splitEngine.computeSplits(SplitType.EXACT, totalAmount, inputs));
    }

    @Test
    @DisplayName("Percentage Split: 50%, 30%, 20% splits ₹5000 correctly")
    void testPercentageSplitSuccess() {
        BigDecimal totalAmount = new BigDecimal("5000.00");
        List<SplitItemInput> inputs = List.of(
                new SplitItemInput(1L, null, new BigDecimal("50.00")),
                new SplitItemInput(2L, null, new BigDecimal("30.00")),
                new SplitItemInput(3L, null, new BigDecimal("20.00"))
        );

        List<CalculatedSplitResult> results = splitEngine.computeSplits(SplitType.PERCENTAGE, totalAmount, inputs);
        assertEquals(new BigDecimal("2500.00"), results.get(0).getShareAmount());
        assertEquals(new BigDecimal("1500.00"), results.get(1).getShareAmount());
        assertEquals(new BigDecimal("1000.00"), results.get(2).getShareAmount());

        BigDecimal sum = results.stream()
                .map(CalculatedSplitResult::getShareAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertEquals(totalAmount, sum);
    }

    @Test
    @DisplayName("Edge Case C-01: Amount too small to split evenly without zero paise shares")
    void testSplitShareTooSmall() {
        BigDecimal totalAmount = new BigDecimal("0.02");
        List<SplitItemInput> inputs = List.of(
                new SplitItemInput(1L, null, null),
                new SplitItemInput(2L, null, null),
                new SplitItemInput(3L, null, null)
        );

        assertThrows(ApiException.class, () -> splitEngine.computeSplits(SplitType.EQUAL, totalAmount, inputs));
    }
}
