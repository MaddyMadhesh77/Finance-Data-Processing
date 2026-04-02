package com.zorvyn.finance.dto;

import com.zorvyn.finance.model.RecordType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Request body for updating an existing financial record.
 * All fields are optional; only supplied fields are applied.
 */
@Data
public class UpdateFinancialRecordRequest {

    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    private BigDecimal amount;

    private RecordType type;

    @Size(max = 100, message = "Category cannot exceed 100 characters")
    private String category;

    private LocalDate date;

    @Size(max = 500, message = "Notes cannot exceed 500 characters")
    private String notes;
}
