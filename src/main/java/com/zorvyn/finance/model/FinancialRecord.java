package com.zorvyn.finance.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Represents a single financial record (income or expense).
 *
 * Soft delete is supported via the 'deleted' flag. Deleted records are
 * excluded from all queries using @Where / service-level filtering.
 */
@Entity
@Table(name = "financial_records")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FinancialRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @NotNull(message = "Record type is required")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RecordType type;

    @NotBlank(message = "Category is required")
    @Size(max = 100, message = "Category cannot exceed 100 characters")
    @Column(nullable = false)
    private String category;

    @NotNull(message = "Date is required")
    @Column(nullable = false)
    private LocalDate date;

    @Size(max = 500, message = "Notes cannot exceed 500 characters")
    @Column(length = 500)
    private String notes;

    /**
     * Soft delete flag. When true, the record is considered deleted and
     * should not appear in normal queries.
     */
    @Column(nullable = false)
    @Builder.Default
    private boolean deleted = false;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
