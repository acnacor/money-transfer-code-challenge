package com.example.account_transfer_api.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "fx_rates")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FxRate {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 3)
    private String fromCurrency;
    @Column(nullable = false, length = 3)
    private String toCurrency;
    @Column(nullable = false, precision = 19, scale = 6)
    private BigDecimal rate;
}
