package com.finmate.expense.split;

import com.finmate.common.exception.ApiException;
import com.finmate.common.exception.ErrorCode;
import com.finmate.expense.entity.SplitType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class SplitStrategyFactory {

    private final Map<SplitType, SplitStrategy> strategies;

    public SplitStrategyFactory(List<SplitStrategy> strategyList) {
        this.strategies = strategyList.stream()
                .collect(Collectors.toMap(SplitStrategy::getSupportedType, Function.identity()));
    }

    public SplitStrategy getStrategy(SplitType splitType) {
        SplitStrategy strategy = strategies.get(splitType);
        if (strategy == null) {
            throw ApiException.badRequest(ErrorCode.VALIDATION_FAILED, "Unsupported split type: " + splitType);
        }
        return strategy;
    }
}
