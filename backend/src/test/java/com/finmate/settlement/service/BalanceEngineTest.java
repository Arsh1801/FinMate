package com.finmate.settlement.service;

import com.finmate.expense.repository.ExpenseSplitRepository;
import com.finmate.expense.repository.SharedExpenseRepository;
import com.finmate.group.entity.ExpenseGroup;
import com.finmate.group.entity.GroupMember;
import com.finmate.group.entity.GroupRole;
import com.finmate.group.repository.GroupMemberRepository;
import com.finmate.settlement.dto.GroupBalancesDto;
import com.finmate.settlement.dto.MemberBalanceDto;
import com.finmate.settlement.dto.SettlementSuggestionDto;
import com.finmate.settlement.repository.SettlementRepository;
import com.finmate.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class BalanceEngineTest {

    private BalanceEngine balanceEngine;
    private final Long groupId = 100L;
    private User userRiya;
    private User userArjun;
    private User userSneha;

    @BeforeEach
    void setUp() {
        userRiya = new User();
        userRiya.setId(1L);
        userRiya.setFullName("Riya Sharma");
        userRiya.setEmail("riya@finmate.local");

        userArjun = new User();
        userArjun.setId(2L);
        userArjun.setFullName("Arjun Mehta");
        userArjun.setEmail("arjun@finmate.local");

        userSneha = new User();
        userSneha.setId(3L);
        userSneha.setFullName("Sneha Patel");
        userSneha.setEmail("sneha@finmate.local");

        ExpenseGroup group = new ExpenseGroup();
        group.setId(groupId);

        GroupMember gm1 = new GroupMember(group, userRiya, GroupRole.OWNER);
        GroupMember gm2 = new GroupMember(group, userArjun, GroupRole.MEMBER);
        GroupMember gm3 = new GroupMember(group, userSneha, GroupRole.MEMBER);

        // Dynamic proxy for GroupMemberRepository
        GroupMemberRepository memberRepo = (GroupMemberRepository) Proxy.newProxyInstance(
                GroupMemberRepository.class.getClassLoader(),
                new Class[]{GroupMemberRepository.class},
                (proxy, method, args) -> {
                    if ("findActiveMembers".equals(method.getName())) {
                        return List.of(gm1, gm2, gm3);
                    }
                    return null;
                }
        );

        // Dynamic proxy for SharedExpenseRepository
        Map<Long, BigDecimal> paidMap = Map.of(
                1L, new BigDecimal("5000.00"),
                2L, new BigDecimal("3000.00"),
                3L, BigDecimal.ZERO
        );
        SharedExpenseRepository expenseRepo = (SharedExpenseRepository) Proxy.newProxyInstance(
                SharedExpenseRepository.class.getClassLoader(),
                new Class[]{SharedExpenseRepository.class},
                (proxy, method, args) -> {
                    if ("sumPaidByMember".equals(method.getName())) {
                        Long uId = (Long) args[1];
                        return paidMap.getOrDefault(uId, BigDecimal.ZERO);
                    }
                    return BigDecimal.ZERO;
                }
        );

        // Dynamic proxy for ExpenseSplitRepository
        Map<Long, BigDecimal> owedMap = Map.of(
                1L, new BigDecimal("2666.67"),
                2L, new BigDecimal("2666.67"),
                3L, new BigDecimal("2666.66")
        );
        ExpenseSplitRepository splitRepo = (ExpenseSplitRepository) Proxy.newProxyInstance(
                ExpenseSplitRepository.class.getClassLoader(),
                new Class[]{ExpenseSplitRepository.class},
                (proxy, method, args) -> {
                    if ("sumOwedByMember".equals(method.getName())) {
                        Long uId = (Long) args[1];
                        return owedMap.getOrDefault(uId, BigDecimal.ZERO);
                    }
                    return BigDecimal.ZERO;
                }
        );

        // Dynamic proxy for SettlementRepository
        SettlementRepository settlementRepo = (SettlementRepository) Proxy.newProxyInstance(
                SettlementRepository.class.getClassLoader(),
                new Class[]{SettlementRepository.class},
                (proxy, method, args) -> BigDecimal.ZERO
        );

        balanceEngine = new BalanceEngine(memberRepo, expenseRepo, splitRepo, settlementRepo);
    }

    @Test
    @DisplayName("Group Balance Invariant & Settlement Suggestions with Greedy Debt Minimization")
    void testGroupBalancesAndSettlementSuggestions() {
        // Execute
        GroupBalancesDto balances = balanceEngine.computeGroupBalances(groupId);

        // Verify Invariant: Net balance sum MUST be 0.00
        assertEquals("0.00", balances.getNetSum());
        assertEquals(3, balances.getMembers().size());

        MemberBalanceDto riyaBal = balances.getMembers().stream().filter(m -> m.getUserId().equals(1L)).findFirst().orElseThrow();
        MemberBalanceDto arjunBal = balances.getMembers().stream().filter(m -> m.getUserId().equals(2L)).findFirst().orElseThrow();
        MemberBalanceDto snehaBal = balances.getMembers().stream().filter(m -> m.getUserId().equals(3L)).findFirst().orElseThrow();

        assertEquals(new BigDecimal("2333.33"), riyaBal.getNetDecimal());
        assertEquals(new BigDecimal("333.33"), arjunBal.getNetDecimal());
        assertEquals(new BigDecimal("-2666.66"), snehaBal.getNetDecimal());

        // Suggestions test
        List<SettlementSuggestionDto> suggestions = balanceEngine.computeSettlementSuggestions(groupId);
        assertNotNull(suggestions);
        assertEquals(2, suggestions.size());

        // Transfer 1: Sneha pays Riya 2333.33
        assertEquals(3L, suggestions.get(0).getFromUserId());
        assertEquals(1L, suggestions.get(0).getToUserId());
        assertEquals("2333.33", suggestions.get(0).getAmount());

        // Transfer 2: Sneha pays Arjun 333.33
        assertEquals(3L, suggestions.get(1).getFromUserId());
        assertEquals(2L, suggestions.get(1).getToUserId());
        assertEquals("333.33", suggestions.get(1).getAmount());
    }
}
