package com.poultry.backend.service.impl;

import com.poultry.backend.common.FinanceEvent;
import com.poultry.backend.dto.*;
import com.poultry.backend.entity.*;
import com.poultry.backend.exception.DuplicateRecordException;
import com.poultry.backend.exception.NotFoundException;
import com.poultry.backend.exception.ValidationException;
import com.poultry.backend.mapper.FinanceMapper;
import com.poultry.backend.repository.*;
import com.poultry.backend.service.FinanceService;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class FinanceServiceImpl implements FinanceService {

    private final LedgerAccountRepository accountRepository;
    private final LedgerTransactionRepository transactionRepository;
    private final IncomeCategoryRepository incomeCategoryRepository;
    private final ExpenseCategoryRepository expenseCategoryRepository;
    private final FarmSettingRepository farmSettingRepository;
    private final NotificationRepository notificationRepository;
    private final FinanceMapper mapper;

    private String getCurrentUser() {
        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            return SecurityContextHolder.getContext().getAuthentication().getName();
        }
        return "SYSTEM";
    }

    // --- Ledger Accounts ---
    @Override
    @Transactional
    public LedgerAccountResponse createLedgerAccount(LedgerAccountRequest request) {
        log.info("Creating ledger account: {}", request.getAccountCode());
        if (accountRepository.existsByAccountCode(request.getAccountCode())) {
            throw new DuplicateRecordException("Account code '" + request.getAccountCode() + "' is already registered.");
        }
        LedgerAccount account = mapper.toEntity(request);
        LedgerAccount saved = accountRepository.save(account);
        log.info("AUDIT: Ledger Account Created. ID: {}, Code: {}", saved.getId(), saved.getAccountCode());
        return mapper.toResponse(saved);
    }

    @Override
    @Transactional
    public LedgerAccountResponse updateLedgerAccount(Long id, LedgerAccountRequest request) {
        log.info("Updating ledger account ID: {}", id);
        LedgerAccount account = accountRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Ledger account not found with ID: " + id));

        if (accountRepository.existsByAccountCodeAndIdNot(request.getAccountCode(), id)) {
            throw new DuplicateRecordException("Account code '" + request.getAccountCode() + "' is already registered.");
        }

        account.setAccountCode(request.getAccountCode());
        account.setAccountName(request.getAccountName());
        account.setAccountType(request.getAccountType());
        account.setDescription(request.getDescription());
        account.setStatus(request.getStatus());

        LedgerAccount saved = accountRepository.save(account);
        log.info("AUDIT: Ledger Account Updated. ID: {}, Code: {}", saved.getId(), saved.getAccountCode());
        return mapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public LedgerAccountResponse getLedgerAccountById(Long id) {
        LedgerAccount account = accountRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Ledger account not found with ID: " + id));
        return mapper.toResponse(account);
    }

    @Override
    @Transactional(readOnly = true)
    public List<LedgerAccountResponse> getAllLedgerAccounts() {
        return accountRepository.findAll().stream()
                .map(mapper::toResponse)
                .collect(Collectors.toList());
    }

    // --- Income Categories ---
    @Override
    @Transactional
    public IncomeCategoryResponse createIncomeCategory(IncomeCategoryRequest request) {
        log.info("Creating income category: {}", request.getCategoryCode());
        if (incomeCategoryRepository.existsByCategoryCode(request.getCategoryCode())) {
            throw new DuplicateRecordException("Income category code '" + request.getCategoryCode() + "' is already registered.");
        }
        IncomeCategory category = mapper.toEntity(request);
        IncomeCategory saved = incomeCategoryRepository.save(category);
        log.info("AUDIT: Income Category Created. ID: {}, Code: {}", saved.getId(), saved.getCategoryCode());
        return mapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<IncomeCategoryResponse> getAllIncomeCategories() {
        return incomeCategoryRepository.findAll().stream()
                .map(mapper::toResponse)
                .collect(Collectors.toList());
    }

    // --- Expense Categories ---
    @Override
    @Transactional
    public ExpenseCategoryResponse createExpenseCategory(ExpenseCategoryRequest request) {
        log.info("Creating expense category: {}", request.getCategoryCode());
        if (expenseCategoryRepository.existsByCategoryCode(request.getCategoryCode())) {
            throw new DuplicateRecordException("Expense category code '" + request.getCategoryCode() + "' is already registered.");
        }
        ExpenseCategory category = mapper.toEntity(request);
        ExpenseCategory saved = expenseCategoryRepository.save(category);
        log.info("AUDIT: Expense Category Created. ID: {}, Code: {}", saved.getId(), saved.getCategoryCode());
        return mapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ExpenseCategoryResponse> getAllExpenseCategories() {
        return expenseCategoryRepository.findAll().stream()
                .map(mapper::toResponse)
                .collect(Collectors.toList());
    }

    // --- Manual Transactions ---
    @Override
    @Transactional
    public LedgerTransactionResponse createManualTransaction(LedgerTransactionRequest request) {
        log.info("Creating manual transaction of type: {}", request.getTransactionType());
        
        if (request.getAmount() <= 0.0) {
            throw new ValidationException("Amount must be greater than zero.");
        }

        String txCode = request.getTransactionCode();
        if (txCode == null || txCode.trim().isEmpty()) {
            txCode = "TXN-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        }

        if (transactionRepository.existsByTransactionCode(txCode)) {
            throw new DuplicateRecordException("Transaction code '" + txCode + "' is already registered.");
        }

        LedgerAccount sourceAccount = accountRepository.findById(request.getLedgerAccountId())
                .orElseThrow(() -> new NotFoundException("Ledger account not found with ID: " + request.getLedgerAccountId()));

        if (sourceAccount.getStatus() != Status.ACTIVE) {
            throw new ValidationException("Cannot post transactions to inactive accounts.");
        }

        String username = getCurrentUser();

        if (request.getTransactionType() == TransactionType.INCOME) {
            if (request.getIncomeCategoryId() == null) {
                throw new ValidationException("Income category ID is required for INCOME transactions.");
            }
            IncomeCategory incomeCategory = incomeCategoryRepository.findById(request.getIncomeCategoryId())
                    .orElseThrow(() -> new NotFoundException("Income category not found: " + request.getIncomeCategoryId()));

            if (incomeCategory.getStatus() != Status.ACTIVE) {
                throw new ValidationException("Cannot associate inactive income categories.");
            }

            sourceAccount.setCurrentBalance(sourceAccount.getCurrentBalance() + request.getAmount());
            accountRepository.save(sourceAccount);
            log.info("AUDIT: Balance Updated. Account: {}, New Balance: {}", sourceAccount.getAccountCode(), sourceAccount.getCurrentBalance());

            LedgerTransaction tx = LedgerTransaction.builder()
                    .transactionCode(txCode)
                    .transactionDate(request.getTransactionDate())
                    .transactionType(TransactionType.INCOME)
                    .ledgerAccount(sourceAccount)
                    .incomeCategory(incomeCategory)
                    .referenceType(ReferenceType.MANUAL)
                    .description(request.getDescription())
                    .amount(request.getAmount())
                    .paymentMethod(request.getPaymentMethod())
                    .createdBy(username)
                    .remarks(request.getRemarks())
                    .build();

            LedgerTransaction saved = transactionRepository.save(tx);
            log.info("AUDIT: Income Recorded. Code: {}, Amount: {}, Account: {}", saved.getTransactionCode(), saved.getAmount(), sourceAccount.getAccountName());

            checkLargeIncomeThreshold(saved);

            return mapper.toResponse(saved);

        } else if (request.getTransactionType() == TransactionType.EXPENSE) {
            if (request.getExpenseCategoryId() == null) {
                throw new ValidationException("Expense category ID is required for EXPENSE transactions.");
            }
            ExpenseCategory expenseCategory = expenseCategoryRepository.findById(request.getExpenseCategoryId())
                    .orElseThrow(() -> new NotFoundException("Expense category not found: " + request.getExpenseCategoryId()));

            if (expenseCategory.getStatus() != Status.ACTIVE) {
                throw new ValidationException("Cannot associate inactive expense categories.");
            }

            // Implement: Ledger balance cannot become invalid (e.g. for CASH & PETTY_CASH no overdraft is allowed)
            if ((sourceAccount.getAccountType() == AccountType.CASH || sourceAccount.getAccountType() == AccountType.PETTY_CASH) 
                     && sourceAccount.getCurrentBalance() < request.getAmount()) {
                throw new ValidationException("Insufficient funds. Account balance cannot go below zero for cash accounts.");
            }

            sourceAccount.setCurrentBalance(sourceAccount.getCurrentBalance() - request.getAmount());
            accountRepository.save(sourceAccount);
            log.info("AUDIT: Balance Updated. Account: {}, New Balance: {}", sourceAccount.getAccountCode(), sourceAccount.getCurrentBalance());

            checkNegativeBalanceThreshold(sourceAccount);

            LedgerTransaction tx = LedgerTransaction.builder()
                    .transactionCode(txCode)
                    .transactionDate(request.getTransactionDate())
                    .transactionType(TransactionType.EXPENSE)
                    .ledgerAccount(sourceAccount)
                    .expenseCategory(expenseCategory)
                    .referenceType(ReferenceType.MANUAL)
                    .description(request.getDescription())
                    .amount(request.getAmount())
                    .paymentMethod(request.getPaymentMethod())
                    .createdBy(username)
                    .remarks(request.getRemarks())
                    .build();

            LedgerTransaction saved = transactionRepository.save(tx);
            log.info("AUDIT: Expense Recorded. Code: {}, Amount: {}, Account: {}", saved.getTransactionCode(), saved.getAmount(), sourceAccount.getAccountName());

            checkLargeExpenseThreshold(saved);

            return mapper.toResponse(saved);

        } else if (request.getTransactionType() == TransactionType.TRANSFER) {
            if (request.getTargetLedgerAccountId() == null) {
                throw new ValidationException("Target ledger account ID is required for TRANSFER transactions.");
            }
            if (request.getLedgerAccountId().equals(request.getTargetLedgerAccountId())) {
                throw new ValidationException("Source and target accounts must be different.");
            }

            LedgerAccount targetAccount = accountRepository.findById(request.getTargetLedgerAccountId())
                    .orElseThrow(() -> new NotFoundException("Target ledger account not found with ID: " + request.getTargetLedgerAccountId()));

            if (targetAccount.getStatus() != Status.ACTIVE) {
                throw new ValidationException("Cannot transfer to inactive accounts.");
            }

            if ((sourceAccount.getAccountType() == AccountType.CASH || sourceAccount.getAccountType() == AccountType.PETTY_CASH) 
                     && sourceAccount.getCurrentBalance() < request.getAmount()) {
                throw new ValidationException("Insufficient funds in source account.");
            }

            // Perform transfer
            sourceAccount.setCurrentBalance(sourceAccount.getCurrentBalance() - request.getAmount());
            targetAccount.setCurrentBalance(targetAccount.getCurrentBalance() + request.getAmount());

            accountRepository.save(sourceAccount);
            accountRepository.save(targetAccount);

            log.info("AUDIT: Balance Updated. Source Account: {}, New Balance: {}", sourceAccount.getAccountCode(), sourceAccount.getCurrentBalance());
            log.info("AUDIT: Balance Updated. Target Account: {}, New Balance: {}", targetAccount.getAccountCode(), targetAccount.getCurrentBalance());

            checkNegativeBalanceThreshold(sourceAccount);

            // Record Transfer Out on Source Account
            LedgerTransaction sourceTx = LedgerTransaction.builder()
                    .transactionCode(txCode + "-OUT")
                    .transactionDate(request.getTransactionDate())
                    .transactionType(TransactionType.TRANSFER)
                    .ledgerAccount(sourceAccount)
                    .referenceType(ReferenceType.MANUAL)
                    .description("Transfer to: " + targetAccount.getAccountName() + ". Description: " + request.getDescription())
                    .amount(request.getAmount())
                    .paymentMethod(request.getPaymentMethod())
                    .createdBy(username)
                    .remarks(request.getRemarks())
                    .build();

            // Record Transfer In on Target Account
            LedgerTransaction targetTx = LedgerTransaction.builder()
                    .transactionCode(txCode + "-IN")
                    .transactionDate(request.getTransactionDate())
                    .transactionType(TransactionType.TRANSFER)
                    .ledgerAccount(targetAccount)
                    .referenceType(ReferenceType.MANUAL)
                    .description("Transfer from: " + sourceAccount.getAccountName() + ". Description: " + request.getDescription())
                    .amount(request.getAmount())
                    .paymentMethod(request.getPaymentMethod())
                    .createdBy(username)
                    .remarks(request.getRemarks())
                    .build();

            transactionRepository.save(targetTx);
            LedgerTransaction savedSource = transactionRepository.save(sourceTx);

            log.info("AUDIT: Transfer Recorded. From: {}, To: {}, Amount: {}", sourceAccount.getAccountName(), targetAccount.getAccountName(), request.getAmount());

            return mapper.toResponse(savedSource);
        }

        throw new ValidationException("Unsupported transaction type: " + request.getTransactionType());
    }

    @Override
    @Transactional(readOnly = true)
    public LedgerTransactionResponse getLedgerTransactionById(Long id) {
        LedgerTransaction tx = transactionRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Ledger transaction not found with ID: " + id));
        return mapper.toResponse(tx);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<LedgerTransactionResponse> searchLedgerTransactions(
            TransactionType transactionType,
            ReferenceType referenceType,
            Long ledgerAccountId,
            Long incomeCategoryId,
            Long expenseCategoryId,
            PaymentMethod paymentMethod,
            LocalDate startDate,
            LocalDate endDate,
            Double minAmount,
            Double maxAmount,
            Pageable pageable
    ) {
        log.info("Searching ledger transactions with criteria");

        Specification<LedgerTransaction> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (transactionType != null) {
                predicates.add(cb.equal(root.get("transactionType"), transactionType));
            }
            if (referenceType != null) {
                predicates.add(cb.equal(root.get("referenceType"), referenceType));
            }
            if (ledgerAccountId != null) {
                predicates.add(cb.equal(root.get("ledgerAccount").get("id"), ledgerAccountId));
            }
            if (incomeCategoryId != null) {
                predicates.add(cb.equal(root.get("incomeCategory").get("id"), incomeCategoryId));
            }
            if (expenseCategoryId != null) {
                predicates.add(cb.equal(root.get("expenseCategory").get("id"), expenseCategoryId));
            }
            if (paymentMethod != null) {
                predicates.add(cb.equal(root.get("paymentMethod"), paymentMethod));
            }
            if (startDate != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("transactionDate"), startDate));
            }
            if (endDate != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("transactionDate"), endDate));
            }
            if (minAmount != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("amount"), minAmount));
            }
            if (maxAmount != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("amount"), maxAmount));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return transactionRepository.findAll(spec, pageable).map(mapper::toResponse);
    }

    // --- Automatic Event Listener Integration ---
    @EventListener
    @Transactional
    public void handleFinanceEvent(FinanceEvent event) {
        log.info("Received FinanceEvent hook. Reference ID: {}, Type: {}", event.getReferenceId(), event.getEventType());

        ReferenceType refType = null;
        if ("FEED_PURCHASE_EXPENSE".equals(event.getEventType())) {
            refType = ReferenceType.FEED_PURCHASE;
        } else if ("SALES_REVENUE".equals(event.getEventType())) {
            refType = ReferenceType.SALE;
        }

        if (refType == null) {
            log.warn("Ignored unsupported FinanceEvent hook details: {}", event.getEventType());
            return;
        }

        // Duplicate Event Prevention
        if (transactionRepository.existsByReferenceTypeAndReferenceId(refType, event.getReferenceId())) {
            log.error("Duplicate finance event detected for ReferenceType: {} and ReferenceId: {}", refType, event.getReferenceId());
            notificationRepository.save(Notification.builder()
                    .message("Duplicate finance event alert: reference " + event.getReferenceCode() + " has already been processed.")
                    .type("DUPLICATE_FINANCE_EVENT")
                    .targetId(event.getReferenceId())
                    .build());
            return;
        }

        // Locate or Provision Default Ledger Account
        LedgerAccount account = accountRepository.findAll().stream()
                .filter(a -> a.getStatus() == Status.ACTIVE)
                .findFirst()
                .orElseGet(() -> {
                    log.info("No active ledger account exists. Creating a system default cash account.");
                    LedgerAccount defaultAcc = LedgerAccount.builder()
                            .accountCode("DEFAULT_CASH")
                            .accountName("Default Cash Account")
                            .accountType(AccountType.CASH)
                            .openingBalance(50000.0)
                            .currentBalance(50000.0)
                            .description("Auto-provisioned default account")
                            .status(Status.ACTIVE)
                            .build();
                    return accountRepository.save(defaultAcc);
                });

        if (refType == ReferenceType.FEED_PURCHASE) {
            // Find or Provision Expense Category
            ExpenseCategory expenseCategory = expenseCategoryRepository.findByCategoryCode("FEED_EXPENSE")
                    .orElseGet(() -> {
                        log.info("Provisioning FEED_EXPENSE category");
                        ExpenseCategory cat = ExpenseCategory.builder()
                                .categoryCode("FEED_EXPENSE")
                                .categoryName("Feed Purchase Expense")
                                .description("Automatic feed purchases expense category")
                                .status(Status.ACTIVE)
                                .build();
                        return expenseCategoryRepository.save(cat);
                    });

            if (expenseCategory.getStatus() != Status.ACTIVE) {
                expenseCategory.setStatus(Status.ACTIVE);
                expenseCategoryRepository.save(expenseCategory);
            }

            // Adjust balance
            account.setCurrentBalance(account.getCurrentBalance() - event.getAmount());
            accountRepository.save(account);
            log.info("AUDIT: Balance Updated. Account: {}, New Balance: {}", account.getAccountCode(), account.getCurrentBalance());

            checkNegativeBalanceThreshold(account);

            LedgerTransaction tx = LedgerTransaction.builder()
                    .transactionCode("AUTO-FEED-" + event.getReferenceCode() + "-" + event.getReferenceId())
                    .transactionDate(LocalDate.now())
                    .transactionType(TransactionType.EXPENSE)
                    .ledgerAccount(account)
                    .expenseCategory(expenseCategory)
                    .referenceType(ReferenceType.FEED_PURCHASE)
                    .referenceId(event.getReferenceId())
                    .description(event.getDescription())
                    .amount(event.getAmount())
                    .paymentMethod(PaymentMethod.CASH)
                    .createdBy("SYSTEM")
                    .build();

            LedgerTransaction saved = transactionRepository.save(tx);
            log.info("AUDIT: Automatic Feed Expense. Saved ID: {}", saved.getId());

            checkLargeExpenseThreshold(saved);

        } else if (refType == ReferenceType.SALE) {
            // Find or Provision Income Category
            IncomeCategory incomeCategory = incomeCategoryRepository.findByCategoryCode("SALES_INCOME")
                    .orElseGet(() -> {
                        log.info("Provisioning SALES_INCOME category");
                        IncomeCategory cat = IncomeCategory.builder()
                                .categoryCode("SALES_INCOME")
                                .categoryName("Sales Revenue Income")
                                .description("Automatic sales revenue income category")
                                .status(Status.ACTIVE)
                                .build();
                        return incomeCategoryRepository.save(cat);
                    });

            if (incomeCategory.getStatus() != Status.ACTIVE) {
                incomeCategory.setStatus(Status.ACTIVE);
                incomeCategoryRepository.save(incomeCategory);
            }

            // Adjust balance
            account.setCurrentBalance(account.getCurrentBalance() + event.getAmount());
            accountRepository.save(account);
            log.info("AUDIT: Balance Updated. Account: {}, New Balance: {}", account.getAccountCode(), account.getCurrentBalance());

            LedgerTransaction tx = LedgerTransaction.builder()
                    .transactionCode("AUTO-SALE-" + event.getReferenceCode() + "-" + event.getReferenceId())
                    .transactionDate(LocalDate.now())
                    .transactionType(TransactionType.INCOME)
                    .ledgerAccount(account)
                    .incomeCategory(incomeCategory)
                    .referenceType(ReferenceType.SALE)
                    .referenceId(event.getReferenceId())
                    .description(event.getDescription())
                    .amount(event.getAmount())
                    .paymentMethod(PaymentMethod.CASH)
                    .createdBy("SYSTEM")
                    .build();

            LedgerTransaction saved = transactionRepository.save(tx);
            log.info("AUDIT: Automatic Sales Income. Saved ID: {}", saved.getId());

            checkLargeIncomeThreshold(saved);
        }
    }

    // --- Reporting Service Implementations ---
    @Override
    public Double getTotalIncome() {
        return transactionRepository.findAll().stream()
                .filter(tx -> tx.getTransactionType() == TransactionType.INCOME)
                .mapToDouble(LedgerTransaction::getAmount)
                .sum();
    }

    @Override
    public Double getTotalExpense() {
        return transactionRepository.findAll().stream()
                .filter(tx -> tx.getTransactionType() == TransactionType.EXPENSE)
                .mapToDouble(LedgerTransaction::getAmount)
                .sum();
    }

    @Override
    public Double getNetProfit() {
        return getTotalIncome() - getTotalExpense();
    }

    @Override
    public Double getMonthlyIncome(int year, int month) {
        LocalDate start = LocalDate.of(year, month, 1);
        LocalDate end = start.plusMonths(1).minusDays(1);
        return transactionRepository.findAll().stream()
                .filter(tx -> tx.getTransactionType() == TransactionType.INCOME)
                .filter(tx -> !tx.getTransactionDate().isBefore(start) && !tx.getTransactionDate().isAfter(end))
                .mapToDouble(LedgerTransaction::getAmount)
                .sum();
    }

    @Override
    public Double getMonthlyExpense(int year, int month) {
        LocalDate start = LocalDate.of(year, month, 1);
        LocalDate end = start.plusMonths(1).minusDays(1);
        return transactionRepository.findAll().stream()
                .filter(tx -> tx.getTransactionType() == TransactionType.EXPENSE)
                .filter(tx -> !tx.getTransactionDate().isBefore(start) && !tx.getTransactionDate().isAfter(end))
                .mapToDouble(LedgerTransaction::getAmount)
                .sum();
    }

    @Override
    public Double getCashFlow(LocalDate startDate, LocalDate endDate) {
        double income = transactionRepository.findAll().stream()
                .filter(tx -> tx.getTransactionType() == TransactionType.INCOME)
                .filter(tx -> (startDate == null || !tx.getTransactionDate().isBefore(startDate))
                           && (endDate == null || !tx.getTransactionDate().isAfter(endDate)))
                .mapToDouble(LedgerTransaction::getAmount)
                .sum();

        double expense = transactionRepository.findAll().stream()
                .filter(tx -> tx.getTransactionType() == TransactionType.EXPENSE)
                .filter(tx -> (startDate == null || !tx.getTransactionDate().isBefore(startDate))
                           && (endDate == null || !tx.getTransactionDate().isAfter(endDate)))
                .mapToDouble(LedgerTransaction::getAmount)
                .sum();

        return income - expense;
    }

    @Override
    public Map<String, Double> getAccountBalances() {
        return accountRepository.findAll().stream()
                .collect(Collectors.toMap(LedgerAccount::getAccountName, LedgerAccount::getCurrentBalance));
    }

    @Override
    public Map<String, Double> getExpenseByCategory() {
        return transactionRepository.findAll().stream()
                .filter(tx -> tx.getTransactionType() == TransactionType.EXPENSE && tx.getExpenseCategory() != null)
                .collect(Collectors.groupingBy(
                        tx -> tx.getExpenseCategory().getCategoryName(),
                        Collectors.summingDouble(LedgerTransaction::getAmount)
                ));
    }

    @Override
    public Map<String, Double> getIncomeByCategory() {
        return transactionRepository.findAll().stream()
                .filter(tx -> tx.getTransactionType() == TransactionType.INCOME && tx.getIncomeCategory() != null)
                .collect(Collectors.groupingBy(
                        tx -> tx.getIncomeCategory().getCategoryName(),
                        Collectors.summingDouble(LedgerTransaction::getAmount)
                ));
    }

    // --- Private Helper Alert Methods ---
    private void checkLargeExpenseThreshold(LedgerTransaction tx) {
        Double limit = 10000.0;
        try {
            Optional<FarmSetting> setting = farmSettingRepository.findById("large_expense_threshold");
            if (setting.isPresent()) {
                limit = Double.parseDouble(setting.get().getValue());
            }
        } catch (Exception ignored) {}

        if (tx.getAmount() >= limit) {
            notificationRepository.save(Notification.builder()
                    .message("Large Expense Alert: Expense code " + tx.getTransactionCode() + " of amount " + tx.getAmount() + " exceeds limit.")
                    .type("LARGE_EXPENSE_ALERT")
                    .targetId(tx.getId())
                    .build());
        }
    }

    private void checkLargeIncomeThreshold(LedgerTransaction tx) {
        Double limit = 10000.0;
        try {
            Optional<FarmSetting> setting = farmSettingRepository.findById("large_income_threshold");
            if (setting.isPresent()) {
                limit = Double.parseDouble(setting.get().getValue());
            }
        } catch (Exception ignored) {}

        if (tx.getAmount() >= limit) {
            notificationRepository.save(Notification.builder()
                    .message("Large Income Alert: Income code " + tx.getTransactionCode() + " of amount " + tx.getAmount() + " exceeds limit.")
                    .type("LARGE_INCOME_ALERT")
                    .targetId(tx.getId())
                    .build());
        }
    }

    private void checkNegativeBalanceThreshold(LedgerAccount account) {
        if (account.getCurrentBalance() < 0.0) {
            notificationRepository.save(Notification.builder()
                    .message("Negative Account Balance Warning: Account " + account.getAccountName() + " has outstanding negative balance: " + account.getCurrentBalance())
                    .type("NEGATIVE_BALANCE_ALERT")
                    .targetId(account.getId())
                    .build());
            log.warn("AUDIT: Negative Balance Updated. Account: {}, Balance: {}", account.getAccountCode(), account.getCurrentBalance());
        }
    }
}
