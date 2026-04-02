package com.zorvyn.finance;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.zorvyn.finance.dto.CreateFinancialRecordRequest;
import com.zorvyn.finance.dto.CreateUserRequest;
import com.zorvyn.finance.model.RecordType;
import com.zorvyn.finance.model.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests — run against a real Spring context with H2 in-memory DB.
 *
 * Tests cover:
 *  - Access control (role enforcement)
 *  - User creation and validation
 *  - Financial record CRUD
 *  - Dashboard summary
 *  - Input validation (bad requests)
 */
@SpringBootTest
@AutoConfigureMockMvc
class FinanceBackendIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private ObjectMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
    }

    // ────────────────────────────────────────────────────────────────────────────
    // Access Control Tests
    // ────────────────────────────────────────────────────────────────────────────

    @Test
    @WithMockUser(username = "bob", roles = "VIEWER")
    void viewer_canAccess_dashboardSummary() throws Exception {
        mockMvc.perform(get("/api/dashboard/summary"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "bob", roles = "VIEWER")
    void viewer_cannotAccess_records() throws Exception {
        mockMvc.perform(get("/api/records"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "bob", roles = "VIEWER")
    void viewer_cannotCreate_records() throws Exception {
        mockMvc.perform(post("/api/records")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "alice", roles = "ANALYST")
    void analyst_canAccess_records() throws Exception {
        mockMvc.perform(get("/api/records"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "alice", roles = "ANALYST")
    void analyst_cannotCreate_records() throws Exception {
        CreateFinancialRecordRequest req = new CreateFinancialRecordRequest();
        req.setAmount(new BigDecimal("100.00"));
        req.setType(RecordType.INCOME);
        req.setCategory("Test");
        req.setDate(LocalDate.now());

        mockMvc.perform(post("/api/records")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "alice", roles = "ANALYST")
    void analyst_cannotManage_users() throws Exception {
        mockMvc.perform(get("/api/users"))
                .andExpect(status().isForbidden());
    }

    // ────────────────────────────────────────────────────────────────────────────
    // User Management Tests
    // ────────────────────────────────────────────────────────────────────────────

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void admin_canList_users() throws Exception {
        mockMvc.perform(get("/api/users"))
                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void admin_canCreate_user() throws Exception {
        CreateUserRequest req = new CreateUserRequest();
        req.setUsername("testuser_" + System.currentTimeMillis());
        req.setPassword("Password!123");
        req.setRole(Role.VIEWER);

        mockMvc.perform(post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").isString())
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void createUser_withBlankUsername_returns400() throws Exception {
        CreateUserRequest req = new CreateUserRequest();
        req.setUsername("");
        req.setPassword("Password!123");
        req.setRole(Role.VIEWER);

        mockMvc.perform(post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.username").exists());
    }

    // ────────────────────────────────────────────────────────────────────────────
    // Financial Record Tests
    // ────────────────────────────────────────────────────────────────────────────

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void admin_canCreate_and_getRecord() throws Exception {
        CreateFinancialRecordRequest req = new CreateFinancialRecordRequest();
        req.setAmount(new BigDecimal("250.00"));
        req.setType(RecordType.EXPENSE);
        req.setCategory("TestCategory");
        req.setDate(LocalDate.now());
        req.setNotes("Integration test record");

        String response = mockMvc.perform(post("/api/records")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.category").value("TestCategory"))
                .andReturn().getResponse().getContentAsString();

        Long id = mapper.readTree(response).get("id").asLong();

        mockMvc.perform(get("/api/records/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id));
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void admin_canSoftDelete_record() throws Exception {
        CreateFinancialRecordRequest req = new CreateFinancialRecordRequest();
        req.setAmount(new BigDecimal("100.00"));
        req.setType(RecordType.INCOME);
        req.setCategory("DeleteTest");
        req.setDate(LocalDate.now());

        String response = mockMvc.perform(post("/api/records")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long id = mapper.readTree(response).get("id").asLong();

        // Delete
        mockMvc.perform(delete("/api/records/" + id))
                .andExpect(status().isNoContent());

        // Deleted record should return 404
        mockMvc.perform(get("/api/records/" + id))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void createRecord_withNegativeAmount_returns400() throws Exception {
        CreateFinancialRecordRequest req = new CreateFinancialRecordRequest();
        req.setAmount(new BigDecimal("-50.00"));
        req.setType(RecordType.INCOME);
        req.setCategory("Bad");
        req.setDate(LocalDate.now());

        mockMvc.perform(post("/api/records")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.amount").exists());
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void records_canBeFiltered_byType() throws Exception {
        mockMvc.perform(get("/api/records?type=INCOME"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void records_canBeSearched_andPaginated() throws Exception {
        CreateFinancialRecordRequest req = new CreateFinancialRecordRequest();
        req.setAmount(new BigDecimal("180.00"));
        req.setType(RecordType.EXPENSE);
        req.setCategory("Travel Meals");
        req.setDate(LocalDate.now());
        req.setNotes("Client lunch reimbursement");

        mockMvc.perform(post("/api/records")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(req)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/records?search=lunch&page=0&size=5&sortBy=amount&sortDir=asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.size").value(5));
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void deletedRecords_canBeQueried_withDeletedTrue() throws Exception {
        CreateFinancialRecordRequest req = new CreateFinancialRecordRequest();
        req.setAmount(new BigDecimal("90.00"));
        req.setType(RecordType.EXPENSE);
        req.setCategory("ArchiveMe");
        req.setDate(LocalDate.now());

        String response = mockMvc.perform(post("/api/records")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long id = mapper.readTree(response).get("id").asLong();

        mockMvc.perform(delete("/api/records/" + id))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/records?deleted=true&search=ArchiveMe"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

        @Test
        @WithMockUser(username = "admin", roles = "ADMIN")
        void records_withInvalidDateRange_returns400() throws Exception {
                mockMvc.perform(get("/api/records?fromDate=2026-05-10&toDate=2026-05-01"))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.message").value("fromDate must be on or before toDate"));
        }

        @Test
        @WithMockUser(username = "admin", roles = "ADMIN")
        void users_supportSortParams() throws Exception {
                mockMvc.perform(get("/api/users?page=0&size=5&sortBy=username&sortDir=asc"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.content").isArray())
                                .andExpect(jsonPath("$.size").value(5));
        }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void admin_canSoftDelete_user() throws Exception {
        CreateUserRequest req = new CreateUserRequest();
        req.setUsername("softdelete_" + System.currentTimeMillis());
        req.setPassword("Password!123");
        req.setRole(Role.VIEWER);

        String response = mockMvc.perform(post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long id = mapper.readTree(response).get("id").asLong();

        mockMvc.perform(delete("/api/users/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));

        mockMvc.perform(get("/api/users?username=softdelete_"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].active").value(false));
    }

    // ────────────────────────────────────────────────────────────────────────────
    // Dashboard Tests
    // ────────────────────────────────────────────────────────────────────────────

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void admin_canGet_dashboardSummary() throws Exception {
        mockMvc.perform(get("/api/dashboard/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalIncome").exists())
                .andExpect(jsonPath("$.totalExpenses").exists())
                .andExpect(jsonPath("$.netBalance").exists())
                .andExpect(jsonPath("$.categoryTotals").exists())
                .andExpect(jsonPath("$.monthlyTrends").isArray())
                .andExpect(jsonPath("$.recentActivity").isArray());
    }

    @Test
    void unauthenticated_request_returns403() throws Exception {
        mockMvc.perform(get("/api/dashboard/summary"))
                .andExpect(status().isForbidden());
    }
}
