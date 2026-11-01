package com.finmate.common.seeder;

import com.finmate.budget.entity.Budget;
import com.finmate.budget.entity.BudgetAlertLevel;
import com.finmate.budget.entity.BudgetStatus;
import com.finmate.budget.repository.BudgetRepository;
import com.finmate.category.entity.Category;
import com.finmate.category.entity.CategoryType;
import com.finmate.category.repository.CategoryRepository;
import com.finmate.common.util.DateUtils;
import com.finmate.expense.entity.ExpenseSplit;
import com.finmate.expense.entity.SharedExpense;
import com.finmate.expense.entity.SplitType;
import com.finmate.expense.repository.ExpenseSplitRepository;
import com.finmate.expense.repository.SharedExpenseRepository;
import com.finmate.goal.entity.GoalContribution;
import com.finmate.goal.entity.GoalStatus;
import com.finmate.goal.entity.SavingsGoal;
import com.finmate.goal.repository.GoalContributionRepository;
import com.finmate.goal.repository.SavingsGoalRepository;
import com.finmate.group.entity.ExpenseGroup;
import com.finmate.group.entity.GroupMember;
import com.finmate.group.entity.GroupRole;
import com.finmate.group.entity.MemberStatus;
import com.finmate.group.repository.ExpenseGroupRepository;
import com.finmate.group.repository.GroupMemberRepository;
import com.finmate.transaction.entity.PaymentMethod;
import com.finmate.transaction.entity.Transaction;
import com.finmate.transaction.entity.TransactionType;
import com.finmate.transaction.repository.TransactionRepository;
import com.finmate.user.entity.Role;
import com.finmate.user.entity.User;
import com.finmate.user.entity.UserStatus;
import com.finmate.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;

@Component
public class DefaultDataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DefaultDataSeeder.class);

    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final TransactionRepository transactionRepository;
    private final BudgetRepository budgetRepository;
    private final SavingsGoalRepository goalRepository;
    private final GoalContributionRepository contributionRepository;
    private final ExpenseGroupRepository groupRepository;
    private final GroupMemberRepository memberRepository;
    private final SharedExpenseRepository expenseRepository;
    private final ExpenseSplitRepository splitRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${finmate.seeder.enabled:true}")
    private boolean seederEnabled;

    @Value("${finmate.admin.seed-enabled:false}")
    private boolean adminSeederEnabled;

    @Value("${finmate.admin.email:admin@finmate.local}")
    private String adminEmail;

    @Value("${finmate.admin.password}")
    private String adminPassword;

    @Value("${finmate.admin.name:System Admin}")
    private String adminName;

    public DefaultDataSeeder(UserRepository userRepository,
                             CategoryRepository categoryRepository,
                             TransactionRepository transactionRepository,
                             BudgetRepository budgetRepository,
                             SavingsGoalRepository goalRepository,
                             GoalContributionRepository contributionRepository,
                             ExpenseGroupRepository groupRepository,
                             GroupMemberRepository memberRepository,
                             SharedExpenseRepository expenseRepository,
                             ExpenseSplitRepository splitRepository,
                             PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.categoryRepository = categoryRepository;
        this.transactionRepository = transactionRepository;
        this.budgetRepository = budgetRepository;
        this.goalRepository = goalRepository;
        this.contributionRepository = contributionRepository;
        this.groupRepository = groupRepository;
        this.memberRepository = memberRepository;
        this.expenseRepository = expenseRepository;
        this.splitRepository = splitRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (categoryRepository.count() == 0) {
            log.info("Seeding system default categories...");
            seedSystemCategories();
        }

        if (adminSeederEnabled) {
            seedAdminUserIfMissing();
        }

        if (seederEnabled && userRepository.findByEmailIgnoreCase("demo@finmate.local").isEmpty()) {
            log.info("Seeding default demo users, transactions, budgets, goals, and shared groups...");
            seedUsersAndDemoData();
        }
    }

    private void seedAdminUserIfMissing() {
        if (userRepository.findByEmailIgnoreCase(adminEmail).isEmpty()) {
            log.info("Seeding system admin user: {}", adminEmail);
            User admin = new User();
            admin.setEmail(adminEmail.toLowerCase().trim());
            admin.setPasswordHash(passwordEncoder.encode(adminPassword));
            admin.setFullName(adminName);
            admin.setRole(Role.ADMIN);
            admin.setStatus(UserStatus.ACTIVE);
            admin.setCurrencyCode("INR");
            admin.setTheme("DARK");
            admin.setEmailNotifications(true);
            userRepository.save(admin);
        }
    }

    private void seedSystemCategories() {
        List<Category> categories = List.of(
                // Income
                createCat("Salary", CategoryType.INCOME, "Briefcase", "#10b981", true),
                createCat("Freelance", CategoryType.INCOME, "Laptop", "#06b6d4", true),
                createCat("Investments", CategoryType.INCOME, "TrendingUp", "#8b5cf6", true),
                createCat("Other Income", CategoryType.INCOME, "PlusCircle", "#64748b", true),

                // Expense
                createCat("Food & Dining", CategoryType.EXPENSE, "Utensils", "#ef4444", true),
                createCat("Groceries", CategoryType.EXPENSE, "ShoppingCart", "#f97316", true),
                createCat("Rent & Housing", CategoryType.EXPENSE, "Home", "#6366f1", true),
                createCat("Utilities", CategoryType.EXPENSE, "Zap", "#eab308", true),
                createCat("Transport", CategoryType.EXPENSE, "Car", "#3b82f6", true),
                createCat("Shopping", CategoryType.EXPENSE, "ShoppingBag", "#ec4899", true),
                createCat("Entertainment", CategoryType.EXPENSE, "Film", "#a855f7", true),
                createCat("Healthcare", CategoryType.EXPENSE, "Activity", "#14b8a6", true),
                createCat("Education", CategoryType.EXPENSE, "BookOpen", "#84cc16", true),
                createCat("Travel", CategoryType.EXPENSE, "Plane", "#f43f5e", true),
                createCat("Subscriptions", CategoryType.EXPENSE, "Repeat", "#0ea5e9", true),
                createCat("Personal Care", CategoryType.EXPENSE, "Smile", "#d946ef", true),
                createCat("Other Expense", CategoryType.EXPENSE, "MoreHorizontal", "#94a3b8", true)
        );
        categoryRepository.saveAll(categories);
    }

    private Category createCat(String name, CategoryType type, String icon, String color, boolean isSystem) {
        Category c = new Category();
        c.setName(name);
        c.setType(type);
        c.setIcon(icon);
        c.setColorHex(color);
        c.setIsSystem(isSystem);
        c.setIsActive(true);
        return c;
    }

    private void seedUsersAndDemoData() {
        // 1. Demo User: Riya Sharma
        User riya = new User();
        riya.setEmail("demo@finmate.local");
        riya.setPasswordHash(passwordEncoder.encode("DemoPassword123!"));
        riya.setFullName("Riya Sharma");
        riya.setRole(Role.USER);
        riya.setStatus(UserStatus.ACTIVE);
        riya.setCurrencyCode("INR");
        riya.setTheme("LIGHT");
        riya.setEmailNotifications(true);
        riya = userRepository.save(riya);

        // 2. Arjun Mehta
        User arjun = new User();
        arjun.setEmail("arjun@finmate.local");
        arjun.setPasswordHash(passwordEncoder.encode("DemoPassword123!"));
        arjun.setFullName("Arjun Mehta");
        arjun.setRole(Role.USER);
        arjun.setStatus(UserStatus.ACTIVE);
        arjun.setCurrencyCode("INR");
        arjun = userRepository.save(arjun);

        // 3. Sneha Patel
        User sneha = new User();
        sneha.setEmail("sneha@finmate.local");
        sneha.setPasswordHash(passwordEncoder.encode("DemoPassword123!"));
        sneha.setFullName("Sneha Patel");
        sneha.setRole(Role.USER);
        sneha.setStatus(UserStatus.ACTIVE);
        sneha.setCurrencyCode("INR");
        sneha = userRepository.save(sneha);

        Map<String, Category> catMap = new HashMap<>();
        categoryRepository.findAll().forEach(c -> catMap.put(c.getName(), c));

        // 4. Seed Transactions for Riya
        LocalDate today = DateUtils.today();
        YearMonth currentYm = YearMonth.from(today);

        // Past 3 months recurring & variable transactions
        for (int i = 2; i >= 0; i--) {
            YearMonth ym = currentYm.minusMonths(i);
            LocalDate salaryDate = ym.atDay(1);
            LocalDate rentDate = ym.atDay(3);
            LocalDate groceries1 = ym.atDay(7);
            LocalDate dining1 = ym.atDay(12);
            LocalDate transport = ym.atDay(15);
            LocalDate subscription = ym.atDay(18);
            LocalDate dining2 = ym.atDay(Math.min(22, ym.lengthOfMonth()));

            // Salary
            createTx(riya, TransactionType.INCOME, new BigDecimal("85000.00"), salaryDate,
                    catMap.get("Salary"), "Monthly Corporate Salary - TechCorp", PaymentMethod.NET_BANKING);

            // Rent
            createTx(riya, TransactionType.EXPENSE, new BigDecimal("22000.00"), rentDate,
                    catMap.get("Rent & Housing"), "Apartment Rent & Maintenance", PaymentMethod.NET_BANKING);

            // Groceries
            createTx(riya, TransactionType.EXPENSE, new BigDecimal("4500.00"), groceries1,
                    catMap.get("Groceries"), "Blinkit & Nature's Basket Essentials", PaymentMethod.UPI);

            // Dining
            createTx(riya, TransactionType.EXPENSE, new BigDecimal("2600.00"), dining1,
                    catMap.get("Food & Dining"), "Dinner with Friends at Toit", PaymentMethod.CREDIT_CARD);

            // Transport
            createTx(riya, TransactionType.EXPENSE, new BigDecimal("1850.00"), transport,
                    catMap.get("Transport"), "Uber & Metro Monthly Passes", PaymentMethod.UPI);

            // Subscription
            createTx(riya, TransactionType.EXPENSE, new BigDecimal("999.00"), subscription,
                    catMap.get("Subscriptions"), "Netflix & Spotify Premium", PaymentMethod.CREDIT_CARD);

            if (i > 0 || today.getDayOfMonth() >= 22) {
                createTx(riya, TransactionType.EXPENSE, new BigDecimal("2600.00"), dining2,
                        catMap.get("Food & Dining"), "Weekend Brunch & Cafe Work", PaymentMethod.UPI);
            }
        }

        // 5. Seed Budgets for current month (matches PRD worked example)
        // Food budget = ₹6000 (spent ~₹5200 => 86.7% CAUTION alert)
        Budget foodBudget = new Budget();
        foodBudget.setUser(riya);
        foodBudget.setCategory(catMap.get("Food & Dining"));
        foodBudget.setBudgetYear(currentYm.getYear());
        foodBudget.setBudgetMonth(currentYm.getMonthValue());
        foodBudget.setLimitAmount(new BigDecimal("6000.00"));
        foodBudget.setAlertThreshold(80);
        foodBudget.setLastAlertLevel(BudgetAlertLevel.WARNING);
        budgetRepository.save(foodBudget);

        Budget groceryBudget = new Budget();
        groceryBudget.setUser(riya);
        groceryBudget.setCategory(catMap.get("Groceries"));
        groceryBudget.setBudgetYear(currentYm.getYear());
        groceryBudget.setBudgetMonth(currentYm.getMonthValue());
        groceryBudget.setLimitAmount(new BigDecimal("8000.00"));
        groceryBudget.setAlertThreshold(80);
        groceryBudget.setLastAlertLevel(BudgetAlertLevel.NONE);
        budgetRepository.save(groceryBudget);

        Budget overallBudget = new Budget();
        overallBudget.setUser(riya);
        overallBudget.setCategory(null);
        overallBudget.setBudgetYear(currentYm.getYear());
        overallBudget.setBudgetMonth(currentYm.getMonthValue());
        overallBudget.setLimitAmount(new BigDecimal("50000.00"));
        overallBudget.setAlertThreshold(80);
        overallBudget.setLastAlertLevel(BudgetAlertLevel.NONE);
        budgetRepository.save(overallBudget);

        // 6. Seed Savings Goal (matches PRD Section 6.7 worked example)
        SavingsGoal laptopGoal = new SavingsGoal();
        laptopGoal.setUser(riya);
        laptopGoal.setName("New MacBook Pro M3");
        laptopGoal.setNote("Fund for upgrading developer workstation");
        laptopGoal.setTargetAmount(new BigDecimal("60000.00"));
        laptopGoal.setTargetDate(today.plusMonths(4));
        laptopGoal.setStatus(GoalStatus.ACTIVE);
        laptopGoal = goalRepository.save(laptopGoal);

        GoalContribution gc1 = new GoalContribution();
        gc1.setGoal(laptopGoal);
        gc1.setAmount(new BigDecimal("25000.00"));
        gc1.setContributionDate(today.minusMonths(2));
        gc1.setNote("Bonus allocation");
        contributionRepository.save(gc1);

        GoalContribution gc2 = new GoalContribution();
        gc2.setGoal(laptopGoal);
        gc2.setAmount(new BigDecimal("20000.00"));
        gc2.setContributionDate(today.minusMonths(1));
        gc2.setNote("Monthly savings transfer");
        contributionRepository.save(gc2);

        // 7. Seed Shared Group & Worked Example Split (PRD Section 6.4)
        ExpenseGroup group = new ExpenseGroup();
        group.setName("Goa Vacation 2024");
        group.setDescription("Beach villa, scooter rentals, dining and activities");
        group.setCreatedBy(riya);
        group = groupRepository.save(group);

        GroupMember gm1 = new GroupMember(group, riya, GroupRole.OWNER);
        GroupMember gm2 = new GroupMember(group, arjun, GroupRole.MEMBER);
        GroupMember gm3 = new GroupMember(group, sneha, GroupRole.MEMBER);
        memberRepository.saveAll(List.of(gm1, gm2, gm3));

        // PRD Section 6.4 Worked Example: ₹5000 Dinner paid by Riya, split equally among Riya, Arjun, Sneha
        SharedExpense dinner = new SharedExpense();
        dinner.setGroup(group);
        dinner.setDescription("Fisherman's Wharf Seafood Feast");
        dinner.setAmount(new BigDecimal("5000.00"));
        dinner.setPaidBy(riya);
        dinner.setExpenseDate(today.minusDays(5));
        dinner.setSplitType(SplitType.EQUAL);
        dinner.setNotes("Includes taxes and service charge");
        dinner.setCreatedBy(riya);
        dinner = expenseRepository.save(dinner);

        ExpenseSplit s1 = new ExpenseSplit(dinner, riya, new BigDecimal("1666.68"), new BigDecimal("33.34"));
        ExpenseSplit s2 = new ExpenseSplit(dinner, arjun, new BigDecimal("1666.66"), new BigDecimal("33.33"));
        ExpenseSplit s3 = new ExpenseSplit(dinner, sneha, new BigDecimal("1666.66"), new BigDecimal("33.33"));
        splitRepository.saveAll(List.of(s1, s2, s3));

        // Shared Expense 2: ₹3000 Car rental paid by Arjun, split equally
        SharedExpense cab = new SharedExpense();
        cab.setGroup(group);
        cab.setDescription("Airport Transfer & Day Cab");
        cab.setAmount(new BigDecimal("3000.00"));
        cab.setPaidBy(arjun);
        cab.setExpenseDate(today.minusDays(3));
        cab.setSplitType(SplitType.EQUAL);
        cab.setCreatedBy(arjun);
        cab = expenseRepository.save(cab);

        ExpenseSplit c1 = new ExpenseSplit(cab, riya, new BigDecimal("1000.00"), new BigDecimal("33.33"));
        ExpenseSplit c2 = new ExpenseSplit(cab, arjun, new BigDecimal("1000.00"), new BigDecimal("33.34"));
        ExpenseSplit c3 = new ExpenseSplit(cab, sneha, new BigDecimal("1000.00"), new BigDecimal("33.33"));
        splitRepository.saveAll(List.of(c1, c2, c3));

        log.info("Finished seeding initial FinMate dataset successfully!");
    }

    private void createTx(User user, TransactionType type, BigDecimal amount, LocalDate date,
                          Category category, String description, PaymentMethod method) {
        Transaction t = new Transaction();
        t.setUser(user);
        t.setType(type);
        t.setAmount(amount);
        t.setTransactionDate(date);
        t.setCategory(category);
        t.setDescription(description);
        t.setPaymentMethod(method);
        transactionRepository.save(t);
    }
}
