package com.veterinary.repository;

import com.veterinary.model.Customer;
import com.veterinary.model.Customer;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerRepository extends JpaRepository<Customer, Long> {
}
