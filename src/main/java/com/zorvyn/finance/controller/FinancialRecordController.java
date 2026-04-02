package com.zorvyn.finance.controller;

import com.zorvyn.finance.dto.*;
import com.zorvyn.finance.model.RecordType;
import com.zorvyn.finance.service.FinancialRecordService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

/**
 * REST controller for financial records management.
 *
 * Read access:  ADMIN + ANALYST  (configured in SecurityConfig)
 * Write access: ADMIN only       (configured in SecurityConfig)
 *
 * Endpoints:
 *   POST   /api/records            -> create record
 *   GET    /api/records            -> list with optional filters + pagination
 *   GET    /api/records/{id}       -> get single record
 *   PUT    /api/records/{id}       -> update record
 *   DELETE /api/records/{id}       -> soft-delete record
 *
 * Filter query params (all optional, combinable):
 *   type       -> INCOME | EXPENSE
 *   category   -> case-insensitive string
 *   fromDate   -> ISO date (yyyy-MM-dd)
 *   toDate     -> ISO date (yyyy-MM-dd)
 *   page       -> page number (default 0)
 *   size       -> page size (default 20)
 */
@RestController
@RequestMapping("/api/records")
@Validated
@RequiredArgsConstructor
public class FinancialRecordController {

    private final FinancialRecordService recordService;

    @PostMapping
    public ResponseEntity<FinancialRecordResponse> create(
            @Valid @RequestBody CreateFinancialRecordRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(recordService.create(request));
    }

    @GetMapping
    public ResponseEntity<Page<FinancialRecordResponse>> getAll(
            @RequestParam(defaultValue = "false") Boolean deleted,
            @RequestParam(required = false) RecordType type,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(defaultValue = "date") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir
    ) {
        return ResponseEntity.ok(recordService.getAll(deleted, type, category, search, fromDate, toDate, page, size, sortBy, sortDir));
    }

    @GetMapping("/{id}")
    public ResponseEntity<FinancialRecordResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(recordService.getById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<FinancialRecordResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateFinancialRecordRequest request
    ) {
        return ResponseEntity.ok(recordService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        recordService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
