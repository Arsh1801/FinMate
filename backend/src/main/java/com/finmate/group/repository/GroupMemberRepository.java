package com.finmate.group.repository;

import com.finmate.group.entity.GroupMember;
import com.finmate.group.entity.MemberStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GroupMemberRepository extends JpaRepository<GroupMember, Long> {

    Optional<GroupMember> findByGroupIdAndUserId(Long groupId, Long userId);

    @Query("SELECT gm FROM GroupMember gm WHERE gm.group.id = :groupId AND gm.user.id = :userId AND gm.status = 'ACTIVE'")
    Optional<GroupMember> findActiveMember(@Param("groupId") Long groupId, @Param("userId") Long userId);

    List<GroupMember> findByGroupIdOrderByJoinedAtAsc(Long groupId);

    @Query("SELECT gm FROM GroupMember gm WHERE gm.group.id = :groupId AND gm.status = 'ACTIVE' ORDER BY gm.joinedAt ASC")
    List<GroupMember> findActiveMembers(@Param("groupId") Long groupId);

    long countByGroupIdAndStatus(Long groupId, MemberStatus status);
}
