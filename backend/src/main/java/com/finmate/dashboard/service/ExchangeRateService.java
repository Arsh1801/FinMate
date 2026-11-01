package com.finmate.dashboard.service;

import com.finmate.common.util.DateUtils;
import com.finmate.dashboard.dto.ExchangeRateDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@Service
public class ExchangeRateService {

    private static final Logger log = LoggerFactory.getLogger(ExchangeRateService.class);

    private final RestTemplate restTemplate;
    private final WebClient webClient;

    // Fallback baseline rates against INR
    private static final Map<String, BigDecimal> INR_BASELINES = Map.of(
            "USD", new BigDecimal("83.95"),
            "EUR", new BigDecimal("91.20"),
            "GBP", new BigDecimal("108.40"),
            "AED", new BigDecimal("22.86"),
            "SGD", new BigDecimal("64.15"),
            "CAD", new BigDecimal("61.50"),
            "AUD", new BigDecimal("55.80"),
            "JPY", new BigDecimal("0.56"),
            "INR", BigDecimal.ONE
    );

    @Autowired
    public ExchangeRateService(RestTemplate restTemplate, WebClient webClient) {
        this.restTemplate = restTemplate;
        this.webClient = webClient;
    }

    public ExchangeRateDto getRate(String base, String target, boolean useWebClient) {
        String baseCurr = (base != null && !base.isBlank()) ? base.toUpperCase() : "USD";
        String targetCurr = (target != null && !target.isBlank()) ? target.toUpperCase() : "INR";

        if (baseCurr.equals(targetCurr)) {
            return new ExchangeRateDto(baseCurr, targetCurr, BigDecimal.ONE.setScale(4, RoundingMode.HALF_UP),
                    DateUtils.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME), useWebClient ? "WebClient" : "RestTemplate");
        }

        // Attempt live rate via free API (e.g. open.er-api.com) with timeout handling
        try {
            if (useWebClient) {
                return fetchWithWebClient(baseCurr, targetCurr);
            } else {
                return fetchWithRestTemplate(baseCurr, targetCurr);
            }
        } catch (Exception ex) {
            log.warn("External exchange rate service call failed ({}), using fallback exchange matrix: {}",
                    ex.getMessage(), ex.getClass().getSimpleName());
            return getFallbackRate(baseCurr, targetCurr, useWebClient ? "WebClient-Fallback" : "RestTemplate-Fallback");
        }
    }

    private ExchangeRateDto fetchWithRestTemplate(String base, String target) {
        String url = "https://open.er-api.com/v6/latest/" + base;
        @SuppressWarnings("unchecked")
        Map<String, Object> resp = restTemplate.getForObject(url, Map.class);
        if (resp != null && resp.containsKey("rates")) {
            @SuppressWarnings("unchecked")
            Map<String, Object> rates = (Map<String, Object>) resp.get("rates");
            if (rates.containsKey(target)) {
                BigDecimal rate = new BigDecimal(rates.get(target).toString()).setScale(4, RoundingMode.HALF_UP);
                return new ExchangeRateDto(base, target, rate, DateUtils.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME), "RestTemplate");
            }
        }
        return getFallbackRate(base, target, "RestTemplate-Fallback");
    }

    private ExchangeRateDto fetchWithWebClient(String base, String target) {
        String url = "https://open.er-api.com/v6/latest/" + base;
        @SuppressWarnings("unchecked")
        Map<String, Object> resp = webClient.get()
                .uri(url)
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        if (resp != null && resp.containsKey("rates")) {
            @SuppressWarnings("unchecked")
            Map<String, Object> rates = (Map<String, Object>) resp.get("rates");
            if (rates.containsKey(target)) {
                BigDecimal rate = new BigDecimal(rates.get(target).toString()).setScale(4, RoundingMode.HALF_UP);
                return new ExchangeRateDto(base, target, rate, DateUtils.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME), "WebClient");
            }
        }
        return getFallbackRate(base, target, "WebClient-Fallback");
    }

    private ExchangeRateDto getFallbackRate(String base, String target, String clientUsed) {
        BigDecimal baseToInr = INR_BASELINES.getOrDefault(base, new BigDecimal("83.95"));
        BigDecimal targetToInr = INR_BASELINES.getOrDefault(target, new BigDecimal("83.95"));

        BigDecimal rate = baseToInr.divide(targetToInr, 4, RoundingMode.HALF_UP);
        return new ExchangeRateDto(base, target, rate, DateUtils.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME), clientUsed);
    }
}
