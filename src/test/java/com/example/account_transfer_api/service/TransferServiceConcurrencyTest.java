package com.example.account_transfer_api.service;

import com.example.account_transfer_api.dto.TransferRequest;
import com.example.account_transfer_api.dto.TransferResponse;
import com.example.account_transfer_api.entity.Account;
import com.example.account_transfer_api.enums.TransactionStatus;
import com.example.account_transfer_api.repository.AccountRepository;
import com.example.account_transfer_api.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
public class TransferServiceConcurrencyTest {

    @Autowired
    private TransferService transferService;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private FeeConfigService feeConfigService;

    private UUID aliceId;
    private UUID bobId;

    @BeforeEach
    void setup() {
        transactionRepository.deleteAll();
        accountRepository.deleteAll();

        aliceId = UUID.randomUUID();
        bobId = UUID.randomUUID();

        Account alice = Account.builder()
                .id(aliceId)
                .name("Alice")
                .balance(new BigDecimal("1000.00"))
                .currency("USD")
                .build();

        Account bob = Account.builder()
                .id(bobId)
                .name("Bob")
                .balance(new BigDecimal("500.00"))
                .currency("USD")
                .build();

        accountRepository.save(alice);
        accountRepository.save(bob);
    }

    /**
     * Tests concurrent transfer safety to prevent race conditions and lost updates.
     *
     * Setup: Alice ($1000) → Bob ($500), 50 threads each transferring $20 simultaneously
     *
     * If locking works
     * - Conflicting transfers wait for each other, preventing race conditions
     * - Final balances: Alice = $1000 - (transfers × $20) - fees, Bob = $500 + (transfers × $20)
     *
     * This test exposes the "lost update" problem where concurrent reads/writes cause some transactions to be overwritten instead of properly accumulated.
     */
    @Test
    void testConcurrencyRaceCondition() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(50);
        CountDownLatch startLatch = new CountDownLatch(1);

        List<Future<TransferResponse>> futures = new ArrayList<>();

        // 50 threads each trying to transfer $20 from Alice to Bob
        for (int i = 0; i < 50; i++) {
            futures.add(executor.submit(() -> {
                startLatch.await();
                TransferRequest request = new TransferRequest(aliceId, bobId, new BigDecimal("20.00"));
                return transferService.transferMoney(request);
            }));
        }

        startLatch.countDown(); // Start all transfers simultaneously

        // Count successful transfers and collect responses
        long successCount = 0;
        BigDecimal totalFees = BigDecimal.ZERO;

        for (Future<TransferResponse> future : futures) {
            TransferResponse response = future.get();
            if (response.getStatus() == TransactionStatus.SUCCESS) {
                successCount++;
                // Add up the fees from successful transfers
                if (response.getFee() != null) {
                    totalFees = totalFees.add(response.getFee());
                }
            }
        }

        // Check final balances
        Account alice = accountRepository.findById(aliceId).orElseThrow();
        Account bob = accountRepository.findById(bobId).orElseThrow();

        // Calculate what balances should be with the fees
        BigDecimal totalTransferred = new BigDecimal("20.00").multiply(new BigDecimal(successCount));
        BigDecimal totalDeducted = totalTransferred.add(totalFees);

        BigDecimal expectedAliceBalance = new BigDecimal("1000.00").subtract(totalDeducted);
        BigDecimal expectedBobBalance = new BigDecimal("500.00").add(totalTransferred);

        // WITHOUT locking, these assertions will fail due to lost updates
        assertThat(alice.getBalance()).isEqualTo(expectedAliceBalance);
        assertThat(bob.getBalance()).isEqualTo(expectedBobBalance);

        executor.shutdown();
    }
}
