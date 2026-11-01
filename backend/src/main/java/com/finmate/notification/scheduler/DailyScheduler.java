package com.finmate.notification.scheduler;

import com.finmate.common.util.DateUtils;
import com.finmate.goal.entity.GoalStatus;
import com.finmate.goal.entity.SavingsGoal;
import com.finmate.goal.repository.GoalContributionRepository;
import com.finmate.goal.repository.SavingsGoalRepository;
import com.finmate.notification.entity.NotificationType;
import com.finmate.notification.service.NotificationService;
import com.finmate.transaction.service.RecurringRuleService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.time.temporal.IsoFields;
import java.util.List;

@Component
public class DailyScheduler {

    private static final Logger log = LoggerFactory.getLogger(DailyScheduler.class);

    private final RecurringRuleService recurringRuleService;
    private final SavingsGoalRepository goalRepository;
    private final GoalContributionRepository contributionRepository;
    private final NotificationService notificationService;

    public DailyScheduler(RecurringRuleService recurringRuleService,
                          SavingsGoalRepository goalRepository,
                          GoalContributionRepository contributionRepository,
                          NotificationService notificationService) {
        this.recurringRuleService = recurringRuleService;
        this.goalRepository = goalRepository;
        this.contributionRepository = contributionRepository;
        this.notificationService = notificationService;
    }

    @Scheduled(cron = "0 0 8 * * *", zone = "Asia/Kolkata")
    @Transactional
    public void runDailyJobs() {
        LocalDate today = DateUtils.today();
        log.info("Starting FinMate daily scheduled jobs for date: {}", today);

        // 1. Process due recurring transactions (FR-034, BR-015)
        int recurringProcessed = recurringRuleService.processDueRules(today);
        log.info("Processed {} recurring transactions", recurringProcessed);

        // 2. Evaluate savings goal reminders (FR-124)
        evaluateGoalReminders(today);
    }

    private void evaluateGoalReminders(LocalDate today) {
        List<SavingsGoal> activeGoals = goalRepository.findAll().stream()
                .filter(g -> g.getStatus() == GoalStatus.ACTIVE)
                .toList();

        int currentWeek = today.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR);
        int currentYear = today.getYear();

        for (SavingsGoal goal : activeGoals) {
            long daysLeft = ChronoUnit.DAYS.between(today, goal.getTargetDate());
            BigDecimal saved = contributionRepository.sumAmountByGoalId(goal.getId());
            BigDecimal target = goal.getTargetAmount();

            boolean targetNear = daysLeft > 0 && daysLeft <= 7 && saved.compareTo(target) < 0;

            long totalDays = ChronoUnit.DAYS.between(goal.getCreatedDate(), goal.getTargetDate());
            long elapsedDays = ChronoUnit.DAYS.between(goal.getCreatedDate(), today);
            boolean behindSchedule = false;

            if (totalDays > 0 && elapsedDays > 0 && elapsedDays < totalDays) {
                double expectedPct = ((double) elapsedDays / totalDays) * 100.0;
                double actualPct = saved.doubleValue() / target.doubleValue() * 100.0;
                if ((expectedPct - actualPct) > 10.0) {
                    behindSchedule = true;
                }
            }

            if (targetNear || behindSchedule) {
                String dedupeKey = "goal_rem_" + goal.getId() + "_" + currentYear + "_w" + currentWeek;
                String reason = targetNear ?
                        "Target date is in " + daysLeft + " days." :
                        "You are currently more than 10% behind your scheduled savings pace.";

                notificationService.createNotification(
                        goal.getUser().getId(),
                        NotificationType.GOAL_REMINDER,
                        "Savings Goal Reminder: " + goal.getName(),
                        reason + " Saved: " + saved + " / Target: " + target,
                        "GOAL",
                        goal.getId(),
                        dedupeKey
                );
            }
        }
    }
}
