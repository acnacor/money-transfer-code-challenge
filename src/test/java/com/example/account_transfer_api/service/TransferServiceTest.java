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
    }

    @Test
    void testSuccessfulUsdToUsdTransfer() {
        Account alice = new Account(aliceId, "Alice", new BigDecimal("1000.00"), "USD");
        Account bob = new Account(bobId, "Bob", new BigDecimal("500.00"), "USD");

        when(accountRepository.findById(aliceId)).thenReturn(Optional.of(alice));
        when(accountRepository.findById(bobId)).thenReturn(Optional.of(bob));
        when(feeConfigService.getGlobalFeePercentage()).thenReturn(new BigDecimal("0.01"));

        TransferRequest request = new TransferRequest(aliceId, bobId, new BigDecimal("100"));

        TransferResponse response = transferService.transferMoney(request);

        assertThat(response.getStatus()).isEqualTo(TransactionStatus.SUCCESS);
        assertThat(response.getAmountDebited()).isEqualByComparingTo(new BigDecimal("100.00"));
        assertThat(response.getFee()).isEqualByComparingTo(new BigDecimal("1.00"));

        assertThat(alice.getBalance()).isEqualByComparingTo(new BigDecimal("899.00"));
        assertThat(bob.getBalance()).isEqualByComparingTo(new BigDecimal("600.00"));

        verify(transactionRepository, times(1)).save(any(Transaction.class));
    }

    @Test
    void testUsdToUsdTransfer() {
        Account alice = new Account(aliceId, "Alice", new BigDecimal("1000.00"), "USD");
        Account bob = new Account(bobId, "Bob", new BigDecimal("500.00"), "USD");

        when(accountRepository.findById(aliceId)).thenReturn(Optional.of(alice));
        when(accountRepository.findById(bobId)).thenReturn(Optional.of(bob));
        when(feeConfigService.getGlobalFeePercentage()).thenReturn(new BigDecimal("0.01"));

        TransferRequest request = new TransferRequest(aliceId, bobId, new BigDecimal("100"));

        TransferResponse response = transferService.transferMoney(request);

        assertThat(response.getStatus()).isEqualTo(TransactionStatus.SUCCESS);

        BigDecimal fee = new BigDecimal("100.00").multiply(new BigDecimal("0.01")).setScale(2, RoundingMode.HALF_UP); // 1.00
        BigDecimal expectedAliceBalance = new BigDecimal("1000.00").subtract(new BigDecimal("100.00").add(fee)).setScale(2, RoundingMode.HALF_UP);
        assertThat(alice.getBalance()).isEqualByComparingTo(expectedAliceBalance);

        BigDecimal expectedBobBalance = new BigDecimal("500.00").add(new BigDecimal("100.00")).setScale(2, RoundingMode.HALF_UP);
        assertThat(bob.getBalance()).isEqualByComparingTo(expectedBobBalance);

        verify(transactionRepository, times(1)).save(any(Transaction.class));
    }

    @Test
    void testInsufficientFunds() {
        Account alice = new Account(aliceId, "Alice", new BigDecimal("50.00"), "USD");
        Account bob = new Account(bobId, "Bob", new BigDecimal("500.00"), "USD");

        when(accountRepository.findById(aliceId)).thenReturn(Optional.of(alice));
        when(accountRepository.findById(bobId)).thenReturn(Optional.of(bob));
        when(feeConfigService.getGlobalFeePercentage()).thenReturn(new BigDecimal("0.01"));

        TransferRequest request = new TransferRequest(aliceId, bobId, new BigDecimal("100"));

        TransferResponse response = transferService.transferMoney(request);

        assertThat(response.getStatus()).isEqualTo(TransactionStatus.FAILED);
        assertThat(response.getMessage()).isEqualTo("Insufficient funds");

        assertThat(alice.getBalance()).isEqualByComparingTo(new BigDecimal("50.00"));
        assertThat(bob.getBalance()).isEqualByComparingTo(new BigDecimal("500.00"));

        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    void testMissingFxRate() {
        Account alice = new Account(aliceId, "Alice", new BigDecimal("1000.00"), "USD");
        Account bob = new Account(bobId, "Bob", new BigDecimal("500.00"), "JPY");

        when(accountRepository.findById(aliceId)).thenReturn(Optional.of(alice));
        when(accountRepository.findById(bobId)).thenReturn(Optional.of(bob));
        when(fxRateService.getRate("USD", "JPY")).thenThrow(new IllegalArgumentException("FX rate not found"));
        when(feeConfigService.getGlobalFeePercentage()).thenReturn(new BigDecimal("0.01"));

        TransferRequest request = new TransferRequest(aliceId, bobId, new BigDecimal("100"));

        TransferResponse response = transferService.transferMoney(request);

        assertThat(response.getStatus()).isEqualTo(TransactionStatus.FAILED);
        assertThat(response.getMessage()).isEqualTo("FX rate not found for transfer");

        verify(transactionRepository, never()).save(any(Transaction.class));
    }
}
