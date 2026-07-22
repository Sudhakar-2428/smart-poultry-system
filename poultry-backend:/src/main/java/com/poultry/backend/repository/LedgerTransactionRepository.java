package com.poultry.backend.repository;

import com.poultry.backend.entity.LedgerTransaction;
import com.poultry.backend.entity.ReferenceType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LedgerTransactionRepository extends JpaRepository<LedgerTransaction, Long>, JpaSpecificationExecutor<LedgerTransaction> {
    boolean existsByTransactionCode(String transactionCode);
    boolean existsByReferenceTypeAndReferenceId(ReferenceType referenceType, Long referenceId);
    List<LedgerTransaction> findByLedgerAccountId(Long ledgerAccountId);
}
