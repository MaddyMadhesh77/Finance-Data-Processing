package com.zorvyn.finance.dto;

import com.zorvyn.finance.model.RecordType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Represents aggregated totals for a single month grouped by record type.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MonthlyTrend {
    private int year;
    private int month;
    private RecordType type;
    private BigDecimal total;
}
