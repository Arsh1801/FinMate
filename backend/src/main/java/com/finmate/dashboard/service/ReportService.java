package com.finmate.dashboard.service;

import com.finmate.budget.dto.BudgetDto;
import com.finmate.budget.service.BudgetService;
import com.finmate.common.exception.ApiException;
import com.finmate.common.exception.ErrorCode;
import com.finmate.common.util.DateUtils;
import com.finmate.common.util.MoneyUtils;
import com.finmate.dashboard.dto.MonthlyReportDto;
import com.finmate.goal.dto.SavingsGoalDto;
import com.finmate.goal.service.GoalService;
import com.finmate.transaction.entity.Transaction;
import com.finmate.transaction.entity.TransactionType;
import com.finmate.transaction.repository.TransactionRepository;
import com.finmate.user.entity.User;
import com.finmate.user.repository.UserRepository;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class ReportService {

    private final TransactionRepository transactionRepository;
    private final BudgetService budgetService;
    private final GoalService goalService;
    private final UserRepository userRepository;

    public ReportService(TransactionRepository transactionRepository,
                         BudgetService budgetService,
                         GoalService goalService,
                         UserRepository userRepository) {
        this.transactionRepository = transactionRepository;
        this.budgetService = budgetService;
        this.goalService = goalService;
        this.userRepository = userRepository;
    }

    public MonthlyReportDto generateMonthlyReport(Long userId, String monthStr) {
        YearMonth ym = (monthStr != null && !monthStr.isBlank()) ? YearMonth.parse(monthStr) : YearMonth.from(DateUtils.today());
        LocalDate start = ym.atDay(1);
        LocalDate end = ym.atEndOfMonth();

        BigDecimal income = transactionRepository.sumByTypeInPeriod(userId, TransactionType.INCOME, start, end);
        BigDecimal expense = transactionRepository.sumByTypeInPeriod(userId, TransactionType.EXPENSE, start, end);
        BigDecimal netSavings = income.subtract(expense);

        BigDecimal savingsRate = BigDecimal.ZERO;
        if (income.compareTo(BigDecimal.ZERO) > 0) {
            savingsRate = netSavings.divide(income, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100)).setScale(2, RoundingMode.HALF_UP);
        }

        // Category breakdown
        List<Transaction> transactions = transactionRepository.findByUserIdAndTransactionDateBetween(userId, start, end);
        Map<String, BigDecimal> catTotals = new HashMap<>();
        Map<String, String> catTypes = new HashMap<>();

        for (Transaction t : transactions) {
            if (t.getType() == TransactionType.EXPENSE) {
                String cName = t.getCategory() != null ? t.getCategory().getName() : "Uncategorized";
                String cType = t.getCategory() != null ? t.getCategory().getType().name() : "EXPENSE";
                catTotals.put(cName, catTotals.getOrDefault(cName, BigDecimal.ZERO).add(t.getAmount()));
                catTypes.put(cName, cType);
            }
        }

        List<MonthlyReportDto.CategoryBreakdownItem> breakdownItems = new ArrayList<>();
        final BigDecimal totalExp = expense;
        catTotals.entrySet().stream()
                .sorted(Map.Entry.<String, BigDecimal>comparingByValue().reversed())
                .forEach(e -> {
                    BigDecimal pct = totalExp.compareTo(BigDecimal.ZERO) > 0
                            ? e.getValue().divide(totalExp, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100)).setScale(2, RoundingMode.HALF_UP)
                            : BigDecimal.ZERO;
                    breakdownItems.add(new MonthlyReportDto.CategoryBreakdownItem(
                            e.getKey(),
                            catTypes.getOrDefault(e.getKey(), "EXPENSE"),
                            MoneyUtils.formatIndian(e.getValue()),
                            pct
                    ));
                });

        // Budgets
        List<BudgetDto> budgets = budgetService.getBudgetsForMonth(userId, ym.getYear(), ym.getMonthValue());
        List<MonthlyReportDto.BudgetPerformanceItem> budgetItems = budgets.stream()
                .map(b -> new MonthlyReportDto.BudgetPerformanceItem(
                        b.getCategory() != null ? b.getCategory().getName() : "Overall Month Budget",
                        b.getLimitAmount(),
                        b.getSpentAmount(),
                        b.getRemainingAmount(),
                        b.getUtilizationPercent(),
                        b.getStatus() != null ? b.getStatus().name() : "NORMAL"
                ))
                .collect(Collectors.toList());

        // Goals
        List<SavingsGoalDto> goals = goalService.getActiveGoals(userId);
        List<MonthlyReportDto.GoalProgressItem> goalItems = goals.stream()
                .map(g -> new MonthlyReportDto.GoalProgressItem(
                        g.getName(),
                        g.getTargetAmount(),
                        g.getSavedAmount(),
                        g.getCompletionPercent(),
                        g.getTargetDate() != null ? g.getTargetDate().toString() : "No deadline"
                ))
                .collect(Collectors.toList());

        MonthlyReportDto report = new MonthlyReportDto();
        report.setMonth(ym.toString());
        report.setTotalIncome(MoneyUtils.formatIndian(income));
        report.setTotalExpense(MoneyUtils.formatIndian(expense));
        report.setNetSavings(MoneyUtils.formatIndian(netSavings));
        report.setSavingsRate(savingsRate);
        report.setCategoryBreakdown(breakdownItems);
        report.setBudgetPerformance(budgetItems);
        report.setGoalsSummary(goalItems);
        report.setGeneratedAt(DateUtils.now().format(DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a")));

        return report;
    }

    public byte[] generateMonthlyReportPdf(Long userId, String monthStr) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> ApiException.notFound("User not found"));

        MonthlyReportDto report = generateMonthlyReport(userId, monthStr);
        YearMonth ym = YearMonth.parse(report.getMonth());

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4, 36, 36, 40, 40);
            PdfWriter.getInstance(document, baos);
            document.open();

            // Fonts
            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 20, new Color(15, 23, 42)); // slate-900
            Font subtitleFont = FontFactory.getFont(FontFactory.HELVETICA, 10, new Color(100, 116, 139)); // slate-500
            Font sectionTitleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 13, new Color(30, 41, 59));
            Font tableHeaderFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Color.WHITE);
            Font cellFont = FontFactory.getFont(FontFactory.HELVETICA, 9, new Color(30, 41, 59));
            Font boldCellFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, new Color(15, 23, 42));

            // Header Banner
            Paragraph brand = new Paragraph("FinMate Financial Report", titleFont);
            brand.setAlignment(Element.ALIGN_LEFT);
            document.add(brand);

            Paragraph meta = new Paragraph(
                    String.format("Period: %s  |  Account: %s (%s)  |  Generated: %s",
                            ym.format(DateTimeFormatter.ofPattern("MMMM yyyy")),
                            user.getFullName(),
                            user.getEmail(),
                            report.getGeneratedAt()),
                    subtitleFont
            );
            meta.setSpacingAfter(18f);
            document.add(meta);

            // 1. Executive Summary Table
            Paragraph s1Title = new Paragraph("1. Monthly Overview", sectionTitleFont);
            s1Title.setSpacingAfter(8f);
            document.add(s1Title);

            PdfPTable summaryTable = new PdfPTable(4);
            summaryTable.setWidthPercentage(100);
            summaryTable.setSpacingAfter(18f);

            addHeaderCell(summaryTable, "Total Income", tableHeaderFont, new Color(30, 41, 59));
            addHeaderCell(summaryTable, "Total Expense", tableHeaderFont, new Color(30, 41, 59));
            addHeaderCell(summaryTable, "Net Savings", tableHeaderFont, new Color(30, 41, 59));
            addHeaderCell(summaryTable, "Savings Rate", tableHeaderFont, new Color(30, 41, 59));

            addCell(summaryTable, report.getTotalIncome(), boldCellFont, Element.ALIGN_CENTER, new Color(248, 250, 252));
            addCell(summaryTable, report.getTotalExpense(), boldCellFont, Element.ALIGN_CENTER, new Color(248, 250, 252));
            addCell(summaryTable, report.getNetSavings(), boldCellFont, Element.ALIGN_CENTER, new Color(248, 250, 252));
            addCell(summaryTable, report.getSavingsRate().toPlainString() + "%", boldCellFont, Element.ALIGN_CENTER, new Color(248, 250, 252));

            document.add(summaryTable);

            // 2. Category Breakdown Table
            Paragraph s2Title = new Paragraph("2. Expense Breakdown by Category", sectionTitleFont);
            s2Title.setSpacingAfter(8f);
            document.add(s2Title);

            PdfPTable catTable = new PdfPTable(4);
            catTable.setWidthPercentage(100);
            catTable.setWidths(new float[]{40, 20, 20, 20});
            catTable.setSpacingAfter(18f);

            addHeaderCell(catTable, "Category", tableHeaderFont, new Color(51, 65, 85));
            addHeaderCell(catTable, "Type", tableHeaderFont, new Color(51, 65, 85));
            addHeaderCell(catTable, "Amount (₹)", tableHeaderFont, new Color(51, 65, 85));
            addHeaderCell(catTable, "% of Total", tableHeaderFont, new Color(51, 65, 85));

            boolean alt = false;
            for (MonthlyReportDto.CategoryBreakdownItem item : report.getCategoryBreakdown()) {
                Color bg = alt ? new Color(248, 250, 252) : Color.WHITE;
                addCell(catTable, item.getCategoryName(), cellFont, Element.ALIGN_LEFT, bg);
                addCell(catTable, item.getCategoryType(), cellFont, Element.ALIGN_LEFT, bg);
                addCell(catTable, item.getAmount(), boldCellFont, Element.ALIGN_RIGHT, bg);
                addCell(catTable, item.getPercentage().toPlainString() + "%", cellFont, Element.ALIGN_RIGHT, bg);
                alt = !alt;
            }
            if (report.getCategoryBreakdown().isEmpty()) {
                PdfPCell empty = new PdfPCell(new Phrase("No expense transactions recorded for this period.", cellFont));
                empty.setColspan(4);
                empty.setPadding(8f);
                catTable.addCell(empty);
            }
            document.add(catTable);

            // 3. Budget Performance Table
            Paragraph s3Title = new Paragraph("3. Budget Performance", sectionTitleFont);
            s3Title.setSpacingAfter(8f);
            document.add(s3Title);

            PdfPTable budgetTable = new PdfPTable(6);
            budgetTable.setWidthPercentage(100);
            budgetTable.setWidths(new float[]{30, 15, 15, 15, 12, 13});
            budgetTable.setSpacingAfter(18f);

            addHeaderCell(budgetTable, "Budget / Category", tableHeaderFont, new Color(51, 65, 85));
            addHeaderCell(budgetTable, "Budgeted", tableHeaderFont, new Color(51, 65, 85));
            addHeaderCell(budgetTable, "Spent", tableHeaderFont, new Color(51, 65, 85));
            addHeaderCell(budgetTable, "Remaining", tableHeaderFont, new Color(51, 65, 85));
            addHeaderCell(budgetTable, "% Used", tableHeaderFont, new Color(51, 65, 85));
            addHeaderCell(budgetTable, "Status", tableHeaderFont, new Color(51, 65, 85));

            alt = false;
            for (MonthlyReportDto.BudgetPerformanceItem b : report.getBudgetPerformance()) {
                Color bg = alt ? new Color(248, 250, 252) : Color.WHITE;
                addCell(budgetTable, b.getCategoryName(), cellFont, Element.ALIGN_LEFT, bg);
                addCell(budgetTable, b.getBudgetAmount(), cellFont, Element.ALIGN_RIGHT, bg);
                addCell(budgetTable, b.getSpentAmount(), cellFont, Element.ALIGN_RIGHT, bg);
                addCell(budgetTable, b.getRemainingAmount(), cellFont, Element.ALIGN_RIGHT, bg);
                addCell(budgetTable, b.getPercentageUsed().toPlainString() + "%", cellFont, Element.ALIGN_RIGHT, bg);
                addCell(budgetTable, b.getStatus(), boldCellFont, Element.ALIGN_CENTER, bg);
                alt = !alt;
            }
            if (report.getBudgetPerformance().isEmpty()) {
                PdfPCell empty = new PdfPCell(new Phrase("No active budgets configured for this month.", cellFont));
                empty.setColspan(6);
                empty.setPadding(8f);
                budgetTable.addCell(empty);
            }
            document.add(budgetTable);

            // 4. Savings Goals
            Paragraph s4Title = new Paragraph("4. Savings Goals Progress", sectionTitleFont);
            s4Title.setSpacingAfter(8f);
            document.add(s4Title);

            PdfPTable goalTable = new PdfPTable(5);
            goalTable.setWidthPercentage(100);
            goalTable.setWidths(new float[]{35, 18, 18, 14, 15});
            goalTable.setSpacingAfter(18f);

            addHeaderCell(goalTable, "Goal Name", tableHeaderFont, new Color(51, 65, 85));
            addHeaderCell(goalTable, "Target (₹)", tableHeaderFont, new Color(51, 65, 85));
            addHeaderCell(goalTable, "Saved (₹)", tableHeaderFont, new Color(51, 65, 85));
            addHeaderCell(goalTable, "Progress", tableHeaderFont, new Color(51, 65, 85));
            addHeaderCell(goalTable, "Target Date", tableHeaderFont, new Color(51, 65, 85));

            alt = false;
            for (MonthlyReportDto.GoalProgressItem g : report.getGoalsSummary()) {
                Color bg = alt ? new Color(248, 250, 252) : Color.WHITE;
                addCell(goalTable, g.getGoalName(), cellFont, Element.ALIGN_LEFT, bg);
                addCell(goalTable, g.getTargetAmount(), cellFont, Element.ALIGN_RIGHT, bg);
                addCell(goalTable, g.getCurrentAmount(), boldCellFont, Element.ALIGN_RIGHT, bg);
                addCell(goalTable, g.getProgressPercentage().toPlainString() + "%", cellFont, Element.ALIGN_RIGHT, bg);
                addCell(goalTable, g.getTargetDate(), cellFont, Element.ALIGN_CENTER, bg);
                alt = !alt;
            }
            if (report.getGoalsSummary().isEmpty()) {
                PdfPCell empty = new PdfPCell(new Phrase("No active savings goals found.", cellFont));
                empty.setColspan(5);
                empty.setPadding(8f);
                goalTable.addCell(empty);
            }
            document.add(goalTable);

            // Footer note
            Paragraph footer = new Paragraph("FinMate Platform - Confidential Financial Summary", subtitleFont);
            footer.setAlignment(Element.ALIGN_CENTER);
            footer.setSpacingBefore(12f);
            document.add(footer);

            document.close();
            return baos.toByteArray();
        } catch (Exception ex) {
            throw new ApiException(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR, ErrorCode.INTERNAL_ERROR, "Failed to render PDF report: " + ex.getMessage());
        }
    }

    private void addHeaderCell(PdfPTable table, String text, Font font, Color bgColor) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBackgroundColor(bgColor);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPadding(6f);
        table.addCell(cell);
    }

    private void addCell(PdfPTable table, String text, Font font, int alignment, Color bgColor) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBackgroundColor(bgColor);
        cell.setHorizontalAlignment(alignment);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPadding(5f);
        table.addCell(cell);
    }
}
