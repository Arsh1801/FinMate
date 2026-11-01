package com.finmate.dashboard.controller;

import com.finmate.dashboard.dto.ExchangeRateDto;
import com.finmate.dashboard.service.ExchangeRateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/external")
@Tag(name = "External Integrations", description = "Live third-party currency conversion and exchange rates")
public class ExternalApiController {

    private final ExchangeRateService exchangeRateService;

    public ExternalApiController(ExchangeRateService exchangeRateService) {
        this.exchangeRateService = exchangeRateService;
    }

    @GetMapping("/exchange-rate")
    @Operation(summary = "Query live currency exchange rate with RestTemplate / WebClient")
    public ResponseEntity<ExchangeRateDto> getExchangeRate(
            @RequestParam(name = "base", defaultValue = "USD") String base,
            @RequestParam(name = "target", defaultValue = "INR") String target,
            @RequestParam(name = "useWebClient", defaultValue = "false") boolean useWebClient) {
        return ResponseEntity.ok(exchangeRateService.getRate(base, target, useWebClient));
    }
}
