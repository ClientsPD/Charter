package com.charter.rewards.repository;

/**
 * @author Prasanna Dupaguntla
 * @created 2/13/26 3:08 PM
 */

import com.charter.rewards.model.Transaction;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    List<Transaction> findAllByCustomerIdAndTimestampBetween(Long customerId, LocalDateTime start, LocalDateTime end);
}