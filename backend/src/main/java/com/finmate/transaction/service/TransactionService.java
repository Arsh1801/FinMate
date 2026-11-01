package com.finmate.transaction.service;

import com.finmate.category.entity.Category;
import com.finmate.category.entity.CategoryType;
import com.finmate.category.service.CategoryService;
import com.finmate.common.exception.ApiException;
import com.finmate.common.exception.ErrorCode;
import com.finmate.common.util.DateUtils;
import com.finmate.common.util.MoneyUtils;
import com.finmate.notification.event.TransactionChangedEvent;
import com.finmate.transaction.dto.*;
import com.finmate.transaction.entity.*;
import com.finmate.transaction.repository.TagRepository;
import com.finmate.transaction.repository.TransactionRepository;
import com.finmate.user.entity.User;
import com.finmate.user.repository.UserRepository;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final CategoryService categoryService;
    private final TagRepository tagRepository;
    private final ApplicationEventPublisher eventPublisher;

    public TransactionService(TransactionRepository transactionRepository,
                              UserRepository userRepository,
                              CategoryService categoryService,
                              TagRepository tagRepository,
                              ApplicationEventPublisher eventPublisher) {
        this.transactionRepository = transactionRepository;
        this.userRepository = userRepository;
        this.categoryService = categoryService;
        this.tagRepository = tagRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public TransactionDto createTransaction(Long userId, CreateTransactionRequest request, boolean confirmDuplicate) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> ApiException.notFound("User not found"));

        DateUtils.validateTransactionDate(request.getTransactionDate());
        BigDecimal amount = MoneyUtils.validateAndScale(request.getAmount());

        // Validate category
        CategoryType expectedType = request.getType() == TransactionType.INCOME ? CategoryType.INCOME : CategoryType.EXPENSE;
        Category category = categoryService.getCategoryForUserAndType(request.getCategoryId(), userId, expectedType);

        // Payment method validation
        PaymentMethod paymentMethod = request.getPaymentMethod();
        if (request.getType() == TransactionType.EXPENSE && paymentMethod == null) {
            throw ApiException.badRequest(ErrorCode.VALIDATION_FAILED, "Payment method is required for expense transactions");
        }
        if (paymentMethod == null) {
            paymentMethod = PaymentMethod.OTHER;
        }

        // Duplicate check (BR-014): same user, type, amount, date, category and description within last 2 minutes
        if (!confirmDuplicate) {
            LocalDateTime twoMinutesAgo = LocalDateTime.now().minusMinutes(2);
            List<Transaction> duplicates = transactionRepository.findSuspectedDuplicates(
                    userId, request.getType(), amount, request.getTransactionDate(),
                    category.getId(), request.getDescription().trim(), twoMinutesAgo);
            if (!duplicates.isEmpty()) {
                throw ApiException.conflict(ErrorCode.DUPLICATE_SUSPECTED,
                        "A similar transaction was recorded in the last 2 minutes. Please confirm to proceed.");
            }
        }

        Transaction transaction = new Transaction();
        transaction.setUser(user);
        transaction.setCategory(category);
        transaction.setType(request.getType());
        transaction.setAmount(amount);
        transaction.setTransactionDate(request.getTransactionDate());
        transaction.setDescription(request.getDescription().trim());
        transaction.setNotes(request.getNotes());
        transaction.setPaymentMethod(paymentMethod);

        // Handle tags
        if (request.getTags() != null && !request.getTags().isEmpty()) {
            Set<Tag> tags = resolveTags(user, request.getTags());
            transaction.setTags(tags);
        }

        Transaction saved = transactionRepository.save(transaction);

        // Publish event for budget alert recalculation
        eventPublisher.publishEvent(TransactionChangedEvent.from(saved));

        return TransactionDto.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public TransactionDto getTransaction(Long userId, Long transactionId) {
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> ApiException.notFound("Transaction not found"));

        // BR-012: access to another user's transaction returns 404 (not 403)
        if (!transaction.getUser().getId().equals(userId)) {
            throw ApiException.notFound("Transaction not found");
        }

        return TransactionDto.fromEntity(transaction);
    }

    @Transactional
    public TransactionDto updateTransaction(Long userId, Long transactionId, UpdateTransactionRequest request) {
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> ApiException.notFound("Transaction not found"));

        if (!transaction.getUser().getId().equals(userId)) {
            throw ApiException.notFound("Transaction not found");
        }

        // Optimistic locking check
        if (request.getVersion() != null && !transaction.getVersion().equals(request.getVersion())) {
            throw ApiException.conflict(ErrorCode.CONCURRENT_MODIFICATION,
                    "Transaction has been modified by another request. Please refresh and retry.");
        }

        DateUtils.validateTransactionDate(request.getTransactionDate());
        BigDecimal amount = MoneyUtils.validateAndScale(request.getAmount());

        CategoryType expectedType = transaction.getType() == TransactionType.INCOME ? CategoryType.INCOME : CategoryType.EXPENSE;
        Category category = categoryService.getCategoryForUserAndType(request.getCategoryId(), userId, expectedType);

        PaymentMethod paymentMethod = request.getPaymentMethod();
        if (transaction.getType() == TransactionType.EXPENSE && paymentMethod == null) {
            throw ApiException.badRequest(ErrorCode.VALIDATION_FAILED, "Payment method is required for expense transactions");
        }
        if (paymentMethod == null) {
            paymentMethod = PaymentMethod.OTHER;
        }

        LocalDate oldDate = transaction.getTransactionDate();
        Long oldCatId = transaction.getCategory().getId();

        transaction.setAmount(amount);
        transaction.setCategory(category);
        transaction.setTransactionDate(request.getTransactionDate());
        transaction.setDescription(request.getDescription().trim());
        transaction.setNotes(request.getNotes());
        transaction.setPaymentMethod(paymentMethod);

        if (request.getTags() != null) {
            Set<Tag> tags = resolveTags(transaction.getUser(), request.getTags());
            transaction.setTags(tags);
        }

        Transaction saved = transactionRepository.save(transaction);

        // Publish event for both old and new dates/categories to recalculate budgets correctly
        eventPublisher.publishEvent(TransactionChangedEvent.from(saved));
        if (!oldDate.equals(saved.getTransactionDate()) || !oldCatId.equals(saved.getCategory().getId())) {
            eventPublisher.publishEvent(new TransactionChangedEvent(
                    userId, oldCatId, oldDate, saved.getType() == TransactionType.EXPENSE));
        }

        return TransactionDto.fromEntity(saved);
    }

    @Transactional
    public void deleteTransaction(Long userId, Long transactionId) {
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> ApiException.notFound("Transaction not found"));

        if (!transaction.getUser().getId().equals(userId)) {
            throw ApiException.notFound("Transaction not found");
        }

        TransactionChangedEvent event = TransactionChangedEvent.from(transaction);
        transactionRepository.delete(transaction);

        // Publish event to recalculate budget and lower alert level if applicable (BR-013)
        eventPublisher.publishEvent(event);
    }

    @Transactional(readOnly = true)
    public TransactionPageResponse searchTransactions(Long userId,
                                                      TransactionType type,
                                                      Long categoryId,
                                                      PaymentMethod paymentMethod,
                                                      LocalDate from,
                                                      LocalDate to,
                                                      BigDecimal minAmount,
                                                      BigDecimal maxAmount,
                                                      String q,
                                                      String sortField,
                                                      String sortDir,
                                                      int page,
                                                      int size) {
        if (from != null && to != null && from.isAfter(to)) {
            throw ApiException.badRequest(ErrorCode.VALIDATION_FAILED, "'from' date cannot be after 'to' date");
        }
        if (minAmount != null && maxAmount != null && minAmount.compareTo(maxAmount) > 0) {
            throw ApiException.badRequest(ErrorCode.VALIDATION_FAILED, "'minAmount' cannot be greater than 'maxAmount'");
        }
        if (size <= 0 || size > 100) {
            throw ApiException.badRequest(ErrorCode.VALIDATION_FAILED, "Page size must be between 1 and 100");
        }
        if (page < 0) {
            throw ApiException.badRequest(ErrorCode.VALIDATION_FAILED, "Page number cannot be negative");
        }

        Specification<Transaction> spec = buildSpecification(userId, type, categoryId, paymentMethod, from, to, minAmount, maxAmount, q);

        // Sorting whitelist: date, amount, category
        Sort sort;
        Sort.Direction direction = "ASC".equalsIgnoreCase(sortDir) ? Sort.Direction.ASC : Sort.Direction.DESC;
        if ("amount".equalsIgnoreCase(sortField)) {
            sort = Sort.by(direction, "amount").and(Sort.by(Sort.Direction.DESC, "id"));
        } else if ("category".equalsIgnoreCase(sortField)) {
            sort = Sort.by(direction, "category.name").and(Sort.by(Sort.Direction.DESC, "id"));
        } else {
            // Default sort: date desc, id desc (FR-029)
            sort = Sort.by(direction, "transactionDate").and(Sort.by(Sort.Direction.DESC, "id"));
        }

        Pageable pageable = PageRequest.of(page, size, sort);
        Page<Transaction> pageResult = transactionRepository.findAll(spec, pageable);

        // Calculate running totals for the entire filtered set (FR-031)
        List<Transaction> allFiltered = transactionRepository.findAll(spec);
        BigDecimal incomeTotal = BigDecimal.ZERO;
        BigDecimal expenseTotal = BigDecimal.ZERO;
        for (Transaction t : allFiltered) {
            if (t.getType() == TransactionType.INCOME) {
                incomeTotal = incomeTotal.add(t.getAmount());
            } else {
                expenseTotal = expenseTotal.add(t.getAmount());
            }
        }
        BigDecimal netTotal = incomeTotal.subtract(expenseTotal);
        FilteredTotalsDto totals = new FilteredTotalsDto(incomeTotal, expenseTotal, netTotal);

        List<TransactionDto> dtos = pageResult.getContent().stream()
                .map(TransactionDto::fromEntity)
                .collect(Collectors.toList());

        return new TransactionPageResponse(
                dtos,
                pageResult.getNumber(),
                pageResult.getSize(),
                pageResult.getTotalElements(),
                pageResult.getTotalPages(),
                totals
        );
    }

    @Transactional(readOnly = true)
    public List<Transaction> getFilteredTransactionsList(Long userId,
                                                         TransactionType type,
                                                         Long categoryId,
                                                         PaymentMethod paymentMethod,
                                                         LocalDate from,
                                                         LocalDate to,
                                                         BigDecimal minAmount,
                                                         BigDecimal maxAmount,
                                                         String q) {
        Specification<Transaction> spec = buildSpecification(userId, type, categoryId, paymentMethod, from, to, minAmount, maxAmount, q);
        Sort sort = Sort.by(Sort.Direction.DESC, "transactionDate").and(Sort.by(Sort.Direction.DESC, "id"));
        return transactionRepository.findAll(spec, sort);
    }

    private Specification<Transaction> buildSpecification(Long userId,
                                                          TransactionType type,
                                                          Long categoryId,
                                                          PaymentMethod paymentMethod,
                                                          LocalDate from,
                                                          LocalDate to,
                                                          BigDecimal minAmount,
                                                          BigDecimal maxAmount,
                                                          String q) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("user").get("id"), userId));

            if (type != null) {
                predicates.add(cb.equal(root.get("type"), type));
            }
            if (categoryId != null) {
                predicates.add(cb.equal(root.get("category").get("id"), categoryId));
            }
            if (paymentMethod != null) {
                predicates.add(cb.equal(root.get("paymentMethod"), paymentMethod));
            }
            if (from != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("transactionDate"), from));
            }
            if (to != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("transactionDate"), to));
            }
            if (minAmount != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("amount"), minAmount));
            }
            if (maxAmount != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("amount"), maxAmount));
            }
            if (q != null && !q.trim().isEmpty()) {
                String searchLike = "%" + q.trim().toLowerCase() + "%";
                Predicate descPredicate = cb.like(cb.lower(root.get("description")), searchLike);
                Predicate notesPredicate = cb.like(cb.lower(root.get("notes")), searchLike);
                predicates.add(cb.or(descPredicate, notesPredicate));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private Set<Tag> resolveTags(User user, List<String> tagNames) {
        Set<Tag> tags = new HashSet<>();
        for (String name : tagNames) {
            String trimmed = name.trim();
            if (!trimmed.isEmpty()) {
                Tag tag = tagRepository.findByUserIdAndNameIgnoreCase(user.getId(), trimmed)
                        .orElseGet(() -> tagRepository.save(new Tag(user, trimmed)));
                tags.add(tag);
            }
        }
        return tags;
    }
}
