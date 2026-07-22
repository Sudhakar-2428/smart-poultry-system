package com.poultry.backend.repository;

import com.poultry.backend.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long>, JpaSpecificationExecutor<Customer> {
    boolean existsByCustomerCode(String customerCode);
    boolean existsByCustomerCodeAndIdNot(String customerCode, Long id);
}
