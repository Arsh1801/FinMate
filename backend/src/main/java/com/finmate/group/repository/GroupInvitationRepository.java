package com.finmate.group.repository;

import com.finmate.group.entity.GroupInvitation;
import com.finmate.group.entity.InvitationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GroupInvitationRepository extends JpaRepository<GroupInvitation, Long> {

    List<GroupInvitation> findByGroupIdAndStatusOrderByCreatedAtDesc(Long groupId, InvitationStatus status);

    List<GroupInvitation> findByGroupIdOrderByCreatedAtDesc(Long groupId);

    List<GroupInvitation> findByInvitedUserIdAndStatusOrderByCreatedAtDesc(Long userId, InvitationStatus status);

    @Query("SELECT i FROM GroupInvitation i WHERE i.group.id = :groupId AND i.invitedUser.id = :userId AND i.status = 'PENDING'")
    Optional<GroupInvitation> findPendingInvitation(@Param("groupId") Long groupId, @Param("userId") Long userId);
}
