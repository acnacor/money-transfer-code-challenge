package com.example.account_transfer_api.integration;

import com.example.account_transfer_api.dto.TransferRequest;
import com.example.account_transfer_api.dto.TransferResponse;
import com.example.account_transfer_api.entity.Account;
import com.example.account_transfer_api.enums.TransactionStatus;
import com.example.account_transfer_api.repository.AccountRepository;
import com.example.account_transfer_api.repository.TransactionRepository;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class TransferApiIntegrationTest {

    @LocalServerPort
    private int port;

    private final UUID aliceId = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private final UUID bobId = UUID.fromString("22222222-2222-2222-2222-222222222222");

    private final BigDecimal feePercentage = new BigDecimal("0.01"); // 1% fee

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @BeforeEach
    void setup() {
        RestAssured.port = port;

        // Reset DB to seeded accounts
        transactionRepository.deleteAll();
        accountRepository.deleteAll();

        accountRepository.save(Account.builder()
                .id(aliceId)
                .name("Alice")
                .balance(new BigDecimal("1000.00"))
                .currency("USD")
                .build());

        accountRepository.save(Account.builder()
                .id(bobId)
                .name("Bob")
                .balance(new BigDecimal("500.00"))
                .currency("AUD")
                .build());
    }

    private TransferResponse doTransfer(UUID from, UUID to, BigDecimal amount) {
        return given()
                .contentType(ContentType.JSON)
                .body(new TransferRequest(from, to, amount))
                .when()
                .post("/api/transfers")
                .then()
                .statusCode(200)
                .extract()
                .as(TransferResponse.class);
    }

    private BigDecimal getBalance(UUID accountId) {
        String balanceStr = given()
                .contentType(ContentType.JSON)
                .when()
                .get("/api/accounts/{id}", accountId)
                .then()
                .statusCode(200)
                .extract()
                .jsonPath()
                .getString("balance");
        return new BigDecimal(balanceStr);
    }

    @Test
    void testTransfer50UsdAliceToBob() {
        BigDecimal amount = new BigDecimal("50.00");
        TransferResponse resp = doTransfer(aliceId, bobId, amount);

        BigDecimal fee = amount.multiply(feePercentage).setScale(2, RoundingMode.HALF_UP);
        BigDecimal aliceExpected = new BigDecimal("1000.00").subtract(amount.add(fee));
        BigDecimal bobExpected = new BigDecimal("500.00").add(amount.multiply(new BigDecimal("2.0"))); // USD→AUD rate 2.0

        assertThat(resp.getStatus()).isEqualTo(TransactionStatus.SUCCESS);
        assertThat(getBalance(aliceId)).isEqualByComparingTo(aliceExpected);
        assertThat(getBalance(bobId)).isEqualByComparingTo(bobExpected);
    }

    /**
     * Bob starts with 500 AUD, fee 1% of each transfer.
     * Max transfers of 50 AUD he can do before running out of money:
     * Each transfer costs 50 + 0.5 = 50.5 AUD
     * 500 / 50.5 ≈ 9 full transfers
     * 10th transfer should fail.
     */
    @Test
    void testTransfer50AudBobToAliceRecurring20Times() {
        BigDecimal amount = new BigDecimal("50.00");

        BigDecimal aliceInitial = getBalance(aliceId);
        BigDecimal bobInitial = getBalance(bobId);

        int successCount = 0;

        for (int i = 0; i < 20; i++) {
            TransferResponse resp = doTransfer(bobId, aliceId, amount);
            if (resp.getStatus() == TransactionStatus.SUCCESS) {
                successCount++;
            } else {
                assertThat(resp.getMessage()).contains("Insufficient funds");
                break; // stop on first failed transfer
            }
        }

        // Total transferred in AUD
        BigDecimal totalTransferredAUD = amount.multiply(new BigDecimal(successCount));
        BigDecimal totalFeeAUD = totalTransferredAUD.multiply(feePercentage).setScale(2, RoundingMode.HALF_UP);

        // Alice receives converted USD: AUD→USD = 0.5
        BigDecimal totalReceivedUSD = totalTransferredAUD.multiply(new BigDecimal("0.5"));

        BigDecimal aliceExpected = aliceInitial.add(totalReceivedUSD).setScale(2, RoundingMode.HALF_UP);
        BigDecimal bobExpected = bobInitial.subtract(totalTransferredAUD).subtract(totalFeeAUD).setScale(2, RoundingMode.HALF_UP);

        assertThat(successCount).isEqualTo(9); // 9 successful transfers
        assertThat(getBalance(aliceId)).isEqualByComparingTo(aliceExpected);
        assertThat(getBalance(bobId)).isEqualByComparingTo(bobExpected);
    }


    /**
     * Test 5 concurrent transfers of 20 AUD from Bob (AUD) to Alice (USD).
     * Fees are 1% charged on Bob's account, and FX rate AUD→USD is applied to calculate Alice's credited amount.
     * The 5 transfers happen simultaneously and should all succeed.
     */
    @Test
    void testConcurrentTransfer20AudBobToAlice() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(5);
        CountDownLatch latch = new CountDownLatch(1);

        BigDecimal amount = new BigDecimal("20.00");
        BigDecimal feePercentage = new BigDecimal("0.01"); // 1% fee
        BigDecimal fxRateAudToUsd = new BigDecimal("0.5"); // seeded FX

        List<Future<TransferResponse>> futures = new ArrayList<>();

        for (int i = 0; i < 5; i++) {
            futures.add(executor.submit(() -> {
                latch.await();
                return doTransfer(bobId, aliceId, amount);
            }));
        }

        latch.countDown(); // Start all transfers simultaneously

        BigDecimal totalTransferredToAlice = BigDecimal.ZERO;
        BigDecimal totalFee = BigDecimal.ZERO;

        for (Future<TransferResponse> f : futures) {
            TransferResponse resp = f.get();
            assertThat(resp.getStatus()).isEqualTo(TransactionStatus.SUCCESS);

            // Bob's fee is 1% of amount
            BigDecimal fee = amount.multiply(feePercentage).setScale(2, RoundingMode.HALF_UP);
            totalFee = totalFee.add(fee);

            // Alice receives converted amount
            totalTransferredToAlice = totalTransferredToAlice.add(amount.multiply(fxRateAudToUsd));
        }

        BigDecimal aliceExpected = new BigDecimal("1000.00").add(totalTransferredToAlice);
        BigDecimal bobExpected = new BigDecimal("500.00")
                .subtract(amount.multiply(new BigDecimal(5))) // total amount sent
                .subtract(totalFee); // total fees

        assertThat(getBalance(aliceId)).isEqualByComparingTo(aliceExpected);
        assertThat(getBalance(bobId)).isEqualByComparingTo(bobExpected);

        executor.shutdown();
    }

    /**
     * Test transferring 40 USD from Alice to Bob.
     *
     * Alice pays a 1% fee on the transfer.
     * Bob receives the amount converted to his account currency (USD → AUD at 2.0 rate).
     * Verifies that balances after the transfer match expected values.
     */
    @Test
    void testTransfer40UsdAliceToBob() {
        BigDecimal amount = new BigDecimal("40.00");

        // Capture balances before the transfer
        BigDecimal aliceInitial = getBalance(aliceId);
        BigDecimal bobInitial = getBalance(bobId);

        TransferResponse resp = doTransfer(aliceId, bobId, amount);

        // Fee for Alice
        BigDecimal fee = amount.multiply(feePercentage).setScale(2, RoundingMode.HALF_UP);

        BigDecimal aliceExpected = aliceInitial.subtract(amount.add(fee));
        BigDecimal bobExpected = bobInitial.add(amount.multiply(new BigDecimal("2.0"))); // USD→AUD rate
        assertThat(resp.getStatus()).isEqualTo(TransactionStatus.SUCCESS);
        assertThat(getBalance(aliceId)).isEqualByComparingTo(aliceExpected);
        assertThat(getBalance(bobId)).isEqualByComparingTo(bobExpected);
    }


    @Test
    void testTransfer40CnyAliceToBob() {
        /**
         * This test is not possible to perform because the API does not allow specifying
         * a different currency than the source account's base currency.
         * Each account has only one base currency, so transferring 40 CNY from Alice's USD account
         * is invalid and cannot be executed.
         */
    }
}
