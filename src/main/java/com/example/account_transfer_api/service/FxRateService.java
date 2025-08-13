package com.example.account_transfer_api.service;

import com.example.account_transfer_api.entity.FxRate;
import com.example.account_transfer_api.repository.FxRateRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

@Service
public class FxRateService {

    private final FxRateRepository fxRateRepository;

    public FxRateService(FxRateRepository fxRateRepository) {
        this.fxRateRepository = fxRateRepository;
    }

    public BigDecimal getRate(String currencyFrom, String currencyTo) {

        if (currencyFrom.equalsIgnoreCase(currencyTo)) {
            return BigDecimal.ONE;
        }

        FxRate rate = fxRateRepository.findByFromCurrencyAndToCurrency(currencyFrom.toUpperCase(), currencyTo.toUpperCase()
        ).orElseThrow(() -> new IllegalArgumentException(
                "FX rate not found for " + currencyFrom + " -> " + currencyTo
        ));

        return rate.getRate();
    }
}
