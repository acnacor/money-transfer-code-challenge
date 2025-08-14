package com.example.account_transfer_api.service;

import com.example.account_transfer_api.dto.TransferRequest;
import com.example.account_transfer_api.dto.TransferResponse;
import com.example.account_transfer_api.entity.Account;
import com.example.account_transfer_api.entity.Transaction;
import com.example.account_transfer_api.enums.TransactionStatus;
import com.example.account_transfer_api.repository.AccountRepository;
import com.example.account_transfer_api.repository.TransactionRepository;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;

@Service
@Slf4j
public class TransferService {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final FxRateService fxRateService;
    private final FeeConfigService feeConfigService;

    public TransferService(AccountRepository accountRepository,
                           TransactionRepository transactionRepository,
                           FxRateService fxRateService,
                           FeeConfigService feeConfigService) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.fxRateService = fxRateService;
        this.feeConfigService = feeConfigService;
    }

    @Transactional
    public TransferResponse transferMoney(TransferRequest request) {

        Account fromAccount = accountRepository.findByIdWithLock(request.getFromAccountId())
                .orElseThrow(() -> new IllegalArgumentException("Source account not found"));
        Account toAccount = accountRepository.findByIdWithLock(request.getToAccountId())
                .orElseThrow(() -> new IllegalArgumentException("Destination account not found"));

        if(fromAccount.getId().equals(toAccount.getId())) {
            throw new IllegalStateException("Source account and Destination account are the same.");
        }

        BigDecimal amountToTransfer = request.getAmount().setScale(2, RoundingMode.HALF_UP);

        // Convert currency
        BigDecimal fxConvertedAmount;
        try {
            fxConvertedAmount = convertCurrency(amountToTransfer, fromAccount.getCurrency(), toAccount.getCurrency());
        } catch (IllegalArgumentException e) {
            log.error(e.getMessage());
            return TransferResponse.builder()
                    .status(TransactionStatus.FAILED)
                    .message("FX rate not found for transfer")
                    .build();
        }

        // Calculate fee
        BigDecimal fee = calculateFee(amountToTransfer);
        BigDecimal totalDebit = amountToTransfer.add(fee);

        // Check balance
        if (fromAccount.getBalance().compareTo(totalDebit) < 0) {
            return TransferResponse.builder()
                    .status(TransactionStatus.FAILED)
                    .message("Insufficient funds")
                    .build();
        }

        // Update balances
        fromAccount.setBalance(fromAccount.getBalance().subtract(totalDebit));
        toAccount.setBalance(toAccount.getBalance().add(fxConvertedAmount));

        accountRepository.save(fromAccount);
        accountRepository.save(toAccount);

        Transaction transaction =  Transaction.builder()
                .fromAccountId(fromAccount.getId())
                .toAccountId(toAccount.getId())
                .amountDebited(amountToTransfer)
                .amountCredited(fxConvertedAmount)
                .fromCurrency(fromAccount.getCurrency())
                .toCurrency(toAccount.getCurrency())
                .transactionFee(fee)
                .status(TransactionStatus.SUCCESS.name())
                .build();

        transactionRepository.save(transaction);


        return TransferResponse.builder()
                .transactionId(transaction.getId())
                .status(TransactionStatus.SUCCESS)
                .message("Successful transfer")
                .amountDebited(transaction.getAmountDebited())
                .amountCredited(transaction.getAmountCredited())
                .fee(transaction.getTransactionFee())
                .fromCurrency(transaction.getFromCurrency())
                .toCurrency(transaction.getToCurrency())
                .timestamp(Instant.now())
                .build();
    }

    private BigDecimal convertCurrency(BigDecimal amount, String fromCurrency, String toCurrency) {
        if (fromCurrency.equalsIgnoreCase(toCurrency)) {
            return amount.setScale(2, RoundingMode.HALF_UP);
        }
        BigDecimal rate = fxRateService.getRate(fromCurrency, toCurrency);
        return amount.multiply(rate).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateFee(BigDecimal amount) {
        BigDecimal feePercentage = feeConfigService.getGlobalFeePercentage();
        return amount.multiply(feePercentage).setScale(2, RoundingMode.HALF_UP);
    }
}