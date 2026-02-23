package com.charter.rewards.repository;

/**
 * @author Prasanna Dupaguntla
 * @created 2/13/26 3:06 PM
 */
import com.charter.rewards.model.Rewards;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface RewardsRepository extends JpaRepository<Rewards, Long> {
    Optional<Rewards> findByCustomerIdAndYearMonth(Long customerId, String yearMonth);

    interface CustomerRollingProjection {
        Long getId();
        String getName();
        Double getGrandSum();
        Double getRollingSum();
    }

    @Query(value = """
        SELECT c.id AS id, c.name AS name, c.total_rewards AS grandSum,
        (SELECT COALESCE(SUM(r.points), 0) FROM rewards r 
         WHERE r.customer_id = c.id AND r.year_month IN :months) AS rollingSum
        FROM customer c
        """, nativeQuery = true)
    List<CustomerRollingProjection> findAllCalculated(@Param("months") List<String> months);
}