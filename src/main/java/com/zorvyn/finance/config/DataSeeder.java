package com.zorvyn.finance.config;

import com.zorvyn.finance.model.FinancialRecord;
import com.zorvyn.finance.model.RecordType;
import com.zorvyn.finance.model.Role;
import com.zorvyn.finance.model.User;
import com.zorvyn.finance.repository.FinancialRecordRepository;
import com.zorvyn.finance.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Seeds the H2 in-memory database with demo users and financial records
 * on application startup so the API is immediately testable without
 * any manual setup.
 *
 * Seeded accounts:
 *   admin   / Admin!12345  -> ADMIN
 *   alice   / Alice!12345  -> ANALYST
 *   bob     / Bob!12345    -> VIEWER
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final FinancialRecordRepository recordRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        seedUsers();
        seedRecords();
        log.info("=== Demo data seeded. Accounts: admin/Admin!12345, alice/Alice!12345, bob/Bob!12345 ===");
    }

    private void seedUsers() {
        if (userRepository.count() > 0) return;

        userRepository.save(User.builder()
                .username("admin")
                .password(passwordEncoder.encode("Admin!12345"))
                .role(Role.ADMIN)
                .active(true)
                .build());

        userRepository.save(User.builder()
                .username("alice")
                .password(passwordEncoder.encode("Alice!12345"))
                .role(Role.ANALYST)
                .active(true)
                .build());

        userRepository.save(User.builder()
                .username("bob")
                .password(passwordEncoder.encode("Bob!12345"))
                .role(Role.VIEWER)
                .active(true)
                .build());
    }

    private void seedRecords() {
        if (recordRepository.count() > 0) return;

        LocalDate today = LocalDate.now();

        recordRepository.save(record(new BigDecimal("5000.00"), RecordType.INCOME,    "Salary",        today.minusDays(1),  "Monthly salary"));
        recordRepository.save(record(new BigDecimal("1200.00"), RecordType.EXPENSE,   "Rent",          today.minusDays(2),  "Office rent"));
        recordRepository.save(record(new BigDecimal("450.00"),  RecordType.EXPENSE,   "Utilities",     today.minusDays(5),  "Electricity and water"));
        recordRepository.save(record(new BigDecimal("3000.00"), RecordType.INCOME,    "Consulting",    today.minusDays(10), "Freelance consulting"));
        recordRepository.save(record(new BigDecimal("200.00"),  RecordType.EXPENSE,   "Office Supplies", today.minusDays(12), "Stationery"));
        recordRepository.save(record(new BigDecimal("800.00"),  RecordType.EXPENSE,   "Software",      today.minusDays(15), "Annual license"));
        recordRepository.save(record(new BigDecimal("1500.00"), RecordType.INCOME,    "Sales",         today.minusDays(20), "Product sales"));
        recordRepository.save(record(new BigDecimal("300.00"),  RecordType.EXPENSE,   "Travel",        today.minusDays(22), "Business trip"));
        recordRepository.save(record(new BigDecimal("2200.00"), RecordType.INCOME,    "Salary",        today.minusMonths(1), "Previous month salary"));
        recordRepository.save(record(new BigDecimal("900.00"),  RecordType.EXPENSE,   "Rent",          today.minusMonths(1).minusDays(2), "Last month rent"));
    }

    private FinancialRecord record(BigDecimal amount, RecordType type, String category, LocalDate date, String notes) {
        return FinancialRecord.builder()
                .amount(amount)
                .type(type)
                .category(category)
                .date(date)
                .notes(notes)
                .deleted(false)
                .build();
    }
}
