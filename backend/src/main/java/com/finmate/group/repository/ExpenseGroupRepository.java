package com.finmate.group.repository;

import com.finmate.group.entity.ExpenseGroup;
import com.finmate.group.entity.GroupStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ExpenseGroupRepository extends JpaRepository<ExpenseGroup, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT g FROM ExpenseGroup g WHERE g.id = :id")
    Optional<ExpenseGroup> findByIdWithLock(@Param("id") Long id);

    @Query("SELECT DISTINCT g FROM ExpenseGroup g JOIN g.members m WHERE m.user.id = :userId AND " +
           "(:status IS NULL OR g.status = :status) ORDER BY g.createdAt DESC")
    List<ExpenseGroup> findUserGroups(@Param("userId") Long userId, @Param("status") GroupStatus status);

    @Query("SELECT g FROM ExpenseGroup g WHERE g.createdBy.id = :userId AND LOWER(g.name) = LOWER(:name) AND g.status = 'ACTIVE'")
    Optional<ExpenseGroup> findActiveByOwnerAndName(@Param("userId") Long userId, @Param("name") String name);

    default List<ExpenseGroup> findActiveGroupsByUserId(Long userId) {
        return findUserGroups(userId, GroupStatus.ACTIVE);
    }
}
