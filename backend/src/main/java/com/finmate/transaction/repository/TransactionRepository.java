package com.finmate.transaction.repository;

import com.finmate.transaction.entity.Transaction;
import com.finmate.transaction.entity.TransactionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long>, JpaSpecificationExecutor<Transaction> {

    @Query("SELECT t FROM Transaction t WHERE " +
           "t.user.id = :userId AND " +
           "t.type = :type AND " +
           "t.amount = :amount AND " +
           "t.transactionDate = :date AND " +
           "t.category.id = :categoryId AND " +
           "LOWER(t.description) = LOWER(:description) AND " +
           "t.createdAt >= :afterTime")
    List<Transaction> findSuspectedDuplicates(@Param("userId") Long userId,
                                              @Param("type") TransactionType type,
                                              @Param("amount") BigDecimal amount,
                                              @Param("date") LocalDate date,
                                              @Param("categoryId") Long categoryId,
                                              @Param("description") String description,
                                              @Param("afterTime") LocalDateTime afterTime);

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t WHERE " +
           "t.user.id = :userId AND t.type = :type AND t.transactionDate <= :asOfDate")
    BigDecimal sumAmountByUserAndTypeDatedBeforeOrOn(@Param("userId") Long userId,
                                                     @Param("type") TransactionType type,
                                                     @Param("asOfDate") LocalDate asOfDate);

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t WHERE " +
           "t.user.id = :userId AND t.type = 'EXPENSE' AND " +
           "(:categoryId IS NULL OR t.category.id = :categoryId) AND " +
           "t.transactionDate >= :startDate AND t.transactionDate <= :endDate")
    BigDecimal sumExpenseInPeriod(@Param("userId") Long userId,
                                  @Param("categoryId") Long categoryId,
                                  @Param("startDate") LocalDate startDate,
                                  @Param("endDate") LocalDate endDate);

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t WHERE " +
           "t.user.id = :userId AND t.type = :type AND " +
           "t.transactionDate >= :startDate AND t.transactionDate <= :endDate")
    BigDecimal sumByTypeInPeriod(@Param("userId") Long userId,
                                 @Param("type") TransactionType type,
                                 @Param("startDate") LocalDate startDate,
                                 @Param("endDate") LocalDate endDate);

    @Query("SELECT t FROM Transaction t WHERE t.user.id = :userId ORDER BY t.transactionDate DESC, t.id DESC")
    List<Transaction> findRecentByUser(@Param("userId") Long userId);

    List<Transaction> findByUserIdAndTransactionDateBetween(Long userId, LocalDate startDate, LocalDate endDate);

    List<Transaction> findByUserIdAndTransactionDateBetweenOrderByTransactionDateDesc(Long userId, LocalDate startDate, LocalDate endDate);

    long countByUserId(Long userId);

    boolean existsByCategoryId(Long categoryId);
}
