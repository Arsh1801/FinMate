package com.finmate.transaction.controller;

import com.finmate.common.util.SecurityUtils;
import com.finmate.transaction.dto.CreateTransactionRequest;
import com.finmate.transaction.dto.TransactionDto;
import com.finmate.transaction.dto.TransactionPageResponse;
import com.finmate.transaction.dto.UpdateTransactionRequest;
import com.finmate.transaction.entity.PaymentMethod;
import com.finmate.transaction.entity.Transaction;
import com.finmate.transaction.entity.TransactionType;
import com.finmate.transaction.service.CsvExportService;
import com.finmate.transaction.service.TransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.net.URI;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/transactions")
@Tag(name = "Transactions", description = "Endpoints for personal income and expense transaction management")
public class TransactionController {

    private final TransactionService transactionService;
    private final CsvExportService csvExportService;

    public TransactionController(TransactionService transactionService, CsvExportService csvExportService) {
        this.transactionService = transactionService;
        this.csvExportService = csvExportService;
    }

    @GetMapping
    @Operation(summary = "Search, filter, sort, and paginate transactions with running totals")
    public ResponseEntity<TransactionPageResponse> getTransactions(
            @RequestParam(required = false) TransactionType type,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) PaymentMethod paymentMethod,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) BigDecimal minAmount,
            @RequestParam(required = false) BigDecimal maxAmount,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "date") String sort,
            @RequestParam(defaultValue = "desc") String dir,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Long userId = SecurityUtils.getCurrentUserId();
        TransactionPageResponse response = transactionService.searchTransactions(
                userId, type, categoryId, paymentMethod, from, to, minAmount, maxAmount, q, sort, dir, page, size);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get single transaction by ID")
    public ResponseEntity<TransactionDto> getTransaction(@PathVariable Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(transactionService.getTransaction(userId, id));
    }

    @PostMapping
    @Operation(summary = "Create a new income or expense transaction")
    public ResponseEntity<TransactionDto> createTransaction(
            @Valid @RequestBody CreateTransactionRequest request,
            @RequestParam(defaultValue = "false") boolean confirmDuplicate) {
        Long userId = SecurityUtils.getCurrentUserId();
        TransactionDto created = transactionService.createTransaction(userId, request, confirmDuplicate);
        return ResponseEntity.created(URI.create("/api/v1/transactions/" + created.getId())).body(created);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an existing transaction")
    public ResponseEntity<TransactionDto> updateTransaction(
            @PathVariable Long id,
            @Valid @RequestBody UpdateTransactionRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(transactionService.updateTransaction(userId, id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a transaction after confirmation")
    public ResponseEntity<Void> deleteTransaction(@PathVariable Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        transactionService.deleteTransaction(userId, id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/export")
    @Operation(summary = "Export filtered transactions as CSV")
    public ResponseEntity<byte[]> exportCsv(
            @RequestParam(required = false) TransactionType type,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) PaymentMethod paymentMethod,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) BigDecimal minAmount,
            @RequestParam(required = false) BigDecimal maxAmount,
            @RequestParam(required = false) String q) {
        Long userId = SecurityUtils.getCurrentUserId();
        List<Transaction> transactions = transactionService.getFilteredTransactionsList(
                userId, type, categoryId, paymentMethod, from, to, minAmount, maxAmount, q);
        String csv = csvExportService.generateCsv(transactions);

        byte[] output = csv.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"transactions.csv\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(output);
    }
}
