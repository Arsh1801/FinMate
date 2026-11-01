package com.finmate.expense.split;

import com.finmate.expense.entity.SplitType;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
public class SplitEngine {

    private final SplitStrategyFactory strategyFactory;

    public SplitEngine(SplitStrategyFactory strategyFactory) {
        this.strategyFactory = strategyFactory;
    }

    public List<CalculatedSplitResult> computeSplits(SplitType splitType, BigDecimal totalAmount, List<SplitItemInput> inputs) {
        SplitStrategy strategy = strategyFactory.getStrategy(splitType);
        return strategy.calculateSplits(totalAmount, inputs);
    }
}
