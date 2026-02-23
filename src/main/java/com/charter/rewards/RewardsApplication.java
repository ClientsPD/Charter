package com.charter.rewards;

import com.charter.rewards.model.Customer;
import com.charter.rewards.repository.CustomerRepository;
import com.charter.rewards.repository.TransactionRepository;
import com.charter.rewards.service.RewardsService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;

@SpringBootApplication
public class RewardsApplication {

	public static void main(String[] args) {
		SpringApplication.run(RewardsApplication.class, args);
	}
	@Bean
	CommandLineRunner initData(RewardsService rewardsService, CustomerRepository customerRepo, TransactionRepository transactionRepo) {
		return args -> {
			// Only run if we haven't generated transactions yet
			if (transactionRepo.count() == 0) {
				List<Customer> customers = customerRepo.findAll();
				Random random = new Random();

				for (Customer customer : customers) {
					// Generate 15 transactions per customer
					for (int i = 0; i < 15; i++) {
						double amount = 30 + (170 * random.nextDouble());

						// Generate dates within the last 90 days (approx 3 months)
						LocalDateTime randomDate = LocalDateTime.now()
								.minusDays(random.nextInt(90))
								.minusHours(random.nextInt(24))
								.minusMinutes(random.nextInt(60));

						// Service logic handles the Rewards summary table and Customer totals
						rewardsService.processPaymentWithDate(customer.getId(), amount, randomDate);
					}
				}
				System.out.println("--- Calculations complete: " + (customers.size() * 15) + " Transactions generated for the last 3 months ---");
			}
		};
	}
}