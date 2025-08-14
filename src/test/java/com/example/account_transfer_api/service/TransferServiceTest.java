package com.example.account_transfer_api.service;

import com.example.account_transfer_api.dto.TransferRequest;
import com.example.account_transfer_api.dto.TransferResponse;
import com.example.account_transfer_api.entity.Account;
import com.example.account_transfer_api.entity.Transaction;
import com.example.account_transfer_api.enums.TransactionStatus;
import com.example.account_transfer_api.repository.AccountRepository;
import com.example.account_transfer_api.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class TransferServiceTest {

    private AccountRepository accountRepository;
    private TransactionRepository transactionRepository;
    private FxRateService fxRateService;
    private FeeConfigService feeConfigService;
    private TransferService transferService;

    private UUID aliceId;
    private UUID bobId;

    @BeforeEach
    void setUp() {
        accountRepository = mock(AccountRepository.class);
        transactionRepository = mock(TransactionRepository.class);
        fxRateService = mock(FxRateService.class);
        feeConfigService = mock(FeeConfigService.class);

        transferService = new TransferService(accountRepository, transactionRepository, fxRateService, feeConfigService);

        aliceId = UUID.randomUUID();
        bobId = UUID.randomUUID();

        when(feeConfigService.getGlobalFeePercentage()).thenReturn(new BigDecimal("0.01"));

    }

    @Test
    void testSuccessfulUsdToUsdTransfer() {
        Account alice = createAccount(aliceId, "Alice", new BigDecimal("1000.00"), "USD");
        Account bob = createAccount(bobId, "Bob", new BigDecimal("500.00"), "USD");

        mockAccounts(alice, bob);

        TransferResponse response = transfer(100, aliceId, bobId);

        assertThat(response.getStatus()).isEqualTo(TransactionStatus.SUCCESS);
        assertThat(alice.getBalance()).isEqualByComparingTo(new BigDecimal("899.00"));
        assertThat(bob.getBalance()).isEqualByComparingTo(new BigDecimal("600.00"));

        verify(transactionRepository, times(1)).save(any(Transaction.class));
    }

    @Test
    void testUsdToAudTransfer() {
        Account alice = createAccount(aliceId, "Alice", new BigDecimal("1000.00"), "USD");
        Account bob = createAccount(bobId, "Bob", new BigDecimal("500.00"), "AUD");

        mockAccounts(alice, bob);

        when(fxRateService.getRate("USD", "AUD")).thenReturn(new BigDecimal("2.0"));

        TransferRequest request = new TransferRequest(aliceId, bobId, new BigDecimal("100"));
        TransferResponse response = transferService.transferMoney(request);

        BigDecimal fee = new BigDecimal("100.00").multiply(feeConfigService.getGlobalFeePercentage()).setScale(2, RoundingMode.HALF_UP);        BigDecimal expectedAliceBalance = new BigDecimal("1000.00").subtract(new BigDecimal("100.00").add(fee)).setScale(2, RoundingMode.HALF_UP);
        BigDecimal expectedBobBalance = new BigDecimal("500.00").add(new BigDecimal("100.00").multiply(new BigDecimal("2.0"))).setScale(2, RoundingMode.HALF_UP); // 100 * 2 = 200

        assertThat(response.getStatus()).isEqualTo(TransactionStatus.SUCCESS);
        assertThat(alice.getBalance()).isEqualByComparingTo(expectedAliceBalance);
        assertThat(bob.getBalance()).isEqualByComparingTo(expectedBobBalance);

        verify(transactionRepository, times(1)).save(any(Transaction.class));
    }

    @Test
    void testInsufficientFunds() {
        Account alice = createAccount(aliceId, "Alice", new BigDecimal("50.00"), "USD");
        Account bob = createAccount(bobId, "Bob", new BigDecimal("500.00"), "USD");

        mockAccounts(alice, bob);

        TransferResponse response = transfer(100, aliceId, bobId);

        assertThat(response.getStatus()).isEqualTo(TransactionStatus.FAILED);
        assertThat(response.getMessage()).isEqualTo("Insufficient funds");

        assertThat(alice.getBalance()).isEqualByComparingTo(new BigDecimal("50.00"));
        assertThat(bob.getBalance()).isEqualByComparingTo(new BigDecimal("500.00"));

        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    void testMissingFxRate() {
        Account alice = createAccount(aliceId, "Alice", new BigDecimal("1000.00"), "USD");
        Account bob = createAccount(bobId, "Bob", new BigDecimal("500.00"), "JPY");

        mockAccounts(alice, bob);
        when(fxRateService.getRate("USD", "JPY")).thenThrow(new IllegalArgumentException("FX rate not found"));

        TransferRequest request = new TransferRequest(aliceId, bobId, new BigDecimal("100"));
        TransferResponse response = transferService.transferMoney(request);

        assertThat(response.getStatus()).isEqualTo(TransactionStatus.FAILED);
        assertThat(response.getMessage()).isEqualTo("FX rate not found for transfer");

        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    private TransferResponse transfer(double amount, UUID from, UUID to) {
        return transferService.transferMoney(new TransferRequest(from, to, BigDecimal.valueOf(amount).setScale(2, RoundingMode.HALF_UP)));
    }

    private Account createAccount(UUID id, String name, BigDecimal balance, String currency) {
        return Account.builder().id(id).name(name).balance(balance).currency(currency).build();
    }

    private void mockAccounts(Account alice, Account bob) {
        when(accountRepository.findByIdWithLock(alice.getId())).thenReturn(Optional.of(alice));
        when(accountRepository.findByIdWithLock(bob.getId())).thenReturn(Optional.of(bob));
    }
}
