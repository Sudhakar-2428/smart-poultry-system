package com.poultry.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.poultry.backend.common.FinanceEvent;
import com.poultry.backend.dto.*;
import com.poultry.backend.entity.*;
import com.poultry.backend.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class FinanceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private LedgerAccountRepository accountRepository;

    @Autowired
    private LedgerTransactionRepository transactionRepository;

    @Autowired
    private IncomeCategoryRepository incomeCategoryRepository;

    @Autowired
    private ExpenseCategoryRepository expenseCategoryRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private FarmSettingRepository farmSettingRepository;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Autowired
    private ObjectMapper objectMapper;

    private LedgerAccount testAccount;
    private IncomeCategory testIncomeCategory;
    private ExpenseCategory testExpenseCategory;

    @BeforeEach
    void setUp() {
        notificationRepository.deleteAll();
        transactionRepository.deleteAll();
        accountRepository.deleteAll();
        incomeCategoryRepository.deleteAll();
        expenseCategoryRepository.deleteAll();
        farmSettingRepository.deleteAll();

        // 1. Create standard account
        testAccount = LedgerAccount.builder()
                .accountCode("ACT-CASH-01")
                .accountName("Main Cash Account")
                .accountType(AccountType.CASH)
                .openingBalance(5000.0)
                .currentBalance(5000.0)
                .status(Status.ACTIVE)
                .description("Primary cash account")
                .build();
        testAccount = accountRepository.save(testAccount);

        // 2. Create categories
        testIncomeCategory = IncomeCategory.builder()
                .categoryCode("INC-SALE")
                .categoryName("Egg Sales")
                .status(Status.ACTIVE)
                .build();
        testIncomeCategory = incomeCategoryRepository.save(testIncomeCategory);

        testExpenseCategory = ExpenseCategory.builder()
                .categoryCode("EXP-FEE")
                .categoryName("Feed Supply")
                .status(Status.ACTIVE)
                .build();
        testExpenseCategory = expenseCategoryRepository.save(testExpenseCategory);
    }

    // --- Ledger Account Tests ---

    @Test
    @WithMockUser(roles = "ADMIN")
    void testCreateLedgerAccount_Success() throws Exception {
        LedgerAccountRequest request = LedgerAccountRequest.builder()
                .accountCode("ACT-BANK-01")
                .accountName("Main Bank Account")
                .accountType(AccountType.BANK)
                .openingBalance(10000.0)
                .status(Status.ACTIVE)
                .description("Main bank deposit")
                .build();

        mockMvc.perform(post("/api/v1/ledger-accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.accountCode", is("ACT-BANK-01")))
                .andExpect(jsonPath("$.data.currentBalance", is(10000.0)))
                .andExpect(jsonPath("$.success", is(true)));

        assertTrue(accountRepository.existsByAccountCode("ACT-BANK-01"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testCreateLedgerAccount_DuplicateCode() throws Exception {
        LedgerAccountRequest request = LedgerAccountRequest.builder()
                .accountCode("ACT-CASH-01") // already created in setUp()
                .accountName("Alternative Cash")
                .accountType(AccountType.CASH)
                .openingBalance(1000.0)
                .status(Status.ACTIVE)
                .build();

        mockMvc.perform(post("/api/v1/ledger-accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", containsString("is already registered")));
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    void testGetLedgerAccounts_ManagerRole() throws Exception {
        mockMvc.perform(get("/api/v1/ledger-accounts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(greaterThanOrEqualTo(1))));
    }

    @Test
    @WithMockUser(roles = "WORKER")
    void testGetLedgerAccounts_ForbiddenForWorker() throws Exception {
        mockMvc.perform(get("/api/v1/ledger-accounts"))
                .andExpect(status().isForbidden());
    }

    // --- Manual Transactions Tests ---

    @Test
    @WithMockUser(roles = "ADMIN")
    void testCreateManualIncome_Success_AndBalanceUpdate() throws Exception {
        LedgerTransactionRequest request = LedgerTransactionRequest.builder()
                .transactionCode("TXN-INC-001")
                .transactionDate(LocalDate.now())
                .transactionType(TransactionType.INCOME)
                .ledgerAccountId(testAccount.getId())
                .incomeCategoryId(testIncomeCategory.getId())
                .amount(500.0)
                .paymentMethod(PaymentMethod.CASH)
                .description("Sold extra egg cartons")
                .build();

        mockMvc.perform(post("/api/v1/ledger-transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.transactionCode", is("TXN-INC-001")))
                .andExpect(jsonPath("$.data.amount", is(500.0)))
                .andExpect(jsonPath("$.data.ledgerAccountName", is("Main Cash Account")));

        // Verify balance updated
        LedgerAccount updatedAccount = accountRepository.findById(testAccount.getId()).orElseThrow();
        assertEquals(5500.0, updatedAccount.getCurrentBalance()); // 5000 + 500
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testCreateManualExpense_Success_AndBalanceUpdate() throws Exception {
        LedgerTransactionRequest request = LedgerTransactionRequest.builder()
                .transactionCode("TXN-EXP-001")
                .transactionDate(LocalDate.now())
                .transactionType(TransactionType.EXPENSE)
                .ledgerAccountId(testAccount.getId())
                .expenseCategoryId(testExpenseCategory.getId())
                .amount(100.0)
                .paymentMethod(PaymentMethod.CASH)
                .description("Lightbulb replacement")
                .build();

        mockMvc.perform(post("/api/v1/ledger-transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.transactionCode", is("TXN-EXP-001")))
                .andExpect(jsonPath("$.data.amount", is(100.0)));

        // Verify balance updated
        LedgerAccount updatedAccount = accountRepository.findById(testAccount.getId()).orElseThrow();
        assertEquals(4900.0, updatedAccount.getCurrentBalance()); // 5000 - 100
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testCreateManualExpense_InsufficientFunds_CashAccount() throws Exception {
        LedgerTransactionRequest request = LedgerTransactionRequest.builder()
                .transactionCode("TXN-EXP-OVERDRAFT")
                .transactionDate(LocalDate.now())
                .transactionType(TransactionType.EXPENSE)
                .ledgerAccountId(testAccount.getId())
                .expenseCategoryId(testExpenseCategory.getId())
                .amount(6000.0) // exceeds 5000.0
                .paymentMethod(PaymentMethod.BANK_TRANSFER)
                .build();

        mockMvc.perform(post("/api/v1/ledger-transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", containsString("balance cannot go below zero")));
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    void testCreateManualTransaction_ManagerForbidden() throws Exception {
        LedgerTransactionRequest request = LedgerTransactionRequest.builder()
                .transactionCode("TXN-MGR-FAIL")
                .transactionDate(LocalDate.now())
                .transactionType(TransactionType.INCOME)
                .ledgerAccountId(testAccount.getId())
                .incomeCategoryId(testIncomeCategory.getId())
                .amount(10.0)
                .paymentMethod(PaymentMethod.CASH)
                .build();

        mockMvc.perform(post("/api/v1/ledger-transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    // --- Automatic Finance Event Integration Tests ---

    @Test
    void testAutomaticFeedExpenseEventListener() {
        assertFalse(transactionRepository.existsByReferenceTypeAndReferenceId(ReferenceType.FEED_PURCHASE, 555L));

        FinanceEvent feedEvent = FinanceEvent.builder()
                .eventType("FEED_PURCHASE_EXPENSE")
                .referenceId(555L)
                .referenceCode("FP-TEST-01")
                .amount(1200.0)
                .description("Feed purchase mapping")
                .timestamp(LocalDateTime.now())
                .build();

        // Publish event
        eventPublisher.publishEvent(feedEvent);

        // Verify transaction is recorded automatically
        assertTrue(transactionRepository.existsByReferenceTypeAndReferenceId(ReferenceType.FEED_PURCHASE, 555L));
        List<LedgerTransaction> txs = transactionRepository.findAll().stream()
                .filter(t -> t.getReferenceType() == ReferenceType.FEED_PURCHASE && t.getReferenceId().equals(555L))
                .toList();
        assertEquals(1, txs.size());
        LedgerTransaction tx = txs.get(0);
        assertEquals(1200.0, tx.getAmount());
        assertEquals(TransactionType.EXPENSE, tx.getTransactionType());
        assertEquals("SYSTEM", tx.getCreatedBy());

        // Verify account balance updated (5000 - 1200 = 3800)
        LedgerAccount updatedAccount = accountRepository.findById(testAccount.getId()).orElseThrow();
        assertEquals(3800.0, updatedAccount.getCurrentBalance());
    }

    @Test
    void testAutomaticSalesIncomeEventListener() {
        assertFalse(transactionRepository.existsByReferenceTypeAndReferenceId(ReferenceType.SALE, 777L));

        FinanceEvent salesEvent = FinanceEvent.builder()
                .eventType("SALES_REVENUE")
                .referenceId(777L)
                .referenceCode("SO-TEST-01")
                .amount(250.0)
                .description("Sales Revenue recorded")
                .timestamp(LocalDateTime.now())
                .build();

        // Publish event
        eventPublisher.publishEvent(salesEvent);

        // Verify transaction auto recorded
        assertTrue(transactionRepository.existsByReferenceTypeAndReferenceId(ReferenceType.SALE, 777L));
        List<LedgerTransaction> txs = transactionRepository.findAll().stream()
                .filter(t -> t.getReferenceType() == ReferenceType.SALE && t.getReferenceId().equals(777L))
                .toList();
        assertEquals(1, txs.size());
        LedgerTransaction tx = txs.get(0);
        assertEquals(250.0, tx.getAmount());
        assertEquals(TransactionType.INCOME, tx.getTransactionType());

        // Verify account balance updated (5000 + 250 = 5250)
        LedgerAccount updatedAccount = accountRepository.findById(testAccount.getId()).orElseThrow();
        assertEquals(5250.0, updatedAccount.getCurrentBalance());
    }

    @Test
    void testDuplicateFinanceEventPreventionAndNotification() {
        FinanceEvent salesEvent = FinanceEvent.builder()
                .eventType("SALES_REVENUE")
                .referenceId(999L)
                .referenceCode("SO-DUPLICATE-01")
                .amount(100.0)
                .description("Double revenue alert")
                .timestamp(LocalDateTime.now())
                .build();

        // Publish once
        eventPublisher.publishEvent(salesEvent);
        assertTrue(transactionRepository.existsByReferenceTypeAndReferenceId(ReferenceType.SALE, 999L));
        long countBefore = transactionRepository.findAll().stream()
                .filter(t -> t.getReferenceType() == ReferenceType.SALE && t.getReferenceId().equals(999L))
                .count();
        assertEquals(1, countBefore);

        // Publish second time
        eventPublisher.publishEvent(salesEvent);

        // Ensure transaction count is still 1
        long countAfter = transactionRepository.findAll().stream()
                .filter(t -> t.getReferenceType() == ReferenceType.SALE && t.getReferenceId().equals(999L))
                .count();
        assertEquals(1, countAfter);

        // Verify notification hook is created
        List<Notification> alerts = notificationRepository.findAll().stream()
                .filter(n -> "DUPLICATE_FINANCE_EVENT".equals(n.getType()))
                .toList();
        assertEquals(1, alerts.size());
        assertTrue(alerts.get(0).getMessage().contains("Duplicate finance event alert"));
    }

    // --- Search, Filtering and Pagination Tests ---

    @Test
    @WithMockUser(roles = "ADMIN")
    void testSearchTransactions_FiltersAndPagination() throws Exception {
        // Create manual transactions first
        transactionRepository.save(LedgerTransaction.builder()
                .transactionCode("TX-F-1")
                .transactionDate(LocalDate.now().minusDays(5))
                .transactionType(TransactionType.INCOME)
                .ledgerAccount(testAccount)
                .incomeCategory(testIncomeCategory)
                .referenceType(ReferenceType.MANUAL)
                .amount(200.0)
                .paymentMethod(PaymentMethod.CASH)
                .build());

        transactionRepository.save(LedgerTransaction.builder()
                .transactionCode("TX-F-2")
                .transactionDate(LocalDate.now().minusDays(2))
                .transactionType(TransactionType.EXPENSE)
                .ledgerAccount(testAccount)
                .expenseCategory(testExpenseCategory)
                .referenceType(ReferenceType.MANUAL)
                .amount(600.0)
                .paymentMethod(PaymentMethod.CARD)
                .build());

        transactionRepository.save(LedgerTransaction.builder()
                .transactionCode("TX-F-3")
                .transactionDate(LocalDate.now())
                .transactionType(TransactionType.INCOME)
                .ledgerAccount(testAccount)
                .incomeCategory(testIncomeCategory)
                .referenceType(ReferenceType.MANUAL)
                .amount(800.0)
                .paymentMethod(PaymentMethod.UPI)
                .build());

        // 1. Filter by Expense
        mockMvc.perform(get("/api/v1/ledger-transactions?transactionType=EXPENSE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.content[0].transactionCode", is("TX-F-2")));

        // 2. Filter by Cash payment
        mockMvc.perform(get("/api/v1/ledger-transactions?paymentMethod=CASH"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.content[0].transactionCode", is("TX-F-1")));

        // 3. Pagination and sort
        mockMvc.perform(get("/api/v1/ledger-transactions?size=2&page=0&sort=amount,asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(2)))
                .andExpect(jsonPath("$.data.content[0].amount", is(200.0)))
                .andExpect(jsonPath("$.data.content[1].amount", is(600.0)));
    }
}
