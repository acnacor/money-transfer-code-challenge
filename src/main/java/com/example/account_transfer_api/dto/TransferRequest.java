package com.example.account_transfer_api.dto;

import jakarta.validation.constraints.DecimalMin;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.antlr.v4.runtime.misc.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransferRequest {
    @NotNull
    private UUID fromAccountId;
    @NotNull
    private UUID toAccountId;
    @NotNull
    @DecimalMin(value = "0.01")
    private BigDecimal amount;
}
