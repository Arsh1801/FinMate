package com.finmate.dashboard.dto;

import java.math.BigDecimal;

public class ExchangeRateDto {
    private String baseCurrency;
    private String targetCurrency;
    private BigDecimal rate;
    private String asOf;
    private String clientUsed;

    public ExchangeRateDto() {}

    public ExchangeRateDto(String baseCurrency, String targetCurrency, BigDecimal rate, String asOf, String clientUsed) {
        this.baseCurrency = baseCurrency;
        this.targetCurrency = targetCurrency;
        this.rate = rate;
        this.asOf = asOf;
        this.clientUsed = clientUsed;
    }

    public String getBaseCurrency() {
        return baseCurrency;
    }

    public void setBaseCurrency(String baseCurrency) {
        this.baseCurrency = baseCurrency;
    }

    public String getTargetCurrency() {
        return targetCurrency;
    }

    public void setTargetCurrency(String targetCurrency) {
        this.targetCurrency = targetCurrency;
    }

    public BigDecimal getRate() {
        return rate;
    }

    public void setRate(BigDecimal rate) {
        this.rate = rate;
    }

    public String getAsOf() {
        return asOf;
    }

    public void setAsOf(String asOf) {
        this.asOf = asOf;
    }

    public String getClientUsed() {
        return clientUsed;
    }

    public void setClientUsed(String clientUsed) {
        this.clientUsed = clientUsed;
    }
}
