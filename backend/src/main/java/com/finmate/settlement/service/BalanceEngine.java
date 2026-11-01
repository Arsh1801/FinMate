package com.finmate.settlement.service;

import com.finmate.common.exception.ApiException;
import com.finmate.common.exception.ErrorCode;
import com.finmate.common.util.MoneyUtils;
import com.finmate.expense.repository.ExpenseSplitRepository;
import com.finmate.expense.repository.SharedExpenseRepository;
import com.finmate.group.entity.GroupMember;
import com.finmate.group.repository.GroupMemberRepository;
import com.finmate.settlement.dto.GroupBalancesDto;
import com.finmate.settlement.dto.MemberBalanceDto;
import com.finmate.settlement.dto.SettlementSuggestionDto;
import com.finmate.settlement.repository.SettlementRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@Component
public class BalanceEngine {

    private static final Logger log = LoggerFactory.getLogger(BalanceEngine.class);

    private final GroupMemberRepository groupMemberRepository;
    private final SharedExpenseRepository sharedExpenseRepository;
    private final ExpenseSplitRepository expenseSplitRepository;
    private final SettlementRepository settlementRepository;

    public BalanceEngine(GroupMemberRepository groupMemberRepository,
                         SharedExpenseRepository sharedExpenseRepository,
                         ExpenseSplitRepository expenseSplitRepository,
                         SettlementRepository settlementRepository) {
        this.groupMemberRepository = groupMemberRepository;
        this.sharedExpenseRepository = sharedExpenseRepository;
        this.expenseSplitRepository = expenseSplitRepository;
        this.settlementRepository = settlementRepository;
    }

    public GroupBalancesDto computeGroupBalances(Long groupId) {
        List<GroupMember> members = groupMemberRepository.findActiveMembers(groupId);
        List<MemberBalanceDto> memberBalances = new ArrayList<>();

        long totalNetPaise = 0;
        BigDecimal totalGroupExpense = BigDecimal.ZERO;
        BigDecimal totalGroupSettled = BigDecimal.ZERO;

        for (GroupMember member : members) {
            Long userId = member.getUser().getId();
            BigDecimal paid = sharedExpenseRepository.sumPaidByMember(groupId, userId);
            BigDecimal owed = expenseSplitRepository.sumOwedByMember(groupId, userId);
            BigDecimal settledPaid = settlementRepository.sumConfirmedPaidByMember(groupId, userId);
            BigDecimal settledReceived = settlementRepository.sumConfirmedReceivedByMember(groupId, userId);

            long paidPaise = MoneyUtils.toPaise(paid);
            long owedPaise = MoneyUtils.toPaise(owed);
            long settledPaidPaise = MoneyUtils.toPaise(settledPaid);
            long settledReceivedPaise = MoneyUtils.toPaise(settledReceived);

            long netPaise = paidPaise - owedPaise + settledPaidPaise - settledReceivedPaise;
            totalNetPaise += netPaise;

            totalGroupExpense = totalGroupExpense.add(paid);
            totalGroupSettled = totalGroupSettled.add(settledPaid);

            BigDecimal net = MoneyUtils.fromPaise(netPaise);
            memberBalances.add(new MemberBalanceDto(
                    userId,
                    member.getUser().getFullName(),
                    member.getUser().getEmail(),
                    paid,
                    owed,
                    settledPaid,
                    settledReceived,
                    net
            ));
        }

        // Invariant check: sum of all members' net balances must equal 0.00
        if (totalNetPaise != 0) {
            log.error("GROUP_BALANCE_INVARIANT violated for group {}: sum of nets in paise is {}", groupId, totalNetPaise);
            throw new ApiException(
                    org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR,
                    ErrorCode.GROUP_BALANCE_INVARIANT,
                    "Group balance invariant violated: net balances do not sum to zero"
            );
        }

        BigDecimal netSum = MoneyUtils.fromPaise(totalNetPaise);
        return new GroupBalancesDto(groupId, memberBalances, totalGroupExpense, totalGroupSettled, netSum);
    }

    public List<SettlementSuggestionDto> computeSettlementSuggestions(Long groupId) {
        GroupBalancesDto balancesDto = computeGroupBalances(groupId);

        // Separate into debtors (net < 0) and creditors (net > 0)
        class Party {
            final Long userId;
            final String name;
            long amountPaise;

            Party(Long userId, String name, long amountPaise) {
                this.userId = userId;
                this.name = name;
                this.amountPaise = amountPaise;
            }
        }

        List<Party> debtors = new ArrayList<>();
        List<Party> creditors = new ArrayList<>();

        for (MemberBalanceDto mb : balancesDto.getMembers()) {
            long netPaise = MoneyUtils.toPaise(mb.getNetDecimal().abs());
            if (mb.getNetDecimal().compareTo(BigDecimal.ZERO) < 0) {
                debtors.add(new Party(mb.getUserId(), mb.getFullName(), netPaise));
            } else if (mb.getNetDecimal().compareTo(BigDecimal.ZERO) > 0) {
                creditors.add(new Party(mb.getUserId(), mb.getFullName(), netPaise));
            }
        }

        List<SettlementSuggestionDto> suggestions = new ArrayList<>();

        // Greedy minimization: repeatedly match largest debtor and largest creditor
        while (!debtors.isEmpty() && !creditors.isEmpty()) {
            debtors.sort((a, b) -> Long.compare(b.amountPaise, a.amountPaise));
            creditors.sort((a, b) -> Long.compare(b.amountPaise, a.amountPaise));

            Party debtor = debtors.get(0);
            Party creditor = creditors.get(0);

            long transferPaise = Math.min(debtor.amountPaise, creditor.amountPaise);
            BigDecimal transferAmount = MoneyUtils.fromPaise(transferPaise);

            suggestions.add(new SettlementSuggestionDto(
                    debtor.userId,
                    debtor.name,
                    creditor.userId,
                    creditor.name,
                    transferAmount
            ));

            debtor.amountPaise -= transferPaise;
            creditor.amountPaise -= transferPaise;

            if (debtor.amountPaise == 0) {
                debtors.remove(0);
            }
            if (creditor.amountPaise == 0) {
                creditors.remove(0);
            }
        }

        return suggestions;
    }
}
