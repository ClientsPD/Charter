package com.charter.rewards.controller;

/**
 * @author Prasanna Dupaguntla
 * @created 2/16/26 11:22 AM
 */
import com.charter.rewards.dto.CustomerRewardsReport;
import com.charter.rewards.service.RewardsService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest; // Modular 4.0 package
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean; // Mandatory replacement for @MockBean
import org.springframework.test.web.servlet.MockMvc;

import java.time.YearMonth;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RewardsController.class)
@DisplayName("Rewards Controller API Endpoint TEST Class")
class RewardsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RewardsService rewardsService;

    @Configuration
    @Import(RewardsController.class)
    static class TestContextConfig { }

    @Test
    @DisplayName("Should return 200 OK with valid report for explicit start and end dates")
    void shouldReturnGrandReportWithDateRange() throws Exception {
        // Arrange
        CustomerRewardsReport report = new CustomerRewardsReport(
                1L, "BEN", Map.of("2025-01", 120.0), 120.0, 90.0
        );

        when(rewardsService.getGrandReport(any(YearMonth.class), any(YearMonth.class), anyString(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(report)));

        // Act & Assert: Using new startDate and endDate params
        mockMvc.perform(get("/api/reports/grand")
                        .param("startDate", "2025-01")
                        .param("endDate", "2025-04")
                        .param("sortBy", "rolling")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].customerName").value("BEN"));
    }

    @Test
    @DisplayName("Should default to current month minus 3 when no parameters provided")
    void shouldDefaultToCurrentMonthRangeWhenParamsMissing() throws Exception {
        when(rewardsService.getGrandReport(any(), any(), anyString(), any()))
                .thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/api/reports/grand")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        verify(rewardsService).getGrandReport(isNull(), isNull(), eq("rolling"), any(Pageable.class));
    }

    @Test
    @DisplayName("Should set end date to start + 3 months when only startDate is provided")
    void shouldSetEndDateToStartPlusThreeMonths() throws Exception {
        YearMonth providedStart = YearMonth.of(2024, 1);

        when(rewardsService.getGrandReport(any(), any(), anyString(), any()))
                .thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/api/reports/grand")
                        .param("startDate", "2024-01")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        verify(rewardsService).getGrandReport(eq(providedStart), isNull(), eq("rolling"), any(Pageable.class));
    }

    @Test
    @DisplayName("Should calculate 3-month range when only endDate is provided")
    void shouldCalculateRangeWhenOnlyEndDateProvided() throws Exception {
        YearMonth providedEnd = YearMonth.of(2023, 12);

        when(rewardsService.getGrandReport(any(), any(), anyString(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(Collections.emptyList()));

        mockMvc.perform(get("/api/reports/grand")
                        .param("endDate", "2023-12")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        verify(rewardsService).getGrandReport(isNull(), eq(providedEnd), eq("rolling"), any(Pageable.class));
    }

    //
    @Test
    @DisplayName("Should return 200 OK with valid report for explicit start and end dates")
    void shouldReturnGrandReportWithDateRangex() throws Exception {
        // Arrange: Create the data the SERVICE will return
        CustomerRewardsReport report = new CustomerRewardsReport(
                1L, "BEN", Map.of("2025-01", 120.0), 120.0, 90.0
        );

        // Stub: Tell the Mock Service to return this data
        when(rewardsService.getGrandReport(any(), any(), anyString(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(report)));

        // Act & Assert
        mockMvc.perform(get("/api/reports/grand")
                        .param("startDate", "2025-01")
                        .param("endDate", "2025-04")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()) // Testing the Controller's Response Status
                .andExpect(jsonPath("$.content[0].customerName").value("BEN")) // Testing the Controller's JSON output
                .andExpect(jsonPath("$.content[0].totalRewards").value(90.0));
    }

    @Test
    @DisplayName("Should default to current month minus 3 when no parameters provided")
    void shouldDefaultToCurrentMonthRangeWhenParamsMissing1() throws Exception {
        // Arrange: Create the DTO with the data we expect
        CustomerRewardsReport report = new CustomerRewardsReport(
                1L, "BEN", Map.of("2025-11", 120.0), 120.0, 90.0
        );

        // Stub: pass NULLs for missing params
        when(rewardsService.getGrandReport(isNull(), isNull(), anyString(), any()))
                .thenReturn(new PageImpl<>(List.of(report)));

        // Act & Assert
        mockMvc.perform(get("/api/reports/grand")
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andDo(org.springframework.test.web.servlet.result.MockMvcResultHandlers.print())
                .andExpect(jsonPath("$.content[0].customerId").value(1))
                .andExpect(jsonPath("$.content[0].customerName").value("BEN"))
                .andExpect(jsonPath("$.content[0].monthsTotal").value(120.0));

    }

    @Test
    @DisplayName("Should set end date to start + 3 months when only startDate is provided")
    void shouldSetEndDateToStartPlusThreeMonths1() throws Exception {
        // Stub
        when(rewardsService.getGrandReport(any(), isNull(), anyString(), any()))
                .thenReturn(new PageImpl<>(List.of()));

        // Act: Pass startDate and null endDate
        mockMvc.perform(get("/api/reports/grand")
                        .param("startDate", "2024-01"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Should calculate 3-month range when only endDate is provided")
    void shouldCalculateRangeWhenOnlyEndDateProvided1() throws Exception {
        // Stub
        when(rewardsService.getGrandReport(isNull(), any(), anyString(), any()))
                .thenReturn(new PageImpl<>(List.of()));

        // Act: Pass null startDate and provided endDate
        mockMvc.perform(get("/api/reports/grand")
                        .param("endDate", "2023-12"))
                .andExpect(status().isOk());
    }

}