package com.charter.rewards.repository;

/**
 * @author Prasanna Dupaguntla
 * @created 2/13/26 3:06 PM
 */
import com.charter.rewards.model.Customer;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {}