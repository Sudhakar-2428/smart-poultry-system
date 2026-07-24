package com.poultry.backend.repository;

import com.poultry.backend.entity.SalesOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SalesOrderRepository extends JpaRepository<SalesOrder, Long>, JpaSpecificationExecutor<SalesOrder> {
    boolean existsByOrderNumber(String orderNumber);
    boolean existsByOrderNumberAndIdNot(String orderNumber, Long id);
    List<SalesOrder> findByCustomerId(Long customerId);

    @org.springframework.data.jpa.repository.Query("SELECT COALESCE(SUM(o.balanceAmount), 0.0) FROM SalesOrder o WHERE o.paymentStatus <> com.poultry.backend.entity.PaymentStatus.PAID AND o.status <> com.poultry.backend.entity.SalesOrderStatus.CANCELLED")
    double sumPendingPayments();

}
