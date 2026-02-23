package com.charter.rewards.model;


import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author Prasanna Dupaguntla
 * @created 2/13/26 5:20 PM
 */
@Entity
@Table(name = "rewards", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"customer_id", "year_month"})
})
@Data
@NoArgsConstructor
public class Rewards {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "customer_id")
    private Long customerId;

    @Column(name = "year_month")
    private String yearMonth; // e.g., "2026-02"
    private Double points = 0.0;

    public Rewards(Long customerId, String yearMonth, Double points) {
        this.customerId = customerId;
        this.yearMonth = yearMonth;
        this.points = points;
    }
}
