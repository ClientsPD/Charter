package com.charter.rewards.dto;

/**
 * @author Prasanna Dupaguntla
 * @created 2/13/26 4:58 PM
 */

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Map;

@Data
@AllArgsConstructor
public class CustomerRewardsReport {
    private Long customerId;
    private String customerName;
    private Map<String, Double> monthlyRewards; // e.g., {"FEBRUARY 2026": 45.0}
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "0.00")
    private Double monthsTotal;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "0.00")
    private Double totalRewards; // The Grand Total
}
