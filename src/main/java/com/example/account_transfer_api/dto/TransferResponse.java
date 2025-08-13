package com.example.account_transfer_api.dto;

import com.example.account_transfer_api.enums.TransactionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransferResponse {
    private UUID transactionId;
    private TransactionStatus status;
    private String message;
    private BigDecimal amountDebited;
    private BigDecimal amountCredited;
    private BigDecimal fee;
    private String fromCurrency;
    private String toCurrency;
    private Instant timestamp;
}
