package com.zorvyn.finance.service;

import com.zorvyn.finance.dto.*;
import com.zorvyn.finance.exception.ResourceNotFoundException;
import com.zorvyn.finance.model.FinancialRecord;
import com.zorvyn.finance.model.RecordType;
import com.zorvyn.finance.repository.FinancialRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Set;

/**
 * Handles CRUD and filtering for financial records.
 *
 * Soft delete is used — records are never physically removed from the DB.
 * All queries operate on non-deleted records only.
 *
 * Pagination defaults: page=0, size=20, sorted by date descending.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class FinancialRecordService {

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of("date", "amount", "category", "createdAt");

    private final FinancialRecordRepository recordRepository;

    /** Create a new financial record. */
    public FinancialRecordResponse create(CreateFinancialRecordRequest request) {
        FinancialRecord record = FinancialRecord.builder()
                .amount(request.getAmount())
                .type(request.getType())
                .category(request.getCategory())
                .date(request.getDate())
                .notes(request.getNotes())
                .deleted(false)
                .build();

        return toResponse(recordRepository.save(record));
    }

    /**
     * Get paginated records with optional filters.
     * Any combination of type, category, fromDate, toDate is supported.
     */
    @Transactional(readOnly = true)
    public Page<FinancialRecordResponse> getAll(
            Boolean deleted,
            RecordType type,
            String category,
            String search,
            LocalDate fromDate,
            LocalDate toDate,
            int page,
            int size,
            String sortBy,
            String sortDir
    ) {
        if (fromDate != null && toDate != null && fromDate.isAfter(toDate)) {
            throw new IllegalArgumentException("fromDate must be on or before toDate");
        }

        Pageable pageable = PageRequest.of(page, size, buildSort(sortBy, sortDir));
        return recordRepository.findWithFilters(deleted, type, normalize(category), normalize(search), fromDate, toDate, pageable)
                .map(this::toResponse);
    }

    /** Get a single record by ID (will 404 if soft-deleted). */
    @Transactional(readOnly = true)
    public FinancialRecordResponse getById(Long id) {
        return toResponse(findRecordOrThrow(id));
    }

    /**
     * Partially update a financial record.
     * Any null field in the request is left unchanged.
     */
    public FinancialRecordResponse update(Long id, UpdateFinancialRecordRequest request) {
        FinancialRecord record = findRecordOrThrow(id);

        if (request.getAmount()   != null) record.setAmount(request.getAmount());
        if (request.getType()     != null) record.setType(request.getType());
        if (request.getCategory() != null) record.setCategory(request.getCategory());
        if (request.getDate()     != null) record.setDate(request.getDate());
        if (request.getNotes()    != null) record.setNotes(request.getNotes());

        return toResponse(recordRepository.save(record));
    }

    /** Soft-delete: marks the record as deleted instead of physically removing it. */
    public void delete(Long id) {
        FinancialRecord record = findRecordOrThrow(id);
        record.setDeleted(true);
        recordRepository.save(record);
    }

    /** Map entity → response DTO. */
    private FinancialRecordResponse toResponse(FinancialRecord r) {
        return FinancialRecordResponse.builder()
                .id(r.getId())
                .amount(r.getAmount())
                .type(r.getType())
                .category(r.getCategory())
                .date(r.getDate())
                .notes(r.getNotes())
                .createdAt(r.getCreatedAt())
                .updatedAt(r.getUpdatedAt())
                .build();
    }

    private FinancialRecord findRecordOrThrow(Long id) {
        return recordRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Financial record not found with id: " + id));
    }

    private Sort buildSort(String sortBy, String sortDir) {
        String field = normalize(sortBy);
        if (field == null) {
            field = "date";
        }
        if (!ALLOWED_SORT_FIELDS.contains(field)) {
            throw new IllegalArgumentException("Invalid sortBy. Allowed values: date, amount, category, createdAt");
        }

        String direction = normalize(sortDir);
        Sort.Direction dir = "asc".equalsIgnoreCase(direction) ? Sort.Direction.ASC : Sort.Direction.DESC;
        return Sort.by(dir, field);
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }
}
