package com.example.account_transfer_api.repository;

import com.example.account_transfer_api.entity.FxRate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FxRateRepository extends JpaRepository<FxRate, Long> {
    Optional<FxRate> findByFromCurrencyAndToCurrency(String fromCurrency, String toCurrency);
}
