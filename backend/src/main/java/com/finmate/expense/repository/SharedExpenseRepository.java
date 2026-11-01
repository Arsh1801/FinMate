package com.finmate.expense.repository;

import com.finmate.expense.entity.SharedExpense;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface SharedExpenseRepository extends JpaRepository<SharedExpense, Long> {

    @Query("SELECT se FROM SharedExpense se WHERE se.group.id = :groupId AND se.deletedAt IS NULL " +
           "AND (:from IS NULL OR se.expenseDate >= :from) " +
           "AND (:to IS NULL OR se.expenseDate <= :to) " +
           "AND (:paidBy IS NULL OR se.paidBy.id = :paidBy) " +
           "AND (:q IS NULL OR LOWER(se.description) LIKE LOWER(CONCAT('%', :q, '%'))) " +
           "ORDER BY se.expenseDate DESC, se.id DESC")
    Page<SharedExpense> searchActiveGroupExpenses(@Param("groupId") Long groupId,
                                                  @Param("from") LocalDate from,
                                                  @Param("to") LocalDate to,
                                                  @Param("paidBy") Long paidBy,
                                                  @Param("q") String q,
                                                  Pageable pageable);

    @Query("SELECT se FROM SharedExpense se WHERE se.group.id = :groupId AND se.deletedAt IS NULL")
    List<SharedExpense> findActiveByGroupId(@Param("groupId") Long groupId);

    @Query("SELECT se FROM SharedExpense se WHERE se.group.id = :groupId ORDER BY se.createdAt DESC")
    List<SharedExpense> findAllByGroupId(@Param("groupId") Long groupId);

    @Query("SELECT COALESCE(SUM(se.amount), 0) FROM SharedExpense se " +
           "WHERE se.group.id = :groupId AND se.paidBy.id = :userId AND se.deletedAt IS NULL")
    BigDecimal sumPaidByMember(@Param("groupId") Long groupId, @Param("userId") Long userId);
}
