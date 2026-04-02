package com.zorvyn.finance.service;

import com.zorvyn.finance.dto.FinancialRecordResponse;
import com.zorvyn.finance.model.FinancialRecord;
import com.zorvyn.finance.model.RecordType;
import com.zorvyn.finance.repository.FinancialRecordRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FinancialRecordServiceTest {

    @Mock
    private FinancialRecordRepository recordRepository;

    @InjectMocks
    private FinancialRecordService recordService;

    @Test
    void getAll_appliesPagingAndSort() {
        FinancialRecord record = FinancialRecord.builder()
                .id(11L)
                .amount(new BigDecimal("250.00"))
                .type(RecordType.EXPENSE)
                .category("Rent")
                .date(LocalDate.of(2026, 4, 1))
                .notes("Office rent")
                .deleted(false)
                .build();

        when(recordRepository.findWithFilters(eq(false), eq(RecordType.EXPENSE), eq("Rent"), eq("office"), any(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(record)));

        Page<FinancialRecordResponse> response = recordService.getAll(
                false,
                RecordType.EXPENSE,
                "Rent",
                " office ",
                null,
                null,
                2,
                25,
                "amount",
                "asc"
        );

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(recordRepository).findWithFilters(eq(false), eq(RecordType.EXPENSE), eq("Rent"), eq("office"), isNull(), isNull(), pageableCaptor.capture());

        Pageable pageable = pageableCaptor.getValue();
        assertEquals(2, pageable.getPageNumber());
        assertEquals(25, pageable.getPageSize());
                assertEquals(Sort.by("amount").ascending(), pageable.getSort());
        assertEquals(1, response.getTotalElements());
        assertEquals(11L, response.getContent().get(0).getId());
    }

    @Test
    void getAll_canRequestDeletedRecords() {
        when(recordRepository.findWithFilters(eq(true), isNull(), isNull(), isNull(), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        Page<FinancialRecordResponse> response = recordService.getAll(
                true,
                null,
                null,
                null,
                null,
                null,
                0,
                20,
                "date",
                "desc"
        );

        assertEquals(0, response.getTotalElements());
        verify(recordRepository).findWithFilters(eq(true), isNull(), isNull(), isNull(), isNull(), isNull(), any(Pageable.class));
    }

    @Test
    void getAll_invalidDateRange_throwsBadRequestSignal() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> recordService.getAll(
                        false,
                        null,
                        null,
                        null,
                        LocalDate.of(2026, 5, 10),
                        LocalDate.of(2026, 5, 1),
                        0,
                        20,
                        "date",
                        "desc"
                )
        );

        assertEquals("fromDate must be on or before toDate", ex.getMessage());
        verify(recordRepository, never()).findWithFilters(any(), any(), any(), any(), any(), any(), any(Pageable.class));
    }

    @Test
    void getAll_invalidSortField_throwsBadRequestSignal() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> recordService.getAll(
                        false,
                        null,
                        null,
                        null,
                        null,
                        null,
                        0,
                        20,
                        "notes",
                        "asc"
                )
        );

        assertTrue(ex.getMessage().contains("Invalid sortBy"));
        verify(recordRepository, never()).findWithFilters(any(), any(), any(), any(), any(), any(), any(Pageable.class));
    }

    @Test
    void delete_marksRecordAsDeleted() {
        FinancialRecord record = FinancialRecord.builder()
                .id(15L)
                .amount(new BigDecimal("100.00"))
                .type(RecordType.INCOME)
                .category("Sales")
                .date(LocalDate.of(2026, 4, 2))
                .deleted(false)
                .build();

        when(recordRepository.findByIdAndDeletedFalse(15L)).thenReturn(java.util.Optional.of(record));

        recordService.delete(15L);

        assertTrue(record.isDeleted());
        verify(recordRepository).save(record);
    }
}