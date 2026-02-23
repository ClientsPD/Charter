package com.charter.rewards.service;

/**
 * @author Prasanna Dupaguntla
 * @created 2/14/26 11:05 AM
 */

import com.charter.rewards.dto.CustomerRewardsReport;
import com.charter.rewards.exception.CustomerNotFoundException;
import com.charter.rewards.model.Customer;
import com.charter.rewards.repository.CustomerRepository;
import com.charter.rewards.repository.RewardsRepository;
import com.charter.rewards.repository.TransactionRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT) // To stop unnecessary stubbing errors
@DisplayName("Rewards Service PURE Unit Test")
class RewardsServiceUnitTest {

    @Mock
    private CustomerRepository customerRepo;
    @Mock
    private TransactionRepository transactionRepo;
    @Mock
    private RewardsRepository summaryRepo;

    @InjectMocks
    private RewardsService rewardsService;

    @Test
    @DisplayName("Unit: Should sort customer report alphabetically by name")
    void testSortByName_Mockito() {

        RewardsRepository.CustomerRollingProjection p1 = mock(RewardsRepository.CustomerRollingProjection.class);
        when(p1.getId()).thenReturn(1L);
        when(p1.getName()).thenReturn("Carlos Alcaraz");

        RewardsRepository.CustomerRollingProjection p2 = mock(RewardsRepository.CustomerRollingProjection.class);
        when(p2.getId()).thenReturn(2L);
        when(p2.getName()).thenReturn("Ben Shelton");

        when(summaryRepo.findAllCalculated(anyList())).thenReturn(List.of(p1, p2));
        when(customerRepo.findById(1L)).thenReturn(Optional.of(new Customer("Carlos Alcaraz")));
        when(customerRepo.findById(2L)).thenReturn(Optional.of(new Customer("Ben Shelton")));

        // Act: Sort by "name"
        Page<CustomerRewardsReport> report = rewardsService.getGrandReport(null, null, "name", PageRequest.of(0, 10));

        // Assert: Ben (B) should be index 0 because the Service's sorting logic worked
        assertEquals("Ben Shelton", report.getContent().get(0).getCustomerName());
    }

    @Test
    @DisplayName("Unit: Should rank customers correctly by Grand Total Rewards")
    void testSortByGrandTotal_Mockito() {
        // Arrange
        RewardsRepository.CustomerRollingProjection p1 = mock(RewardsRepository.CustomerRollingProjection.class);
        when(p1.getId()).thenReturn(1L);
        when(p1.getName()).thenReturn("Ben");
        when(p1.getGrandSum()).thenReturn(200.0);

        RewardsRepository.CustomerRollingProjection p2 = mock(RewardsRepository.CustomerRollingProjection.class);
        when(p2.getId()).thenReturn(2L);
        when(p2.getName()).thenReturn("Carlos");
        when(p2.getGrandSum()).thenReturn(100.0);

        when(summaryRepo.findAllCalculated(anyList())).thenReturn(List.of(p1, p2));
        when(customerRepo.findById(1L)).thenReturn(Optional.of(new Customer("Ben")));
        when(customerRepo.findById(2L)).thenReturn(Optional.of(new Customer("Carlos")));

        // Act
        Page<CustomerRewardsReport> report = rewardsService.getGrandReport(null, null, "grand", PageRequest.of(0, 10));

        // Assert
        assertTrue(report.getContent().get(0).getTotalRewards() >= report.getContent().get(1).getTotalRewards(),
                "Higher grand total must come first");
    }

    @Test
    @DisplayName("Unit: Should throw CustomerNotFoundException for missing ID")
    void testCustomerException() {
        // Create a fake projection that tells the service: "Found a customer with ID 999"
        RewardsRepository.CustomerRollingProjection mockProj = mock(RewardsRepository.CustomerRollingProjection.class);
        when(mockProj.getId()).thenReturn(999L);

        // Stub the summaryRepo so the service logic doesn't return an empty page early
        when(summaryRepo.findAllCalculated(anyList())).thenReturn(List.of(mockProj));

        // Stub the customerRepo to return EMPTY (this triggers the throw)
        when(customerRepo.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(CustomerNotFoundException.class,
                () -> rewardsService.getGrandReport(null, null, "name", PageRequest.of(0, 1)));
    }

    @Test
    @DisplayName("Should throw CustomerNotFoundException when Summary has ID but Customer Table is missing it")
    void testCustomerNotFoundIntegrity() {
        // Arrange: Summary says ID 999 exists (Ben is a 'ghost' here)
        RewardsRepository.CustomerRollingProjection ghost = mock(RewardsRepository.CustomerRollingProjection.class);
        when(ghost.getId()).thenReturn(999L);
        when(ghost.getName()).thenReturn("Ghost");

        // Stub: Summary returns the ghost
        when(summaryRepo.findAllCalculated(anyList())).thenReturn(List.of(ghost));

        // Stub: Customer table returns EMPTY (The actual "Not Found" trigger)
        when(customerRepo.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert: This will now THROW because the service is forced to map the ghost
        assertThrows(CustomerNotFoundException.class,
                () -> rewardsService.getGrandReport(null, null, "name", PageRequest.of(0, 10)));
    }

}
