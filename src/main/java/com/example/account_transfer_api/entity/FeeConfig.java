package com.example.account_transfer_api.entity;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "fee_config")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FeeConfig {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, precision = 5, scale = 4)
    private BigDecimal globalFeePercentage;
}