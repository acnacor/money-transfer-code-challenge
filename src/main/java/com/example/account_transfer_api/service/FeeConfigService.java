package com.example.account_transfer_api.service;

import com.example.account_transfer_api.entity.FeeConfig;
import com.example.account_transfer_api.repository.FeeConfigRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class FeeConfigService {

    private final FeeConfigRepository feeConfigRepository;

    public FeeConfigService(FeeConfigRepository feeConfigRepository) {
        this.feeConfigRepository = feeConfigRepository;
    }

    public BigDecimal getGlobalFeePercentage() {
        return feeConfigRepository.findAll()
                .stream()
                .findFirst()
                .map(FeeConfig::getGlobalFeePercentage)
                .orElse(BigDecimal.valueOf(0.01)); // default 1% if none configured
    }
}
