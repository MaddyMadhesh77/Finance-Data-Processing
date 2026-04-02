package com.zorvyn.finance.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Aggregated dashboard summary returned by GET /api/dashboard/summary.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardSummary {
    private BigDecimal totalIncome;
    private BigDecimal totalExpenses;
    private BigDecimal netBalance;

    /** category -> total amount (income + expense combined) */
    private Map<String, BigDecimal> categoryTotals;

    /** Last 10 records ordered by date descending */
    private List<FinancialRecordResponse> recentActivity;

    /** Monthly breakdown */
    private List<MonthlyTrend> monthlyTrends;
}
