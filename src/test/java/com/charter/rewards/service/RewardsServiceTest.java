package com.charter.rewards.service;

/**
 * @author Prasanna Dupaguntla
 * @created 2/13/26 10:17 PM
 */
import com.charter.rewards.dto.CustomerRewardsReport;
import com.charter.rewards.exception.CustomerNotFoundException;
import com.charter.rewards.model.Customer;
import com.charter.rewards.repository.CustomerRepository;
import com.charter.rewards.repository.RewardsRepository;
import com.charter.rewards.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestConstructor;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;


@SpringBootTest
@Transactional
@ActiveProfiles("test")
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
@DisplayName("Rewards Program Integration Suite")
public class RewardsServiceTest {

    private final RewardsService rewardsService;
    private final CustomerRepository customerRepo;
    private final RewardsRepository summaryRepo;
    private final TransactionRepository transactionRepo;

    public RewardsServiceTest(RewardsService rewardsService,
                              CustomerRepository customerRepo,
                              RewardsRepository summaryRepo,
                              TransactionRepository transactionRepo) {
        this.rewardsService = rewardsService;
        this.customerRepo = customerRepo;
        this.summaryRepo = summaryRepo;
        this.transactionRepo = transactionRepo;
    }

    // Helper to get the default 3-month range used
    private YearMonth getEnd() { return YearMonth.now(); }
    private YearMonth getStart() { return getEnd().minusMonths(3); }

    private void cleanUp() {
        transactionRepo.deleteAllInBatch();
        summaryRepo.deleteAllInBatch();
        customerRepo.deleteAllInBatch();
    }

    @BeforeEach
    void setUp() {
        cleanUp();

        Customer c1 = new Customer("Ben Shelton");
        c1.setTotalRewards(500.0);

        Customer c2 = new Customer("Carlos Alcaraz");
        c2.setTotalRewards(100.0);

        customerRepo.saveAllAndFlush(List.of(c1, c2));

        // Create transactions for the current month
        rewardsService.processPaymentWithDate(c1.getId(), 100.0, LocalDateTime.now());
        rewardsService.processPaymentWithDate(c2.getId(), 175.0, LocalDateTime.now());

        summaryRepo.flush();
    }

    @Test
    @DisplayName("Should rank customers correctly by Grand Total Rewards")
    void testSortByGrandTotal() {
        // Pass startMonth and endMonth
        Page<CustomerRewardsReport> report = rewardsService.getGrandReport(getStart(), getEnd(), "grand", PageRequest.of(0, 10));
        List<CustomerRewardsReport> content = report.getContent();

        assertTrue(content.get(0).getTotalRewards() >= content.get(1).getTotalRewards(),
                "Leaderboard #1 should have more or equal grand total rewards than #2");
    }

    @Test
    @DisplayName("Should rank customers correctly by Range Rolling Total")
    void testSortByRollingTotal() {
        // Pass startMonth and endMonth
        Page<CustomerRewardsReport> report = rewardsService.getGrandReport(getStart(), getEnd(), "rolling", PageRequest.of(0, 10));
        List<CustomerRewardsReport> content = report.getContent();

        assertTrue(content.get(0).getMonthsTotal() >= content.get(1).getMonthsTotal(),
                "Customer with higher rolling activity should be ranked first");
    }

    @Test
    @DisplayName("Should sort customer report alphabetically by name")
    void testSortByName() {
        Page<CustomerRewardsReport> report = rewardsService.getGrandReport(getStart(), getEnd(), "name", PageRequest.of(0, 10));
        List<CustomerRewardsReport> content = report.getContent();

        assertEquals("Ben Shelton", content.get(0).getCustomerName());
        assertEquals("Carlos Alcaraz", content.get(1).getCustomerName());
    }

    @Test
    @DisplayName("Should maintain correct relative order of Ben and Carlos in large datasets")
    void testSortByNameRelativeOrder() {
        Page<CustomerRewardsReport> report = rewardsService.getGrandReport(getStart(), getEnd(), "name", PageRequest.of(0, 100));
        List<CustomerRewardsReport> content = report.getContent();

        int benIndex = -1;
        int carlosIndex = -1;

        for (int i = 0; i < content.size(); i++) {
            if (content.get(i).getCustomerName().equalsIgnoreCase("Ben Shelton")) benIndex = i;
            if (content.get(i).getCustomerName().equalsIgnoreCase("Carlos Alcaraz")) carlosIndex = i;
        }

        assertTrue(benIndex < carlosIndex, "Ben (B) must appear before Carlos (C) alphabetically");
    }

    @Test
    @DisplayName("Should correctly calculate tiered points ($50-$100 and >$100)")
    void testPrivateTieredRewardsWithReflection() {
        Double points120 = ReflectionTestUtils.invokeMethod(rewardsService, "calculateTieredPoints", 120.0);
        assertEquals(90.0, points120, "Total for $120 should be (20*2) + (50*1) = 90");

        Double points75 = ReflectionTestUtils.invokeMethod(rewardsService, "calculateTieredPoints", 75.0);
        assertEquals(25.0, points75, "Total for $75 should be (25*1) = 25");

        Double points40 = ReflectionTestUtils.invokeMethod(rewardsService, "calculateTieredPoints", 40.0);
        assertEquals(0.0, points40, "Total for $40 should be 0");
    }

    @Test
    @DisplayName("Should verify pagination metadata and page boundaries")
    void testPaginationCalculation() {
        cleanUp();
        customerRepo.saveAllAndFlush(List.of(
                new Customer("User 1"), new Customer("User 2"),
                new Customer("User 3"), new Customer("User 4"),
                new Customer("User 5")
        ));

        // Pass YearMonth range
        Page<CustomerRewardsReport> firstPage = rewardsService.getGrandReport(getStart(), getEnd(), "name", PageRequest.of(0, 2));

        assertAll("Pagination Verification",
                () -> assertEquals(5, firstPage.getTotalElements(), "Total elements mismatch"),
                () -> assertEquals(3, firstPage.getTotalPages(), "Total pages mismatch"),
                () -> assertEquals(2, firstPage.getContent().size(), "Page size mismatch"),
                () -> assertTrue(firstPage.hasNext(), "Should have a next page")
        );

        Page<CustomerRewardsReport> lastPage = rewardsService.getGrandReport(getStart(), getEnd(), "name", PageRequest.of(2, 2));
        assertEquals(1, lastPage.getContent().size(), "Last page should have remainder");
        assertTrue(lastPage.isLast(), "Should be marked as last page");
    }

    @Test
    @DisplayName("Should process existing customer but throw for missing ID in same context")
    void testCustomerVerificationLogic() {
        // Setup  data
        Customer ben = customerRepo.save(new Customer("Ben Shelton"));
        Long validId = ben.getId();
        Long invalidId = -1L; // Guaranteed to be missing

        // Positive test case
        assertDoesNotThrow(() ->
                        rewardsService.processPaymentWithDate(validId, 100.0, LocalDateTime.now()),
                "Should work fine for Ben"
        );

        // Negative test case
        CustomerNotFoundException exception = assertThrows(
                CustomerNotFoundException.class,
                () -> rewardsService.processPaymentWithDate(invalidId, 100.0, LocalDateTime.now()),
                "Must throw for the fake ID"
        );

        assertEquals("Customer not found with ID: -1", exception.getMessage());
    }
}
