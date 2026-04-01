package com.zorvyn.finance.repository;

import com.zorvyn.finance.model.FinancialRecord;
import com.zorvyn.finance.model.RecordType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface FinancialRecordRepository extends JpaRepository<FinancialRecord, Long> {

    // Find all non-deleted records with pagination support
    Page<FinancialRecord> findAllByDeletedFalse(Pageable pageable);

    // Find non-deleted record by id
    Optional<FinancialRecord> findByIdAndDeletedFalse(Long id);

    // Single-field filters
    Page<FinancialRecord> findByTypeAndDeletedFalse(RecordType type, Pageable pageable);
    Page<FinancialRecord> findByCategoryIgnoreCaseAndDeletedFalse(String category, Pageable pageable);

    // Date range filter
    Page<FinancialRecord> findByDateBetweenAndDeletedFalse(LocalDate from, LocalDate to, Pageable pageable);

    // Combined filter (all optional fields combined in one JPQL query)
    @Query("""
            SELECT r FROM FinancialRecord r
            WHERE (:deleted IS NULL OR r.deleted = :deleted)
              AND (:type IS NULL OR r.type = :type)
              AND (:category IS NULL OR LOWER(r.category) LIKE LOWER(CONCAT('%', :category, '%')))
              AND (:search IS NULL OR LOWER(r.category) LIKE LOWER(CONCAT('%', :search, '%'))
                               OR LOWER(COALESCE(r.notes, '')) LIKE LOWER(CONCAT('%', :search, '%')))
              AND (:from IS NULL OR r.date >= :from)
              AND (:to IS NULL OR r.date <= :to)
            """)
    Page<FinancialRecord> findWithFilters(
            @Param("deleted") Boolean deleted,
            @Param("type") RecordType type,
            @Param("category") String category,
            @Param("search") String search,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to,
            Pageable pageable
    );

    // Dashboard aggregations
    @Query("SELECT COALESCE(SUM(r.amount), 0) FROM FinancialRecord r WHERE r.type = :type AND r.deleted = false")
    BigDecimal sumByType(@Param("type") RecordType type);

    @Query("SELECT r.category, COALESCE(SUM(r.amount), 0) FROM FinancialRecord r WHERE r.deleted = false GROUP BY r.category")
    List<Object[]> sumGroupedByCategory();

    @Query("""
            SELECT YEAR(r.date), MONTH(r.date), r.type, COALESCE(SUM(r.amount), 0)
            FROM FinancialRecord r
            WHERE r.deleted = false
            GROUP BY YEAR(r.date), MONTH(r.date), r.type
            ORDER BY YEAR(r.date) DESC, MONTH(r.date) DESC
            """)
    List<Object[]> monthlyTrends();

    // Recent records ordered by date descending
    List<FinancialRecord> findTop10ByDeletedFalseOrderByDateDesc();
}
