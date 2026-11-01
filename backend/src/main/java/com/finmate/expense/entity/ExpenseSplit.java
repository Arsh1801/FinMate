package com.finmate.expense.entity;

import com.finmate.user.entity.User;
import jakarta.persistence.*;

import java.math.BigDecimal;

@Entity
@Table(name = "expense_splits", uniqueConstraints = {
        @UniqueConstraint(name = "uk_expense_user", columnNames = {"expense_id", "user_id"})
})
public class ExpenseSplit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "expense_id", nullable = false)
    private SharedExpense expense;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "share_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal shareAmount;

    @Column(name = "percentage", precision = 5, scale = 2)
    private BigDecimal percentage;

    public ExpenseSplit() {}

    public ExpenseSplit(SharedExpense expense, User user, BigDecimal shareAmount, BigDecimal percentage) {
        this.expense = expense;
        this.user = user;
        this.shareAmount = shareAmount;
        this.percentage = percentage;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public SharedExpense getExpense() {
        return expense;
    }

    public void setExpense(SharedExpense expense) {
        this.expense = expense;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public BigDecimal getShareAmount() {
        return shareAmount;
    }

    public void setShareAmount(BigDecimal shareAmount) {
        this.shareAmount = shareAmount;
    }

    public BigDecimal getPercentage() {
        return percentage;
    }

    public void setPercentage(BigDecimal percentage) {
        this.percentage = percentage;
    }
}
