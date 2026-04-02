package com.zorvyn.finance.service;

import com.zorvyn.finance.dto.*;
import com.zorvyn.finance.model.RecordType;
import com.zorvyn.finance.repository.FinancialRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Returns pre-computed summary and trend data for the dashboard.
 *
 * All calculations are performed in the database layer (JPQL aggregations)
 * rather than in application memory, keeping this service thin and efficient.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardService {

    private final FinancialRecordRepository recordRepository;

    /**
     * Build the full dashboard summary in a single service call.
     * Internally makes 4 DB calls:
     *   1. SUM of income
     *   2. SUM of expenses
     *   3. GROUP BY category totals
     *   4. Monthly trends (year + month + type aggregation)
     *   5. Top-10 recent records
     */
    public DashboardSummary getSummary() {
        BigDecimal totalIncome   = recordRepository.sumByType(RecordType.INCOME);
        BigDecimal totalExpenses = recordRepository.sumByType(RecordType.EXPENSE);
        BigDecimal netBalance    = totalIncome.subtract(totalExpenses);

        Map<String, BigDecimal> categoryTotals = buildCategoryTotals();
        List<MonthlyTrend> trends              = buildMonthlyTrends();
        List<FinancialRecordResponse> recent   = buildRecentActivity();

        return DashboardSummary.builder()
                .totalIncome(totalIncome)
                .totalExpenses(totalExpenses)
                .netBalance(netBalance)
                .categoryTotals(categoryTotals)
                .monthlyTrends(trends)
                .recentActivity(recent)
                .build();
    }

    private Map<String, BigDecimal> buildCategoryTotals() {
        List<Object[]> rows = recordRepository.sumGroupedByCategory();
        Map<String, BigDecimal> result = new LinkedHashMap<>();
        for (Object[] row : rows) {
            result.put((String) row[0], (BigDecimal) row[1]);
        }
        return result;
    }

    private List<MonthlyTrend> buildMonthlyTrends() {
        return recordRepository.monthlyTrends().stream()
                .map(row -> MonthlyTrend.builder()
                        .year(((Number) row[0]).intValue())
                        .month(((Number) row[1]).intValue())
                        .type((RecordType) row[2])
                        .total((BigDecimal) row[3])
                        .build())
                .toList();
    }

    private List<FinancialRecordResponse> buildRecentActivity() {
        return recordRepository.findTop10ByDeletedFalseOrderByDateDesc().stream()
                .map(r -> FinancialRecordResponse.builder()
                        .id(r.getId())
                        .amount(r.getAmount())
                        .type(r.getType())
                        .category(r.getCategory())
                        .date(r.getDate())
                        .notes(r.getNotes())
                        .createdAt(r.getCreatedAt())
                        .updatedAt(r.getUpdatedAt())
                        .build())
                .toList();
    }
}
