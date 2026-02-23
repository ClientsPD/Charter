package com.charter.rewards.service;

/**
 * @author Prasanna Dupaguntla
 * @created 2/13/26 3:10 PM
 */
import com.charter.rewards.dto.CustomerRewardsReport;

import com.charter.rewards.exception.CustomerNotFoundException;

import com.charter.rewards.model.Customer;
import com.charter.rewards.model.Rewards;
import com.charter.rewards.model.Transaction;

import com.charter.rewards.repository.CustomerRepository;
import com.charter.rewards.repository.RewardsRepository;
import com.charter.rewards.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RewardsService {

    private final CustomerRepository customerRepo;
    private final TransactionRepository transactionRepo;
    private final RewardsRepository summaryRepo;

    @Transactional(noRollbackFor = RuntimeException.class)
    public Transaction processPaymentWithDate(Long customerId, Double amount, LocalDateTime date) {
        Customer customer = customerRepo.findById(customerId).orElseThrow(() -> new CustomerNotFoundException(customerId));
        amount = round(amount);
        Double points = calculateTieredPoints(amount);

        customer.setTotalRewards(round(customer.getTotalRewards() + points));
        customerRepo.save(customer);

        String monthKey = YearMonth.from(date).toString();
        Rewards summary = summaryRepo.findByCustomerIdAndYearMonth(customerId, monthKey)
                .orElse(new Rewards(customerId, monthKey, 0.0));

        summary.setPoints(round(summary.getPoints() + points));
        summaryRepo.save(summary);

        Transaction tx = new Transaction();
        tx.setAmount(amount);
        tx.setRewardPoints(points);
        tx.setTimestamp(date);
        tx.setCustomer(customer);
        return transactionRepo.save(tx);
    }

    public Page<CustomerRewardsReport> getGrandReport(YearMonth startDate, YearMonth endDate, String sortBy, Pageable pageable) {

        YearMonth finalStart;
        YearMonth finalEnd;

        if (startDate != null && endDate != null) {
            finalStart = startDate;
            finalEnd = endDate;
        } else if (startDate != null) {
            finalStart = startDate;
            finalEnd = startDate.plusMonths(3);
        } else if (endDate != null) {
            finalEnd = endDate;
            finalStart = endDate.minusMonths(3);
        } else {
            finalEnd = YearMonth.now();
            finalStart = finalEnd.minusMonths(3);
        }

        // Generate list of month keys between start and end (inclusive)
        List<String> monthKeys = new ArrayList<>();
        YearMonth current = finalStart;
        while (!current.isAfter(finalEnd)) {
            monthKeys.add(current.toString());
            current = current.plusMonths(1);
        }

        // Fetch aggregated data for the selected months
        List<RewardsRepository.CustomerRollingProjection> allData = summaryRepo.findAllCalculated(monthKeys);

        // Sorting Logic
        Comparator<RewardsRepository.CustomerRollingProjection> comparator = switch (sortBy.toLowerCase()) {
            case "name" -> Comparator.comparing(p -> p.getName().toUpperCase());
            case "grand" -> Comparator.comparing(RewardsRepository.CustomerRollingProjection::getGrandSum).reversed();
            default -> Comparator.comparing(RewardsRepository.CustomerRollingProjection::getRollingSum).reversed();
        };

        List<RewardsRepository.CustomerRollingProjection> sortedList = allData.stream().sorted(comparator).toList();

        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), sortedList.size());
        List<RewardsRepository.CustomerRollingProjection> pagedSubList = (start < sortedList.size())
                ? sortedList.subList(start, end) : Collections.emptyList();

        List<CustomerRewardsReport> content = pagedSubList.stream().map(p -> {
            return getMonthlyReportInRange(p.getId(), finalStart, finalEnd);
        }).collect(Collectors.toList());

        return new PageImpl<>(content, pageable, sortedList.size());
    }

    private CustomerRewardsReport getMonthlyReportInRange(Long customerId, YearMonth startMonth, YearMonth endMonth) {
        Customer customer = customerRepo.findById(customerId).orElseThrow(() -> new CustomerNotFoundException(customerId));

        // Convert YearMonth boundaries to LocalDateTime for DB query
        LocalDateTime startDateTime = startMonth.atDay(1).atStartOfDay();
        LocalDateTime endDateTime = endMonth.atEndOfMonth().atTime(23, 59, 59);

        // findAllByCustomerIdAndTimestampBetween returns an empty list if no results; SO NO NULL CHECK REQUIRED.
        // SHOULD NOT THROW EXCEPTION HERE. If a customer has no transactions in this specific range, SHOULD NOT CRASH THE ENTIRE PAGE REQUEST.
        List<Transaction> transactions = transactionRepo.findAllByCustomerIdAndTimestampBetween(customerId, startDateTime, endDateTime);

        // Date Format e.g, : "2026-01".
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM");

        Map<String, Double> monthlyRewards = transactions.stream()
                .collect(Collectors.groupingBy(
                        t -> YearMonth.from(t.getTimestamp()),
                        TreeMap::new,
                        Collectors.collectingAndThen(Collectors.summingDouble(Transaction::getRewardPoints), this::round)
                ))
                .entrySet().stream()
                .collect(Collectors.toMap(
                        e -> e.getKey().format(formatter),
                        Map.Entry::getValue,
                        (v1, v2) -> v1,
                        LinkedHashMap::new)
                );

        Double rollingTotal = round(monthlyRewards.values().stream().mapToDouble(d -> d).sum());
        return new CustomerRewardsReport(customer.getId(), customer.getName(), monthlyRewards, rollingTotal, customer.getTotalRewards());
    }

    private Double calculateTieredPoints(Double amount) {
        double points = (amount > 100) ? (amount - 100) * 2 + 50 : (amount > 50) ? (amount - 50) : 0;
        return round(points);
    }

    private Double round(Double value) {
        return (value == null) ? 0.0 : BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

}