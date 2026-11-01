package com.finmate.expense.split;

import com.finmate.expense.entity.SplitType;

import java.math.BigDecimal;
import java.util.List;

public interface SplitStrategy {

    SplitType getSupportedType();

    List<CalculatedSplitResult> calculateSplits(BigDecimal totalAmount, List<SplitItemInput> inputs);
}
