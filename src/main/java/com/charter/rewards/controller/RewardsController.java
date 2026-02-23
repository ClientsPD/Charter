package com.charter.rewards.controller;

/**
 * @author Prasanna Dupaguntla
 * @created 2/13/26 3:12 PM
 */
import com.charter.rewards.dto.CustomerRewardsReport;
import com.charter.rewards.service.RewardsService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.YearMonth;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class RewardsController {
    private final RewardsService rewardsService;

    /**
     * Retrieves a paginated "Grand Report" of customer rewards within a calculated time range.
     *
     * <p>The date range logic follows these prioritized rules:
     * <ul>
     *     <li><b>Both dates provided:</b> Uses the exact {@code startDate} to {@code endDate} range.</li>
     *     <li><b>Only startDate provided:</b> Sets the range from {@code startDate} to 3 months after.</li>
     *     <li><b>Only endDate provided:</b> Sets the range from 3 months before {@code endDate} up to {@code endDate}.</li>
     *     <li><b>No dates provided:</b> Defaults to the last 3 months ending at the current month.</li>
     * </ul>
     *
     * @param startDate The starting month of the report in {@code yyyy-MM} format (optional).
     * @param endDate   The ending month of the report in {@code yyyy-MM} format (optional).
     * @param sortBy    The criteria to sort the results by. Accepted values:
     *                  {@code "grand"}, {@code "rolling"}, or {@code "name"}. Defaults to {@code "rolling"}.
     * @param pageable  Pagination information. Defaults to {@link Integer#MAX_VALUE} to return
     *                  all records unless specific {@code page} or {@code size} parameters are provided.
     * @return A {@link ResponseEntity} containing a {@link Page} of {@link CustomerRewardsReport}.
     */
    @GetMapping("/reports/grand")
    public ResponseEntity<Page<CustomerRewardsReport>> getGrandReport(
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM") YearMonth startDate,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM") YearMonth endDate,
            @RequestParam(defaultValue = "rolling") String sortBy,
            @PageableDefault(size = Integer.MAX_VALUE) Pageable pageable) {

        // Pass the range to the service
        return ResponseEntity.ok(rewardsService.getGrandReport(startDate, endDate, sortBy, pageable));
    }
}
