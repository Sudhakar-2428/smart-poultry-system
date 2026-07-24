package com.poultry.backend.repository;

import com.poultry.backend.entity.LedgerAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface LedgerAccountRepository extends JpaRepository<LedgerAccount, Long>, JpaSpecificationExecutor<LedgerAccount> {
    boolean existsByAccountCode(String accountCode);
    boolean existsByAccountCodeAndIdNot(String accountCode, Long id);
}
