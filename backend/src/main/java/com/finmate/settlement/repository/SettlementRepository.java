package com.finmate.settlement.repository;

import com.finmate.settlement.entity.Settlement;
import com.finmate.settlement.entity.SettlementStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SettlementRepository extends JpaRepository<Settlement, Long> {

    @Query("SELECT s FROM Settlement s WHERE s.group.id = :groupId AND " +
           "(:status IS NULL OR s.status = :status) ORDER BY s.settlementDate DESC, s.id DESC")
    Page<Settlement> findByGroupIdAndStatus(@Param("groupId") Long groupId,
                                            @Param("status") SettlementStatus status,
                                            Pageable pageable);

    @Query("SELECT s FROM Settlement s WHERE s.group.id = :groupId ORDER BY s.settlementDate DESC, s.id DESC")
    List<Settlement> findByGroupIdOrderByDateDesc(@Param("groupId") Long groupId);

    @Query("SELECT COUNT(s) > 0 FROM Settlement s WHERE s.group.id = :groupId " +
           "AND s.status = 'CONFIRMED' AND s.confirmedAt > :expenseCreatedAt")
    boolean existsConfirmedAfter(@Param("groupId") Long groupId, @Param("expenseCreatedAt") LocalDateTime expenseCreatedAt);

    @Query("SELECT COALESCE(SUM(s.amount), 0) FROM Settlement s " +
           "WHERE s.group.id = :groupId AND s.fromUser.id = :userId AND s.status = 'CONFIRMED'")
    BigDecimal sumConfirmedPaidByMember(@Param("groupId") Long groupId, @Param("userId") Long userId);

    @Query("SELECT COALESCE(SUM(s.amount), 0) FROM Settlement s " +
           "WHERE s.group.id = :groupId AND s.toUser.id = :userId AND s.status = 'CONFIRMED'")
    BigDecimal sumConfirmedReceivedByMember(@Param("groupId") Long groupId, @Param("userId") Long userId);

    @Query("SELECT COALESCE(SUM(s.amount), 0) FROM Settlement s " +
           "WHERE s.group.id = :groupId AND s.fromUser.id = :fromUserId AND s.toUser.id = :toUserId AND s.status = 'PENDING'")
    BigDecimal sumPendingBetween(@Param("groupId") Long groupId,
                                 @Param("fromUserId") Long fromUserId,
                                 @Param("toUserId") Long toUserId);

    @Query("SELECT s FROM Settlement s WHERE s.toUser.id = :userId AND s.status = 'PENDING' ORDER BY s.createdAt DESC")
    List<Settlement> findPendingForReceiver(@Param("userId") Long userId);

    @Query("SELECT COUNT(s) > 0 FROM Settlement s WHERE s.group.id = :groupId AND " +
           "(s.fromUser.id = :userId OR s.toUser.id = :userId) AND s.status = 'PENDING'")
    boolean hasPendingSettlementForUser(@Param("groupId") Long groupId, @Param("userId") Long userId);
}
